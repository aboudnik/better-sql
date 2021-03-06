/*
 * Copyright (c) 2009-2017 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.qa.core;

import org.boudnik.better.sql.MANDATORY;
import org.boudnik.better.sql.OBJ;
import org.boudnik.better.sql.TABLE;

/**
 * @author shr
 * @since Oct 20, 2005 6:38:41 AM
 */
@TABLE(8)
public class Poo extends OBJ {
    @MANDATORY
    public final REF<Foo> foo = new REF<Foo>(Foo.class);
}
