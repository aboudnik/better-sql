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
 * @since Aug 31, 2005 6:48:38 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Type {
    boolean required();

    boolean deferred() default false;

    String pattern() default "";
}
