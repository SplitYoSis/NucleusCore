package dev.splityosis.nucleuscore.exceptions;

public class UnsupportedDatabaseType extends Exception{
    public UnsupportedDatabaseType(String type) {
        super("Unknown or unsupported database type ["+type+"]");
    }
}
