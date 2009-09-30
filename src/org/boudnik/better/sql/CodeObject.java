/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

/**
 * @author shr
 * @since Nov 21, 2005 11:41:34 PM
 */
public abstract class CodeObject implements Comparable<CodeObject> {
    private final String objectId;

    protected CodeObject(String objectId) {
        this.objectId = objectId;
        PS.getInstance().storeCodeObject(this);
    }

    public String getObjectId() {
        return objectId;
    }

    public int compareTo(CodeObject o) {
        return objectId.compareTo(o.objectId);
    }

    public String toString() {
        return objectId;
    }
}
