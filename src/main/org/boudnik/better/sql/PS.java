/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql;

import java.util.Map;
import java.util.HashMap;

/**
 * @author shr
 * @since Nov 22, 2005 1:58:23 AM
 */
public class PS {
    private static PS ourInstance = new PS();
    private static final Map<String, CodeObject> map = new HashMap<String, CodeObject>();

    public static PS getInstance() {
        return ourInstance;
    }

    private PS() {

    }

    CodeObject getCodeObject(String id) {
        return map.get(id);
    }

    void storeCodeObject(CodeObject obj) {
        map.put(obj.getObjectId(), obj);
    }
}
