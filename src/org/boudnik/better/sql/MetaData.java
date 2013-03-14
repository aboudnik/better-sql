/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Alexander Boudnik (shr)
 * @since Apr 6, 2008 11:24:45 PM
 *
 * I'm going to put some life in in
 */
public class MetaData {
    private static final transient String REQUIRED = "is required";
    private static final transient String ZERO_LENGTH = "zero-length is prohibited";
    private static Table[] all;
    private int maxId;
    private static final Map<Integer, MetaData.Table> byId = new HashMap<Integer, MetaData.Table>();
    private static final Map<Class<? extends OBJ>, MetaData.Table> byClass = new HashMap<Class<? extends OBJ>, MetaData.Table>();
    private static DB db;

    public MetaData(DB db, final Class<? extends OBJ>... classList) throws InstantiationException, IllegalAccessException {
        MetaData.db = db;
        maxId = 0;
        for (Class<? extends OBJ> clazz : classList)
            createOne(clazz);
        all = new Table[maxId + 1];
        for (final Map.Entry<Integer, Table> entry : byId.entrySet())
            all[entry.getKey()] = entry.getValue();
        OBJ.done = true;
    }

    private <T extends OBJ> void createOne(final Class<T> clazz) throws InstantiationException, IllegalAccessException {
        if (!Table.visited.add(clazz))
            return;
        if (clazz.getSuperclass() == Object.class)
            return;
        //noinspection unchecked
        Class<T> superClass = (Class<T>) clazz.getSuperclass();
        createOne(superClass);
        final int id = getId(clazz);
        if (id < 0)
            throw new IllegalArgumentException(clazz + " id should be > 0");
        final OBJ obj = clazz.newInstance();
        final Table table = new Table(clazz, obj.length, byClass.get(superClass));
        maxId = Math.max(maxId, id);
        final Table prev = byId.put(id, table);
        byClass.put(clazz, table);
        if (prev != null)
            throw new IllegalArgumentException("duplicate id " + id + " in " + prev.clazz + " and " + clazz);
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (OBJ.FIELD.class.isAssignableFrom(field.getType())) {
                final OBJ.FIELD oField = (OBJ.FIELD) field.get(obj);
                final Field meta = new Field(field, oField.getTarget(), oField.index);
                if ((field.getModifiers() & Modifier.FINAL) == 0)
                    throw new Table.IllegalFieldDeclaration(oField, "should be final");
                table.byName.put(meta.getName(), meta);
                table.fields[oField.index] = meta;
                oField.check(meta);
            } else if ((field.getModifiers() & Modifier.TRANSIENT) == 0)
                throw new Table.IllegalFieldDeclaration(clazz, field.getName(), "should be transient");
        }
    }

    public static Table get(final int id) {
        return all[id];
    }

    public static Table get(OBJ obj) {
        return get(getId(obj.getClass()));
    }

    public static String getShortName(final Class clazz) {
        return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
    }

    private static Integer getId(final Class<? extends OBJ> clazz) {
        return clazz.getAnnotation(TABLE.class).value();
    }

    public void print() {
        for (Table table : all)
            if (table != null) {
                System.out.println(table.renderCreate());
                System.out.println();
            }
    }

    public void create() throws SQLException {
        for (Table table : all)
            if (table != null)
                table.create();
    }

    public static class Table {
        protected final Class<? extends OBJ> clazz;
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        private final Map<String, Field> byName = new HashMap<String, Field>();
        final Field[] fields;
        private static Set<Class<? extends OBJ>> visited = new HashSet<Class<? extends OBJ>>();

        public Table(final Class<? extends OBJ> clazz, final int length, Table zuper) {
            this.clazz = clazz;
            fields = new MetaData.Field[length];
            if (zuper != null)
                System.arraycopy(zuper.fields, 0, fields, 0, zuper.fields.length);
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s(%d)", getShortName(clazz), getId(clazz)));
            for (Field field : fields)
                sb.append(String.format("%n%s", field));
            return sb.toString();
        }

        public PreparedStatement prepare(final String sql) throws SQLException {
            return db.getTransaction().prepareStatement(sql);
        }

        public void create() throws SQLException {
            String sql = renderCreate();
            System.out.println(sql);
            PreparedStatement statement = db.getTransaction().prepareStatement(sql);
            statement.executeUpdate();
        }

        public static class IllegalFieldDeclaration extends IllegalArgumentException {
            public IllegalFieldDeclaration(final OBJ.FIELD field, final String message) {
                this(field.getOwner().getClass(), get(field.getOwner()).fields[field.index].getName(), message);
            }

            public IllegalFieldDeclaration(final Class<? extends OBJ> clazz, final String field, final String message) {
                super(getShortName(clazz) + '.' + field + ' ' + message);
            }
        }

        public static class IllegalZeroLength extends IllegalFieldDeclaration {
            public IllegalZeroLength(final Class<? extends OBJ> clazz, final String field) {
                super(clazz, field, ZERO_LENGTH);
            }
        }

        public static class IllegalNullable extends IllegalFieldDeclaration {
            public IllegalNullable(final OBJ.FIELD field) {
                super(field, REQUIRED);
            }
        }

        public String renderCreate() {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE %s (", getShortName(clazz)));
            String comma = "";
            for (Field field : fields) {
                sb.append(String.format("%s%n\t%s", comma, field.getDefinition()));
                comma = ",";
            }
            sb.append(String.format("%n)"));
            return sb.toString();
        }

        public String renderInsert() {
            final StringBuilder list = new StringBuilder();
            final StringBuilder values = new StringBuilder();
            list.append(String.format("INSERT INTO %s (", getShortName(clazz)));
            String comma = "";
            for (Field field : fields) {
                list.append(String.format("%s%s", comma, field.getName()));
                values.append(String.format("%s?", comma));
                comma = ",";
            }
            list.append(") values (").append(values).append(")");
            return list.toString();
        }
    }

    public static class Field {
        final java.lang.reflect.Field field;
        private final Class<? extends OBJ.FIELD> type;
        private final int index;
        private final Class target;
        private final String name;
        private final int length;
        private final boolean isRequired;
        private final boolean isDeferred;

        public Field(final java.lang.reflect.Field field, final Class target, final int index) {
            this.field = field;
            this.target = target;
            this.index = index;
            name = (field.getAnnotation(NAME.class) == null || "".equals(field.getAnnotation(NAME.class).value())) ? field.getName() : field.getAnnotation(NAME.class).value();
            isRequired = field.getAnnotation(MANDATORY.class) == null ? field.getType().getAnnotation(Type.class).required() : field.getAnnotation(MANDATORY.class).value();
            isDeferred = field.getAnnotation(DEFERRED.class) == null ? field.getType().getAnnotation(Type.class).deferred() : field.getAnnotation(DEFERRED.class).value();
            length = field.getAnnotation(LENGTH.class) == null ? 0 : field.getAnnotation(LENGTH.class).value();
            //noinspection unchecked
            type = (Class<? extends OBJ.FIELD>) field.getType();
        }

        public Class<? extends OBJ.FIELD> getType() {
            return type;
        }

        String getName() {
            return name;
        }

        boolean isRequired() {
            return isRequired;
        }

        boolean isDeferred() {
            return isDeferred;
        }

        int getLength() {
            return length;
        }

        String getTitle() {
            if (target != null)
                return String.format("%s:%s<%s>[%d]", getName(), MetaData.getShortName(getType()), MetaData.getShortName(target), index);
            else
                return String.format("%s:%s[%d]", getName(), MetaData.getShortName(getType()), index);
        }

        public String toString() {
            return String.format("%s:%s[%d] %s%s%d", getName(), getShortName(getType()), index, isRequired() ? "NOT NULL" : "NULL", isDeferred() ? " DEFERRED " : " ", getLength());
        }

        public String getDefinition() {
            return String.format("%s %s %s", getName(), getColumnDefinition(), isRequired() ? "NOT NULL" : "NULL");
        }

        private String getColumnDefinition() {
            return String.format(getSQLType(), getLength());
        }

        public String getSQLType() {
            return db.get(type);
        }
    }
}
