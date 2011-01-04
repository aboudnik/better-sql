package org.boudnik.better.sql;

import com.sun.corba.se.impl.encoding.CDRInputObject;
import com.sun.xml.internal.bind.v2.runtime.reflect.Accessor;

import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexandre Boudnik (BoudnikA)
 * @since Jul 27, 2010 4:18:52 PM
 */
public abstract class DB {
    private final String driverClass;
    private final String format;
    private final int port;

    protected String server;
    protected String database;
    private PasswordAuthentication authentication;
    //todo: pool
    private static Transaction current;
    private static final Map<Class<? extends OBJ.FIELD>, String> byType = new HashMap<Class<? extends OBJ.FIELD>, String>() {
        {
            put(OBJ.INT.class, "int");
            put(OBJ.LONG.class, "bigint");
            put(OBJ.LONGSTR.class, "clob");
            put(OBJ.IMAGE.class, "int");
            put(OBJ.UUID.class, "bigint");
            put(OBJ.REF.class, "bigint");
            put(OBJ.CODEREF.class, "char(%d)");
            put(OBJ.BOOL.class, "char(1)");
            put(OBJ.STR.class, "varchar(%d)");
            put(OBJ.DATE.class, "date");
            put(OBJ.TIMESTAMP.class, "timestamp");
        }
    };

    protected String getUrl() {
        return String.format(format, server, getPort(), database);
    }

    public PasswordAuthentication getAuthentication() {
        return authentication;
    }

    private DB(String driverClass, String format, int port) {
        this.driverClass = driverClass;
        this.format = format;
        this.port = port;
    }

    public static <T extends DB> T open(Class<T> clazz, String server, String database, PasswordAuthentication authentication) throws IllegalAccessException, InstantiationException, UnknownHostException {
        final T t = clazz.newInstance();
        final InetAddress byName = InetAddress.getByName(server);
        t.server = byName.getCanonicalHostName();
        t.database = database;
        t.authentication = authentication;
        return t;
    }

    Connection getConnection() throws SQLException {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage());
        }
        final PasswordAuthentication authentication = getAuthentication();
        synchronized (this) {
            return DriverManager.getConnection(getUrl(), authentication.getUserName(), String.valueOf(authentication.getPassword()));
        }
    }

    public int getPort() {
        return port;
    }

    public synchronized Transaction getTransaction() {
        //todo: pool
        return current == null ? current = new Transaction() : current;
    }

    public String get(Class<? extends OBJ.FIELD> type) {
        return byType.get(type);
    }

    public void commit() {
        getTransaction().commit();
    }

    class Transaction {
        private Connection connection;

        public synchronized Connection getConnection() {
            try {
                return connection == null ? connection = DB.this.getConnection() : connection;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return getConnection().prepareStatement(sql);
        }

        public void commit() {
            try {
                getConnection().commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        void rollback() {
            try {
                getConnection().rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Oracle extends DB {
        public Oracle() {
            super("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d/%s", 1521);
            byType.put(OBJ.INT.class, "number(9)");
        }
    }

    private abstract static class TDS extends DB {
        public TDS(String format, int port) {
            super("net.sourceforge.jtds.jdbc.Driver", format, port);
        }
    }

    public static class MSSQL extends TDS {
        public MSSQL() {
            super("jdbc:jtds:sqlserver://%s:%d/%s", 1433);
        }
    }

    public static class Sybase extends TDS {
        public Sybase() {
            super("jdbc:jtds:sybase://%s:%d/%s", 5000);
        }
    }

    public static class Postgres extends DB {
        protected Postgres(String driverClass, String format, final int port) {
            super(driverClass, format, port);
        }

        public Postgres() {
            this("org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s", 5432);
        }
    }

    public static class Netezza extends Postgres {
        public Netezza() {
            super("org.netezza.Driver", "jdbc:netezza://%s:%d/%s", 5480);
        }
    }

    public static class GreenPlum extends Postgres {
        private GreenPlum() {
        }
    }

    public static class DB2 extends DB {
//        446, 6789, or 50000

        public DB2() {
            super("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://%s:%d/%s", 50000);
        }
    }

    public static class H2 extends DB {
        public H2() {
//            super("org.h2.Driver", "jdbc:h2:tcp://%s:%d/%s", 9092);
            super("org.h2.Driver", "jdbc:h2:mem:%3$s", 0);
        }
    }
}
