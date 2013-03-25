/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;


import java.io.File;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Date;

/**
 * @author shr
 * @since Aug 31, 2005 6:43:59 PM
 */
@TABLE(0)
public class OBJ {
    private final transient Object[] values;
    private final transient Metadata.Table meta;
    private transient int read;
    private transient int dirty;

    //todo make another derived class with this field
//    private final UUID uuid = new UUID();
    protected int length = 0;

    //todo: remove ASAP. for compatibility only
    protected OBJ() {
        this(null);
    }

    protected OBJ(final Metadata metadata) {
        if (metadata == null) {
            meta = null;
            values = null;
        } else {
            values = new Object[(meta = metadata.get(getClass())).fields.length];
        }
    }

    public ComparableFIELD[] getKey() {
        return new ComparableFIELD[0];
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Metadata.getShortName(getClass())).append("@").append(hashCode());
        for (int i = 0; i < meta.fields.length; i++) {
            try {
                FIELD oField = (FIELD) this.meta.fields[i].field.get(this);
                sb.append(" ").append(oField);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return sb.toString();
    }

    public void save() {
        System.out.println("OBJ.save");
    }

    public UUID getUuid() {
        return null;
//todo make another derived class with this field
//        return uuid;
    }

    abstract class FIELD<T> implements Data<T> {
        public int index = length++;

        protected FIELD() {
        }

        protected FIELD(final T value) {
            set(value);
        }

        public boolean accept() {
            return true;
        }

        abstract void check(Metadata.Field field);

        public void setValue(Object value) {
            values[index] = value;
        }

        public void set(final T value) {
            if (getMeta().isRequired() && value == null)
                throw new NullPointerException();
            values[index] = value;
        }

        public T get() {
            //noinspection unchecked
            return (T) values[index];
        }

        OBJ getOwner() {
            return OBJ.this;
        }

        Class getTarget() {
            return null;
        }

        boolean isRead() {
            return (read & (1 << index)) != 0;
        }

        void setRead() {
            read |= (1 << index);
        }

        boolean isDirty() {
            return (dirty & (1 << index)) != 0;
        }

        void setDirty() {
            dirty |= (1 << index);
        }

        public boolean isRequired() {
            return getMeta().isRequired();
        }

        boolean isDeferred() {
            return getMeta().isDeferred();
        }

        public int getMaxLength() {
            return getMeta().getLength();
        }

        public Class<? extends OBJ.FIELD> getType() {
            return getMeta().getType();
        }

        @Override
        public final int hashCode() {
            throw new InternalError("Not supported.");
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        @Override
        public final boolean equals(Object obj) {
            throw new InternalError("Not supported.");
        }

        String getFunction() {
            return null;
        }

        boolean isMassiveFunction() {
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            return sb.append(getMeta().getTitle()).append(" ").append(get()).toString();
        }

        protected Metadata.Field getMeta() {
            return meta.fields[index];
        }
    }

    public abstract class ComparableFIELD<T> extends FIELD<T> implements Comparable<FIELD<T>> {
        protected ComparableFIELD() {
        }

        protected ComparableFIELD(T value) {
            super(value);
        }

        public ComparableFIELD<Integer> count() {
            return new FUNC<Integer>(INT.class, "count", true, this);
        }

        public int compareTo(FIELD<T> o) {
            //noinspection unchecked
            Comparable<T> o1 = (Comparable<T>) get();
            T o2 = o.get();
            return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? 1 : o1.compareTo(o2);
        }
    }

    @Type(deferred = false, required = true)
    public class UUID extends ComparableFIELD<Identity<OBJ>> {
        void check(final Metadata.Field meta) {
            if (!meta.isRequired())
                throw new Metadata.IllegalNullable(this);
        }
    }

    @Type(deferred = false, required = false)
    public class DATE extends ComparableFIELD<Date> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class TIME extends ComparableFIELD<Time> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class TIMESTAMP extends ComparableFIELD<Timestamp> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class INT extends ComparableFIELD<Integer> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class LONG extends ComparableFIELD<Long> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class NUMERIC extends ComparableFIELD<Number> {
        void check(final Metadata.Field meta) {
        }
    }

    @Type(deferred = false, required = false)
    public class REF<T extends OBJ> extends ComparableFIELD<T> {
        final Class<T> clazz;

        public REF(Class<T> clazz) {
            this.clazz = clazz;
        }

        public void set(T value) {
            super.setValue(value == null ? null : new Reference<T>(value));
        }

        public T get() {
            //noinspection unchecked
            return ((Reference<T>) values[index]).get();
        }

        void check(final Metadata.Field meta) {
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            return sb.append(getMeta().getTitle()).append(" ").append(values[index]).toString();
        }

        Class getTarget() {
            return clazz;
        }
    }

    @Type(deferred = false, required = false)
    public class CODEREF<T extends CodeObject> extends ComparableFIELD<T> {
        final Class<T> clazz;

        public CODEREF(Class<T> clazz) {
            this.clazz = clazz;
        }

        public void set(T value) {
            setValue(value == null ? null : value.getObjectId());
        }

        public T get() {
            //noinspection unchecked
            return (T) PS.getInstance().getCodeObject((String) values[index]);
        }

        void check(final Metadata.Field meta) {
        }

        Class getTarget() {
            return clazz;
        }
    }

    @Type(deferred = false, required = true)
    public class BOOL extends ComparableFIELD<Boolean> {
        public BOOL() {
            super(false);
        }

        void check(final Metadata.Field meta) {
            if (!meta.isRequired())
                throw new Metadata.IllegalNullable(this);
        }
    }

    @Type(deferred = false, required = false)
    public class STR extends ComparableFIELD<String> {
        void check(final Metadata.Field meta) {
//            if (meta.getLength() == 0)
//                throw new Metadata.Table.IllegalZeroLength(getOwner().getClass(), meta.getName());
        }

        public void set(String value) {
            if (value.length() <= getMeta().getLength() && value.length() > 0)
                super.set(value);
        }

        public ComparableFIELD<Integer> length() {
            return new FUNC<Integer>(INT.class, "length", false, this);
        }
    }

    @Type(deferred = false, required = false)
    public class VARCHAR extends STR {
    }

    @Type(deferred = false, required = false)
    public class CHAR extends ComparableFIELD<String> {
        void check(final Metadata.Field meta) {
//            if (meta.getLength() == 0)
//                throw new Metadata.Table.IllegalZeroLength(getOwner().getClass(), meta.getName());
        }

        public void set(String value) {
            if (value.length() <= getMeta().getLength() && value.length() > 0)
                super.set(value);
        }

        public ComparableFIELD<Integer> length() {
            return new FUNC<Integer>(INT.class, "length", false, this);
        }
    }

    @Type(deferred = false, required = false)
    public class LONGSTR extends FIELD<String> {
        void check(final Metadata.Field meta) {
            if (meta.getLength() == 0)
                throw new Metadata.IllegalZeroLength(getOwner().getClass(), meta.getName());
        }

        public void set(String value) {
            if (value.length() <= getMeta().getLength() && value.length() > 0)
                super.set(value);
        }

    }

    @Type(deferred = true, required = false)
    public class IMAGE extends FIELD<File> {
        void check(final Metadata.Field meta) {
        }
    }

    class FUNC<T extends Comparable<T>> extends OBJ.ComparableFIELD<T> {
        final Class<? extends OBJ.FIELD> clazz;
        final String function;
        final boolean massive;
        final OBJ.FIELD argument;

        void check(Metadata.Field field) {
        }

        public FUNC(Class clazz, String function, boolean massive, final OBJ.FIELD argument) {
            //noinspection unchecked
            this.clazz = clazz;
            this.function = function;
            this.massive = massive;
            this.argument = argument;
//            argument.getName(), argument.getOwner(), argument.index;
        }

        public String getFunction() {
            return function;
        }

        public boolean isMassiveFunction() {
            return massive;
        }

        public Class<? extends OBJ.FIELD> getType() {
            return clazz;
        }
    }
}
