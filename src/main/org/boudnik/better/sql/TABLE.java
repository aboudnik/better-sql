/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.lang.annotation.*;

/**
 * @author shr
 * @since Aug 31, 2005 6:42:11 PM
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TABLE {
    int value();
}
