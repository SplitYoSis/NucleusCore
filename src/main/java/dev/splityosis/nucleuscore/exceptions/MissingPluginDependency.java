package dev.splityosis.nucleuscore.exceptions;

import java.util.List;

public class MissingPluginDependency extends Exception{

    public MissingPluginDependency(String moduleName, List<String> plugins) {
        super("Missing plugin dependencies for " +moduleName + " " + msg(plugins));
    }

    private static String msg(List<String> plugins){
        StringBuilder stringBuilder = new StringBuilder("[");
        for (String plugin : plugins)
            stringBuilder.append(plugin).append(", ");
        return stringBuilder.substring(0, stringBuilder.length()-2) + "]";
    }
}
