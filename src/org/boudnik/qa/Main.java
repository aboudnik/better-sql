/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.qa;

import org.boudnik.better.sql.DB;
import org.boudnik.better.sql.OBJ;
import org.boudnik.better.sql.MetaData;
import org.boudnik.qa.core.*;

import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * @author shr
 * @since Aug 31, 2005 10:29:19 PM
 */
public class Main {
    public static void main(final String[] args) throws IllegalAccessException, InstantiationException, UnknownHostException, SQLException {
        MetaData metaData = new MetaData(OBJ.class, Foo.class, Zoo.class, Bar.class, Poo.class);
//        MetaData.process(Boolean.class, OBJ.class, Foo.class, Zoo.class, Bar.class);

        DB db = DB.open(DB.H2.class, "localhost", "test", new PasswordAuthentication("APP", "".toCharArray()));
        final Connection connection = db.getConnection();
        final DatabaseMetaData data = connection.getMetaData();
        System.out.println(data.getDriverName() + " " + data.getDriverVersion());
        System.out.println(data.getDatabaseProductVersion() + " " + data.getDatabaseProductVersion());
        metaData.print();
        final Foo foo = new Foo();
        foo.name.set("bla");
        foo.age.set(15);
        foo.income.set(1500);
        foo.sex.set(Sex.FEMALE);
        final Poo poo = new Poo();
        poo.foo.set(foo);
        System.out.println("poo.foo = " + poo.foo);
        System.out.println("poo.foo.get() = " + poo.foo.get());
        System.out.println("foo = " + foo);
        System.out.println("poo = " + poo);
    }
}
