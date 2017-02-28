/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

/**
 * @author Alexander Boudnik (shr)
 * @since Apr 6, 2008 3:47:06 PM
 */
public interface VIEW<T> {
    public void set(final T value);

    public T get();

    public Class<? extends OBJ.FIELD> getType();

    abstract static class FIELD<T> implements VIEW<T> {
        private T value;

        protected FIELD() {
        }

        protected FIELD(T value) {
            this.value = value;
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }
    }

    abstract static class CFIELD<I extends Comparable<I>> extends FIELD<I> implements Comparable<CFIELD<I>> {
        protected CFIELD() {
        }

        protected CFIELD(I value) {
            super(value);
        }

        public int compareTo(CFIELD<I> o) {
            Comparable<I> o1 = get();
            I o2 = o.get();
            return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? 1 : o1.compareTo(o2);
        }
    }

    public static class STR extends CFIELD<String> {
        public STR() {
        }

        public STR(String value) {
            super(value);
        }

        public Class<? extends OBJ.FIELD> getType() {
            return OBJ.STR.class;
        }
    }
}
