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

    private final DB db;
    private final Map<Integer, Metadata.Table> byId = new HashMap<Integer, Metadata.Table>();
    private final Map<Class<? extends OBJ>, Metadata.Table> byClass = new HashMap<Class<? extends OBJ>, Metadata.Table>();

    public Metadata(Class<? extends OBJ>... classList) {
        this(null, classList);
    }

    public Metadata(DB db, Class<? extends OBJ>... classList) {
        this.db = db;
        Set<Class<? extends OBJ>> visited = new HashSet<Class<? extends OBJ>>();
        for (Class<? extends OBJ> clazz : classList)
            createOne(visited, clazz);
    }

    public static class IllegalFieldDeclaration extends IllegalArgumentException {
        public IllegalFieldDeclaration(final java.lang.reflect.Field field, final String message) {
            this(field.getDeclaringClass(), field.getName(), message);
        }

        public IllegalFieldDeclaration(final Class clazz, final String field, final String message) {
            super(clazz.getSimpleName() + '.' + field + ' ' + message);
        }
    }

    public static class IllegalZeroLength extends IllegalFieldDeclaration {
        public IllegalZeroLength(java.lang.reflect.Field field) {
            super(field, ZERO_LENGTH);
        }
    }

    public static class IllegalNullable extends IllegalFieldDeclaration {
        public IllegalNullable(final java.lang.reflect.Field field) {
            super(field, REQUIRED);
        }
    }

    private <T extends OBJ> void createOne(Set<Class<? extends OBJ>> visited, final Class<T> clazz) {
        if (!visited.add(clazz))
            return;
        final Class<? super T> superclass = clazz.getSuperclass();
        if (superclass == Object.class)
            return;
        //noinspection RedundantCast
        createOne(visited, (Class<OBJ>) superclass);
        final int id = getId(clazz);
        if (id < 0)
            throw new IllegalArgumentException(clazz + " id should be > 0");
        @SuppressWarnings("RedundantCast") final Table table = new Table(clazz, getFields(clazz.getDeclaredFields()).size(), byClass.get((Class<OBJ>) superclass));
        byClass.put(clazz, table);
        final Table prev = byId.put(id, table);
        if (prev != null)
            throw new IllegalArgumentException("duplicate id " + id + " in " + prev.clazz + " and " + clazz);
        try {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                OBJ obj = clazz.newInstance();
                for (java.lang.reflect.Field field : getFields(clazz.getFields())) {
                    final OBJ.FIELD oField = (OBJ.FIELD) field.get(obj);
                    final Field meta = new Field(field, oField.getTarget(), oField.index);
                    table.byName.put(meta.getName(), meta);
                    table.fields[oField.index] = meta;
                    oField.check(meta);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<java.lang.reflect.Field> getFields(java.lang.reflect.Field[] declaredFields) {
        List<java.lang.reflect.Field> fields = new ArrayList<java.lang.reflect.Field>();
        for (java.lang.reflect.Field field : declaredFields) {
            final int modifiers = field.getModifiers();
            if (OBJ.FIELD.class.isAssignableFrom(field.getType())) {
                if (!Modifier.isFinal(modifiers)) {
                    throw new IllegalFieldDeclaration(field, "should be final");
                } else if (Modifier.isStatic(modifiers)) { //skip it
                } else {
                    fields.add(field);
                }
            }
        }
        return fields;
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

        public OBJ create() {
            try {
                return getType().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Class<? extends OBJ> getType() {
            return clazz;
        }

        public Field[] getFields() {
            return fields;
        }

        public Table(final Class<? extends OBJ> clazz, final int length, Table zuper) {
            this.clazz = clazz;
            fields = new Metadata.Field[length + (zuper != null ? zuper.fields.length : 0)];
        }

        public boolean isAbstract() {
            return Modifier.isAbstract(clazz.getModifiers());
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s(%d)", clazz.getSimpleName(), getId(clazz)));
            for (Field field : fields)
                sb.append(String.format("%n%s", field));
            return sb.toString();
        }

        public String render() {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE %s (", clazz.getSimpleName()));
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
        private final String pattern;
        private final int length;
        private final boolean isRequired;
        private final boolean isDeferred;
        private final boolean isTransient;

        public Field(final java.lang.reflect.Field field, final Class target, final int index) {
            this.field = field;
            this.target = target;
            this.index = index;
            name = (field.getAnnotation(NAME.class) == null || "".equals(field.getAnnotation(NAME.class).value())) ? field.getName() : field.getAnnotation(NAME.class).value();
            isRequired = field.getAnnotation(MANDATORY.class) == null ? field.getType().getAnnotation(Type.class).required() : field.getAnnotation(MANDATORY.class).value();
            isDeferred = field.getAnnotation(DEFERRED.class) == null ? field.getType().getAnnotation(Type.class).deferred() : field.getAnnotation(DEFERRED.class).value();
            isTransient = Modifier.isTransient(field.getModifiers());
            length = field.getAnnotation(LENGTH.class) == null ? 0 : field.getAnnotation(LENGTH.class).value();
            type = (Class<? extends OBJ.FIELD>) field.getType();
            pattern = field.getAnnotation(PATTERN.class)== null ? "".equals(field.getType().getAnnotation(Type.class).pattern()) ? null : field.getType().getAnnotation(Type.class).pattern() : field.getAnnotation(PATTERN.class).value();
        }

        public boolean isTransient() {
            return isTransient;
        }

        public String getPattern() {
            return pattern;
        }

        public Class<? extends OBJ.FIELD> getType() {
            return type;
        }

        String getName() {
            return name;
        }

        public int getIndex() {
            return index;
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

        @SuppressWarnings("UnusedDeclaration")
        public java.lang.reflect.Field getReflection() {
            return field;
        }

        String getTitle() {
            if (target != null)
                return String.format("%s:%s<%s>[%d]", getName(), getType().getSimpleName(), target.getSimpleName(), index);
            else
                return String.format("%s:%s[%d]", getName(), getType().getSimpleName(), index);
        }

        public String toString() {
            return String.format("%s:%s[%d] %s%s%d", getName(), getType().getSimpleName(), index, isRequired() ? "NOT NULL" : "NULL", isDeferred() ? " DEFERRED " : " ", getLength());
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
