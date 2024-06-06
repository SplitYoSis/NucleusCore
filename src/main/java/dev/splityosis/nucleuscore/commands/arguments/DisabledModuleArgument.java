package dev.splityosis.nucleuscore.commands.arguments;

import com.octanepvp.splityosis.commandsystem.SYSArgument;
import com.octanepvp.splityosis.commandsystem.SYSCommand;
import dev.splityosis.nucleuscore.Nucleus;
import org.bukkit.command.CommandSender;
import dev.splityosis.nucleuscore.module.Module;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisabledModuleArgument extends SYSArgument {
    private Nucleus nucleus;

    public DisabledModuleArgument(Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public boolean isValid(String s) {
        Module module = nucleus.getModuleLoader().getModule(s);
        return module != null && !nucleus.getModuleLoader().isModuleEnabled(module);
    }

    @Override
    public List<String> getInvalidInputMessage(String s) {
        Module module = nucleus.getModuleLoader().getModule(s);
        if (module == null)
            return Arrays.asList(nucleus.getLOG_PREFIX() + " &cUnknown module '"+s+"'.");
        return Arrays.asList(nucleus.getLOG_PREFIX() + " &cModule '"+module+"' is enabled, you must provide a disabled module.");
    }

    @Override
    public @NonNull List<String> tabComplete(CommandSender sender, SYSCommand command, String input) {
        List<String> complete = new ArrayList<>();
        for (Module disabledModule : nucleus.getModuleLoader().getDisabledModules())
            complete.add(disabledModule.getName());
        return complete;
    }
}
