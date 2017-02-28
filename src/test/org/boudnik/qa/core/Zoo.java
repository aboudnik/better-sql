/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.qa.core;

import org.boudnik.better.sql.TABLE;

/**
 * @author shr
 * @since Aug 31, 2005 6:46:49 PM
 */
@TABLE(3)
public class Zoo extends Foo {
    public final IMAGE picture = new IMAGE();
    public final transient String x = "5";
}
