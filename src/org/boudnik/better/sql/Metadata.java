/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Alexander Boudnik (shr)
 * @since Apr 6, 2008 11:24:45 PM
 */
public class Metadata implements Iterable<Metadata.Table> {

    private static final transient String REQUIRED = "is required";
    private static final transient String ZERO_LENGTH = "zero-length is prohibited";

    private int maxId;
    private final DB db;
    private final Map<Integer, Metadata.Table> byId = new HashMap<Integer, Metadata.Table>();
    private final Map<Class<? extends OBJ>, Metadata.Table> byClass = new HashMap<Class<? extends OBJ>, Metadata.Table>();


    public Metadata(Class<? extends OBJ>... classList) {
        this(null, classList);
    }

    public Metadata(DB db, Class<? extends OBJ>... classList) {
        maxId = 0;
        this.db = db;
        for (Class<? extends OBJ> clazz : classList)
            try {
                createOne(clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public static class IllegalFieldDeclaration extends IllegalArgumentException {

        public IllegalFieldDeclaration(final OBJ.FIELD field, final String message) {
            this(field.getOwner().getClass(), field.getMeta().getName(), message);
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

    private static Set<Class<? extends OBJ>> visited = new HashSet<Class<? extends OBJ>>();

    private <T extends OBJ> void createOne(final Class<T> clazz) throws InstantiationException, IllegalAccessException {
        if (!visited.add(clazz))
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
            int modifiers = field.getModifiers();
            if (OBJ.FIELD.class.isAssignableFrom(field.getType())) {
                final OBJ.FIELD oField = (OBJ.FIELD) field.get(obj);
                final Field meta = new Field(field, oField.getTarget(), oField.index);
                if ((modifiers & Modifier.FINAL) == 0)
                    throw new IllegalFieldDeclaration(oField, "should be final");
                table.byName.put(meta.getName(), meta);
                table.fields[oField.index] = meta;
                oField.check(meta);
            } else if ((modifiers & Modifier.STATIC) == Modifier.STATIC) {
            } else if ((modifiers & Modifier.TRANSIENT) == 0) {
                throw new IllegalFieldDeclaration(clazz, field.getName(), "should be transient");
            }
        }
    }

    @Override
    public Iterator<Table> iterator() {
        return byClass.values().iterator();
    }

    public Table get(final int id) {
        return byId.get(id);
    }

    public Table get(Class<? extends OBJ> clazz) {
        return byClass.get(clazz);
    }

    public static String getShortName(final Class clazz) {
        return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
    }

    private static Integer getId(final Class<? extends OBJ> clazz) {
        return clazz.getAnnotation(TABLE.class).value();
    }

    public void print() {
        for (Table table : this)
            if (table != null) {
                System.out.println(table.render());
                System.out.println();
            }
    }

    public class Table {
        protected final Class<? extends OBJ> clazz;
        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        private final Map<String, Field> byName = new HashMap<String, Field>();
        final Field[] fields;

        public Class<? extends OBJ> getType() {
            return clazz;
        }

        public Table(final Class<? extends OBJ> clazz, final int length, Table zuper) {
            this.clazz = clazz;
            fields = new Metadata.Field[length];
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

        public String render() {
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
    }

    public class Field {
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
                return String.format("%s:%s<%s>[%d]", getName(), Metadata.getShortName(getType()), Metadata.getShortName(target), index);
            else
                return String.format("%s:%s[%d]", getName(), Metadata.getShortName(getType()), index);
        }

        public String toString() {
            return String.format("%s:%s[%d] %s%s%d", getName(), getShortName(getType()), index, isRequired() ? "NOT NULL" : "NULL", isDeferred() ? " DEFERRED " : " ", getLength());
        }

        public String getDefinition() {
            return String.format("%s %s %s", getName(), getColumnDefinition(), isRequired() ? "NOT NULL" : "NULL");
        }

        private String getColumnDefinition() {
            final Adapter adapter = db.adapters.get(getType());
            try {
                return adapter.getSchemaType(this);
            } catch (Exception e) {
                throw new RuntimeException("no adapter for " + getType());
            }
        }
    }
}
