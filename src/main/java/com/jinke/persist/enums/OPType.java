package com.jinke.persist.enums;

public enum OPType {
    INSERT("INSERT"), UPDATE("UPDATE"), CHECK("CHECK"), CREATE("CREATE");

    private final String name;

    OPType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
