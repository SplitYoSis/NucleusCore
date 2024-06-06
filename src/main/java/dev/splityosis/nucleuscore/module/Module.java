package dev.splityosis.nucleuscore.module;

import dev.splityosis.nucleuscore.Nucleus;
import dev.splityosis.commandsystem.SYSCommand;
import dev.splityosis.commandsystem.SYSCommandBranch;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class Module {

    private final List<Listener> moduleListeners = new LinkedList<>();
    private final List<SYSCommandBranch> moduleCommandBranches = new LinkedList<>();
    private final List<SYSCommand> moduleCommands = new LinkedList<>();

    protected Class<? extends Module> clazz;
    protected Signature signature;
    protected Nucleus nucleus;
    protected ModuleLoader moduleLoader;
    protected File folder;
    protected File configFile;
    protected FileConfiguration config;
    protected boolean isEnabled = false;

    protected Module() {}

    /**
     * Called when the module is enabled.
     */
    public abstract void onEnable();

    /**
     * Called when the module is disabled.
     */
    public abstract void onDisable();

    /**
     * Called when the module is reloaded.
     */
    public abstract void onReload();

    /**
     * Registers module's events.
     * @param listeners Instances of listener classes.
     */
    public final void registerEvents(Listener... listeners){
        for (Listener listener : listeners) {
            getNucleus().getPlugin().getServer().getPluginManager().registerEvents(listener, getNucleus().getPlugin());
            this.moduleListeners.add(listener);
        }
    }

    /**
     * Unregisters module's events.
     * @param listeners Instances of registered listener classes.
     */
    public final void unregisterEvents(Listener... listeners){
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
            this.moduleListeners.remove(listener);
        }
    }

    /**
     * Unregisters all events registered under this module. This method is automatically called on disable.
     */
    public final void unregisterAllEvents(){
        unregisterEvents(moduleListeners.toArray(new Listener[0]));
    }

    /**
     * @return An unmodifiable list of all registered listeners under this module.
     */
    public final List<Listener> getModuleListeners() {
        return Collections.unmodifiableList(moduleListeners);
    }

    /**
     * Registers module's commands.
     * @param commands Instances of SYSCommand.
     */
    public final void registerCommands(SYSCommand... commands){
        for (SYSCommand command : commands) {
            command.unregisterFromCommandMap();
            command.registerCommand(getNucleus().getPlugin());
            nucleus.getCommandsCommandBranch().addCommand(command);
            moduleCommands.add(command);
        }
    }

    /**
     * Unregisters module's commands
     * @param commands Instances of registered SYSCommand.
     */
    public final void unregisterCommands(SYSCommand... commands){
        for (SYSCommand command : commands) {
            command.unregisterFromCommandMap();
            nucleus.getCommandsCommandBranch().removeCommand(command);
            moduleCommands.remove(command);
        }
    }

    /**
     * Registers module's command branches.
     * @param commandBranch Instances of SYSCommandBranch.
     */
    public final void registerCommandBranch(SYSCommandBranch... commandBranch){
        for (SYSCommandBranch command : commandBranch) {
            command.registerCommandBranch(getNucleus().getPlugin());
            nucleus.getCommandsCommandBranch().addBranch(command);
            moduleCommandBranches.remove(command);
        }
    }

    /**
     * Unregisters module's command branches.
     * @param commandBranch Instances of registered SYSCommandBranch.
     */
    public final void unregisterCommandBranches(SYSCommandBranch... commandBranch){
        for (SYSCommandBranch command : commandBranch) {
            command.unregisterFromCommandMap();
            nucleus.getCommandsCommandBranch().removeBranch(command);
            moduleCommandBranches.remove(command);
        }
    }

    /**
     * Unregisters all commands registered under this module. This method is automatically called on disable.
     */
    public void unregisterAllCommands(){
        unregisterCommands(moduleCommands.toArray(new SYSCommand[0]));
    }

    /**
     * Unregisters all command branches registered under this module. This method is automatically called on disable.
     */
    public void unregisterAllCommandBranches(){
        unregisterCommandBranches(moduleCommandBranches.toArray(new SYSCommandBranch[0]));
    }


    /**
     * @return An unmodifiable list of all registered commands under this module.
     */
    public List<SYSCommand> getModuleCommands() {
        return Collections.unmodifiableList(moduleCommands);
    }

    /**
     * @return An unmodifiable list of all registered command branches under this module.
     */
    public List<SYSCommandBranch> getModuleCommandBranches() {
        return Collections.unmodifiableList(moduleCommandBranches);
    }

    /**
     * Gets the connection to the either local or remote sql database.
     * @return A Connection.
     * @throws SQLException SQLException.
     */
    public Connection getDatabaseConnection() throws SQLException {
        return getNucleus().getDatabaseConnectionManager().getConnection();
    }

    public Signature getSignature() {
        return signature;
    }

    public String getName(){
        return getSignature().name();
    }

    public String[] getAuthors(){
        return getSignature().authors();
    }

    public String getDescription(){
        return getSignature().description();
    }

    public String[] getRequiredPlugins(){
        return signature.requiredPlugins();
    }

    public String[] getRequiredModules(){
        return signature.requiredModules();
    }

    public Class<? extends Module> getClazz() {
        return clazz;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig(){
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public File getFolder() {
        return folder;
    }

    public File getConfigFile() {
        return configFile;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public Nucleus getNucleus() {
        return nucleus;
    }
}
