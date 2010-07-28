package org.boudnik.better.sql;

import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
    private Connection connection;


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

    public static class Oracle extends DB {
        public Oracle() {
            super("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@%s:%d/%s", 1521);
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
            super("org.h2.Driver", "jdbc:h2:tcp://%s:%d/%s", 9092);
        }
    }
}
