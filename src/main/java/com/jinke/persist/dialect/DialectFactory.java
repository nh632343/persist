package com.jinke.persist.dialect;

import com.jinke.persist.enums.DialectType;

public class DialectFactory {

    public static AbstractDialect getDialect(DialectType dialectType) {
        switch (dialectType) {
            case POSTGRESQL:
                return new POSTGRESQLDialect();
        }

        throw new UnsupportedOperationException("dialect: " + dialectType.name() + " has no implementation");
    }
}
