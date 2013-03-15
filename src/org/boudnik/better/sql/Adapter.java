package org.boudnik.better.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Alexandre Boudnik (BoudnikA)
 * @since Aug 16, 2010 3:20:33 PM
 */
abstract class Adapter {

    protected Object getValue(OBJ.FIELD type, ResultSet rs, int index) throws SQLException {
        return rs.getObject(index);
    }

    protected Finalizer setValue(OBJ.FIELD type, PreparedStatement ps, int index, Object value) throws SQLException {
        ps.setObject(index, value/*, type*/);
        return null;
    }

    abstract protected String getSchemaType(Metadata.Field field);

    abstract protected int getDBlength(Metadata.Field field);


    static class INT extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "int";
        }

        public int getDBlength(Metadata.Field field) {
            return 4;
        }
    }

    static class LONG extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "bigint";
        }

        public int getDBlength(Metadata.Field field) {
            return 8;
        }
    }

    static class STR extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return String.format("varchar(%d)", field.getLength());
        }

        public int getDBlength(Metadata.Field field) {
            return field.getLength() * 2;
        }
    }

    static class CHAR extends STR {
    }

    static class VARCHAR extends STR {
    }

    static class LONGSTR extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "ntext";
        }

        public int getDBlength(Metadata.Field field) {
            return field.getLength() * 2;
        }
    }

    static class CODEREF extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "varchar(20)";
        }

        public int getDBlength(Metadata.Field field) {
            return 20;
        }
    }

    static class IMAGE extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "OID";
        }

        public int getDBlength(Metadata.Field field) {
            return field.getLength() * 2;
        }
    }

    static class REF extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "int8";
        }

        public int getDBlength(Metadata.Field field) {
            return field.getLength() * 2;
        }
    }

    static class DATE extends Adapter {
        public String getSchemaType(Metadata.Field field) {
            return "date";
        }

        public int getDBlength(Metadata.Field field) {
            return 8;
        }
    }
}
