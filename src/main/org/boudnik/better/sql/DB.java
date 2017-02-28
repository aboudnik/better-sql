package org.boudnik.better.sql;

import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
import java.sql.*;
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
    protected final Map<Class<? extends OBJ.FIELD>, Adapter> adapters = new HashMap<Class<? extends OBJ.FIELD>, Adapter>();

    protected String server;
    protected String database;
    protected PasswordAuthentication authentication;
    private Connection connection;

    private static final Map<String, DB> dbs = new HashMap<String, DB>();

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

    public Connection getConnection() throws SQLException {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage());
        }
        final PasswordAuthentication authentication = getAuthentication();
        synchronized (this) {
            return connection == null ? connection = DriverManager.getConnection(getUrl(), authentication.getUserName(), String.valueOf(authentication.getPassword())) : connection;
        }
    }

    public int getPort() {
        return port;
    }

    public final static class Oracle extends DB {
        Oracle() {
            super("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d/%s", 1521);
        }
    }

    private abstract static class TDS extends DB {
        protected TDS(String format, int port) {
            super("net.sourceforge.jtds.jdbc.Driver", format, port);
        }
    }

    public final static class MSSQL extends TDS {
        MSSQL() {
            super("jdbc:jtds:sqlserver://%s:%d/%s", 1433);
        }
    }

    public static class Sybase extends TDS {
        Sybase() {
            super("jdbc:jtds:sybase://%s:%d/%s", 5000);
        }
    }

    private abstract static class PostgresLike extends DB {
        protected PostgresLike(String driverClass, String format, final int port) {
            super(driverClass, format, port);
        }

        protected PostgresLike() {
            this("org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s", 5432);
        }
    }

    public static class Postgres extends PostgresLike {
        Postgres() {
        }
    }

    public final static class Netezza extends PostgresLike {
        Netezza() {
            super("org.netezza.Driver", "jdbc:netezza://%s:%d/%s", 5480);
        }
    }

    public final static class GreenPlum extends PostgresLike {
        GreenPlum() {
        }
    }

    public final static class DB2 extends DB {
//        446, 6789, or 50000

        DB2() {
            super("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://%s:%d/%s", 50000);
        }
    }

    public final static class H2 extends DB {
        H2() {
            super("org.h2.Driver", "jdbc:h2:tcp://%s:%d/%s", 9092);
            adapters.put(OBJ.INT.class, new Adapter.INT());
            adapters.put(OBJ.LONG.class, new Adapter.LONG());
            adapters.put(OBJ.STR.class, new Adapter.STR());
            adapters.put(OBJ.CHAR.class, new Adapter.CHAR());
            adapters.put(OBJ.VARCHAR.class, new Adapter.VARCHAR());
            adapters.put(OBJ.LONGSTR.class, new Adapter.LONGSTR());
            adapters.put(OBJ.CODEREF.class, new Adapter.CODEREF());
            adapters.put(OBJ.IMAGE.class, new Adapter.IMAGE());
            adapters.put(OBJ.REF.class, new Adapter.REF());
            adapters.put(OBJ.DATE.class, new Adapter.DATE());
        }
    }
}
