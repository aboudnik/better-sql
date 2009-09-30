package org.boudnik.better.sql.qa.core;

import org.boudnik.better.sql.CodeObject;

/**
 * @author shr
 * @since Nov 22, 2005 12:09:32 AM
 */
public class Sex extends CodeObject {
    public Sex(String objectId) {
        super("sex." + objectId);
    }
    public static final Sex MALE = new Sex("M");
    public static final Sex FEMALE = new Sex("F");
}
