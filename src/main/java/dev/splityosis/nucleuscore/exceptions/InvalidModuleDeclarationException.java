package dev.splityosis.nucleuscore.exceptions;

public class InvalidModuleDeclarationException extends Exception{

    public InvalidModuleDeclarationException(Class clazz, String message) {
        super(message + " at class "+clazz);
    }
}
