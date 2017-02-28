/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

/**
 * @author Alexander Boudnik (shr)
 * @since Apr 6, 2008 3:47:06 PM
 */
public interface Data<T> {
    void set(final T value);

    T get();

    Class<? extends OBJ.FIELD> getType();

    int getMaxLength();

    boolean isRequired();
}
