/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.io.Serializable;

/**
 * @author shr
 * @since Oct 21, 2005 12:42:11 AM
 */
public class Identity<T extends OBJ> implements Serializable, Comparable<Identity<T>> {
    private static int seq = 0;
    private static long mills = System.currentTimeMillis();
    private long i1;

    public Identity(int classId) {
        synchronized (Identity.class) {
            int x = seq++ & 0xfff;
            if (x == 0)
                mills = System.currentTimeMillis() & 0x000000ffffffffffL;
            i1 = ((long) classId) << 52 | mills << 12 | x;
        }
    }

    public Integer getClassId() {
        return (int) (i1 >>> 52);
    }

    Identity(long value) {
        i1 = value;
    }

    public static Identity valueOf(String presentation) {
        return new Identity(Long.parseLong(presentation.substring(15, 18), 16) << 52 | // class_id
                Long.parseLong(presentation.substring(0, 10), 16) << 12 | // time
                Long.parseLong(presentation.substring(11, 14), 16));       // seq
    }

    public int hashCode() {
        return (int) (i1 ^ (i1 >>> 32));
    }

    public boolean equals(Object obj) {
        return obj instanceof Identity && i1 == ((Identity) obj).i1;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String h = Long.toHexString(i1);
        String s = "0000000000000000" + Long.toHexString(i1);
        int l = h.length();
        h = s.substring(l, l + 16);
        sb.append(h.substring(3, 13));
        sb.append('-');
        sb.append(h.substring(13, 16));
        sb.append('-');
        sb.append(h.substring(0, 3));
        return sb.toString();
    }

    public int compareTo(Identity<T> o) {
        return o == null ? 1 : new Long(i1).compareTo(o.i1);
    }
}
