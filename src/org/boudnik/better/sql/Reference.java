/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.io.Serializable;

/**
 * @author shr
 * @since Oct 21, 2005 12:47:04 AM
 */
public class Reference<T extends OBJ> implements Serializable, Cloneable {
    transient private T object;
    private Identity id;

    Reference(long value) {
        id = new Identity(value);
    }

    Reference(Identity id) {
        if (id == null)
            throw new NullPointerException();
        this.id = id;
    }

    public Reference(T object) {
        if (object == null)
            throw new NullPointerException();
        this.object = object;
//todo        this.id = object.isPersistent() ? object.uuid.get() : null;
//        this.id = object.uuid.get();
    }

    protected void sync() {
        if (id == null) {
            object.save();
            id = object.uuid.get();
        }
    }

    public Identity getIdentity() {
        sync();
        return id;
    }

    public T get() {
        return object;
    }

    public String toString() {
        return id == null ? MetaData.getShortName(object.getClass()) + "@" + System.identityHashCode(object) : id.toString();
    }

    public int hashCode() {
        if (id != null)
            return id.hashCode();
        return System.identityHashCode(object);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Reference))
            return false;
        Reference r = (Reference) obj;
        if (id != null)
            return id.equals(r.id);

        return object == r.object;
    }

}
