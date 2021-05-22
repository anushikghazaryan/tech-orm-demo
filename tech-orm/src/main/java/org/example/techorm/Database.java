package org.example.techorm;

public enum Database {

    POSTGRES("postgresql"),
    MYSQL("mysql");

    private final String dbType;

    Database(String dbType) {
        this.dbType = dbType;
    }

    public String getDbType() {
        return dbType;
    }

}
