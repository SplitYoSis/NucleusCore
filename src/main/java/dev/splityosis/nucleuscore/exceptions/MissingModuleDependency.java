package dev.splityosis.nucleuscore.exceptions;

import java.util.List;

public class MissingModuleDependency extends Exception{
    public MissingModuleDependency(String moduleName, List<String> plugins) {
        super("The following modules must be enabled for " +moduleName + " to enable " + msg(plugins));
    }

    private static String msg(List<String> plugins){
        StringBuilder stringBuilder = new StringBuilder("[");
        for (String plugin : plugins)
            stringBuilder.append(plugin).append(", ");
        return stringBuilder.substring(0, stringBuilder.length()-2) + "]";
    }
}
