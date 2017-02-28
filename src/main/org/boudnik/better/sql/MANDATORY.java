/*
 * Copyright (c) 2009-2017 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author shr
 * @since Sep 1, 2005 3:39:31 AM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MANDATORY {
    boolean value() default true;
}

