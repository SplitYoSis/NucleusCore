package dev.splityosis.nucleuscore.module;

import dev.splityosis.nucleuscore.Nucleus;
import dev.splityosis.nucleuscore.exceptions.DuplicateModuleNameException;
import dev.splityosis.nucleuscore.exceptions.InvalidModuleDeclarationException;
import dev.splityosis.nucleuscore.exceptions.MissingModuleDependency;
import dev.splityosis.nucleuscore.exceptions.MissingPluginDependency;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleLoader {

    private final Map<Class<? extends Module>, Module> classModuleMap;
    private final Map<String, Module> modulesMap;
    private List<Module> enabledModules;
    private Nucleus nucleus;
    private File modulesFolder;

    public ModuleLoader(Nucleus nucleus, File modulesFolder) {
        this.nucleus = nucleus;
        this.modulesFolder = modulesFolder;
        if (!modulesFolder.exists())
            modulesFolder.mkdirs();
        modulesMap = new HashMap<>();
        classModuleMap = new HashMap<>();
        enabledModules = new ArrayList<>();
    }

    /**
     * Loads all modules in given package.
     * @param jarFile The jar file that hosts the package.
     * @param packagePath The package path in form of "com.package".
     */
    public void loadModules(File jarFile, String packagePath){
        Set<Class<?>> classes = null;
        classes = getClassesInPackage(jarFile, packagePath);
        nucleus.log("Found "+classes.size() + " classes in package "+packagePath);
        if (!classes.isEmpty())
            for (Class<?> aClass : classes) {
                try {
                    if (Module.class.isAssignableFrom(aClass)) {
                        validateModuleClass(aClass);
                        try {
                            loadModuleClass((Class<? extends Module>) aClass);
                        }catch (InvalidModuleDeclarationException e){
                            e.printStackTrace();
                        }
                    }
                } catch (InvalidModuleDeclarationException e) {
                    e.printStackTrace();
                }
            }
    }

    public void initializeModulesState(){
        Bukkit.getScheduler().runTaskLater(nucleus.getPlugin(), () -> {
            LinkedList<PendingModuleProfile> moduleClassesToLoad = new LinkedList<>();
            for (Class<? extends Module> aClass : classModuleMap.keySet())
                moduleClassesToLoad.add(new PendingModuleProfile(aClass));

            Map<String, PendingModuleProfile> modulesByNameMap = new HashMap<>();
            // Putting modules into the map by name
            for (PendingModuleProfile pendingModuleProfile : moduleClassesToLoad) {
                String name = pendingModuleProfile.getLowerCaseName();
                if (modulesByNameMap.containsKey(name)){
                    moduleClassesToLoad.remove(pendingModuleProfile);
                    new DuplicateModuleNameException("Module with name '"+name+"' is already loaded.").printStackTrace();
                    continue;
                }
                modulesByNameMap.put(name, pendingModuleProfile);
            }

            if (!moduleClassesToLoad.isEmpty())
                while(scan(moduleClassesToLoad, modulesByNameMap));

            if (moduleClassesToLoad.size() != 0){
                // Some classes couldn't be enabled
                getNucleus().log("&4Modules couldn't enable because of a circular dependency structure:");
                for (PendingModuleProfile pendingModuleProfile : moduleClassesToLoad) {
                    getNucleus().log(" &c- "+pendingModuleProfile.getSignature().name());
                }
            }
        }, 1L);
    }

    private boolean scan(LinkedList<PendingModuleProfile> pendingModuleProfileList, Map<String, PendingModuleProfile> modulesByNameMap){

        int pendingSize = pendingModuleProfileList.size();

        Iterator<PendingModuleProfile> iterator = pendingModuleProfileList.iterator();
        modules:
        while (iterator.hasNext()) {
            PendingModuleProfile pendingModuleProfile = iterator.next();

            // Check if meant to be enabled (meaning if the user disabled it)
            if (!isModuleSetToEnabled(getModule(pendingModuleProfile.getModuleClass()))){
                pendingModuleProfileList.remove(pendingModuleProfile);
                pendingModuleProfile.setWillNeverEnable(true);
                continue modules;
            }

            // Check if required plugins are on the server
            for (String requiredPlugin : pendingModuleProfile.getSignature().requiredPlugins()) {
                if (!Bukkit.getPluginManager().isPluginEnabled(requiredPlugin)) {
                    List<String> missingPlugins = new ArrayList<>();
                    for (String s : pendingModuleProfile.getSignature().requiredPlugins()) {
                        if (!Bukkit.getPluginManager().isPluginEnabled(s))
                            missingPlugins.add(s);
                    }
                    new MissingPluginDependency(pendingModuleProfile.getSignature().name(), missingPlugins).printStackTrace();
                    pendingModuleProfileList.remove(pendingModuleProfile);
                    pendingModuleProfile.setWillNeverEnable(true);
                    continue modules;
                }
            }

            // Check if required modules are on the server
            for (String requiredModuleName : pendingModuleProfile.getLowerCaseRequiredModules()) {
                PendingModuleProfile requiredModuleProfile = modulesByNameMap.get(requiredModuleName);
                Module requiredModule = getModule(requiredModuleProfile.getModuleClass());
                if (requiredModuleProfile.isWillNeverEnable() || !isModuleSetToEnabled(requiredModule)){
                    List<String> missingModules = new ArrayList<>();
                    for (String s : pendingModuleProfile.getSignature().requiredModules()) {
                        Module anotherRequiredModule = getModule(s);
                        PendingModuleProfile anotherPendingProfile = modulesByNameMap.get(s.toLowerCase());
                        if (anotherRequiredModule == null || anotherPendingProfile.isWillNeverEnable() || !isModuleSetToEnabled(anotherRequiredModule))
                            missingModules.add(s);
                    }
                    new MissingModuleDependency(pendingModuleProfile.getSignature().name(), missingModules).printStackTrace();

                    pendingModuleProfileList.remove(pendingModuleProfile);
                    pendingModuleProfile.setWillNeverEnable(true);
                    continue modules;
                }

                if (!requiredModule.isEnabled)
                    continue modules;
            }

            // Check if load-after modules are
            for (String lowerCaseLoadAfterModule : pendingModuleProfile.getLowerCaseLoadAfterModules()) {
                PendingModuleProfile enableAfterModule = modulesByNameMap.get(lowerCaseLoadAfterModule);
                Module requiredModule = getModule(enableAfterModule.getModuleClass());
                if (!requiredModule.isEnabled && !enableAfterModule.willNeverLoad)
                    continue modules;
            }

            Module module = getModule(pendingModuleProfile.getModuleClass());
            enableModule(module);
            iterator.remove();
            //pendingModuleProfileList.remove(pendingModuleProfile);
        }

        return pendingSize == pendingModuleProfileList.size();
    }


    public void onDisable(){
        //TODO @Sllly make it so they disable in the right order
        for (Module enabledModule : enabledModules) {
            try {
                disableModule(enabledModule, false);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public Module loadModuleClass(Class<? extends Module> moduleClass) throws InvalidModuleDeclarationException {
        Module module;
        if (isLoaded(moduleClass))
            return getModule(moduleClass);
        validateModuleClass(moduleClass);
        module = initializeModuleInstance(moduleClass);
        module.nucleus = nucleus;
        module.moduleLoader = this;

        module.folder = new File(getModulesFolder(), module.getName());
        if (!module.folder.exists())
            module.folder.mkdirs();

        module.configFile = new File(module.folder, "config.yml");
        if (!module.configFile.exists()) {
            try {
                module.configFile.createNewFile();
                module.config = YamlConfiguration.loadConfiguration(module.configFile);
                module.config.set("enabled", true);
                module.saveConfig();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            module.config = YamlConfiguration.loadConfiguration(module.configFile);


        modulesMap.put(module.getName().toLowerCase(), module);
        classModuleMap.put(moduleClass, module);
        return module;
    }

    public boolean isModuleClassLoaded(Class<? extends Module> moduleClass){
        return classModuleMap.containsKey(moduleClass);
    }

    private void validateModuleClass(Class<?> clazz) throws InvalidModuleDeclarationException {
        if (!Module.class.isAssignableFrom(clazz))
            throw new InvalidModuleDeclarationException(clazz, "Class does not extend " + Module.class);
        if (!clazz.isAnnotationPresent(Signature.class))
            throw new InvalidModuleDeclarationException(clazz, "Class isn't annotated with " + Signature.class);
    }

    public Module getModule(String name){
        return modulesMap.get(name.toLowerCase());
    }

    public Module getModule(Class<? extends Module> clazz){
        return classModuleMap.get(clazz);
    }

    public boolean isModuleSetToEnabled(Module module){
        return module.getConfig().getBoolean("enabled");
    }

    public boolean isModuleEnabled(Module module){
        return enabledModules.contains(module);
    }

    public boolean enableModule(Module module){
        try {
            nucleus.log("&7Enabling "+module.getName()+"...");

            // Check if required plugins are on the server
            for (String requiredPlugin : module.getSignature().requiredPlugins()) {
                if (!Bukkit.getPluginManager().isPluginEnabled(requiredPlugin)) {
                    List<String> missingPlugins = new ArrayList<>();
                    for (String s : module.getSignature().requiredPlugins()) {
                        if (!Bukkit.getPluginManager().isPluginEnabled(s))
                            missingPlugins.add(s);
                    }
                    throw new MissingPluginDependency(module.getSignature().name(), missingPlugins);
                }
            }

            // Check if required modules are on the server
            for (String requiredModuleName : module.getSignature().requiredModules()) {
                requiredModuleName = requiredModuleName.toLowerCase();
                Module requiredModule = getModule(requiredModuleName);
                if (!isModuleEnabled(requiredModule)){
                    List<String> missingModules = new ArrayList<>();
                    for (String s : module.getSignature().requiredModules()) {
                        Module anotherRequiredModule = getModule(s);
                        if (anotherRequiredModule == null || !isModuleEnabled(anotherRequiredModule))
                            missingModules.add(s);
                    }
                    throw new MissingModuleDependency(module.getSignature().name(), missingModules);
                }
            }

            module.isEnabled = true;
            module.config.set("enabled", true);
            enabledModules.add(module);
            module.onEnable();
            module.saveConfig();
            nucleus.log("&eSuccessfully &aenabled &emodule &a&l"+module.getName()+"&e.");
        }catch (Exception e){
            nucleus.log("&cAn error occurred while enabling module '"+module.getName()+"'");
            e.printStackTrace();
            enabledModules.remove(module);
            module.isEnabled = false;
            module.config.set("enabled", false);
            module.saveConfig();
            disableModule(module);
            return false;
        }
        return true;
    }

    public boolean disableModule(Module module){
        return disableModule(module, false);
    }

    public boolean disableModule(Module module, boolean setDisabled){
        try {
            nucleus.log("&7Disabling "+module.getName()+"...");
            module.isEnabled = false;
            enabledModules.remove(module);
            module.unregisterAllCommandBranches();
            module.unregisterAllCommands();
            module.unregisterAllEvents();
            module.onDisable();
            if (setDisabled) {
                module.config.set("enabled", false);
                module.saveConfig();
            }
            nucleus.log("&eSuccessfully &cdisabled &emodule &a&l"+module.getName()+"&e.");
        }catch (Exception e){
            nucleus.log("&cAn error occurred while disabling module '"+module.getName()+"'");
            e.printStackTrace();
            module.config.set("enabled", false);
            module.saveConfig();
            return false;
        }

        return true;
    }

    public boolean reloadModule(Module module){
        try {
            module.onReload();
        }catch (Exception e){
            nucleus.log("&cAn error occurred while reloading module '"+module.getName()+"'");
            e.printStackTrace();
            return false;
        }
        nucleus.log("&eSuccessfully reloaded module &a&l"+module.getName()+"&e.");
        return true;
    }

    public Nucleus getNucleus() {
        return nucleus;
    }

    public List<Module> getEnabledModules() {
        return Collections.unmodifiableList(enabledModules);
    }

    public List<Module> getDisabledModules(){
        List<Module> modules = new ArrayList<>(classModuleMap.values());
        for (Module enabledModule : enabledModules)
            modules.remove(enabledModule);
        return modules;
    }

    public Collection<Module> getLoadedModules(){
        return classModuleMap.values();
    }

    protected Module initializeModuleInstance(Class<? extends Module> moduleClass){
        Module module;
        try {
            Signature signature = moduleClass.getAnnotation(Signature.class);
            Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            module = constructor.newInstance();
            module.clazz = moduleClass;
            module.signature = signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        modulesMap.put(module.getName().toLowerCase(), module);
        classModuleMap.put(moduleClass, module);
        return module;
    }

    protected boolean isLoaded(Class<? extends Module> moduleClass){
        return classModuleMap.containsKey(moduleClass);
    }

    public File getModulesFolder() {
        return modulesFolder;
    }

    public static Set<Class<?>> getClassesInPackage(File jarFile, String packageName) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        try {
            JarFile file = new JarFile(jarFile);
            for (Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements();) {
                JarEntry jarEntry = entry.nextElement();
                String name = jarEntry.getName().replace("/", ".");
                if(name.startsWith(packageName) && name.endsWith(".class")) {
                    classes.add(Class.forName(name.substring(0, name.length() - 6)));
                }
            }
            file.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    private class PendingModuleProfile {
        private Class<? extends Module> moduleClass;
        private Signature signature;
        private String lowerCaseName;
        private String[] lowerCaseRequiredPlugins;
        private String[] lowerCaseRequiredModules;
        private String[] lowerCaseLoadAfterModules;
        private boolean willNeverLoad = false;

        public PendingModuleProfile(Class<? extends Module> moduleClass) {
            this.moduleClass = moduleClass;
            signature = moduleClass.getAnnotation(Signature.class);
            lowerCaseName = signature.name().toLowerCase();

            lowerCaseRequiredPlugins = new String[signature.requiredPlugins().length];
            for (int i = 0; i < signature.requiredPlugins().length; i++)
                lowerCaseRequiredPlugins[i] = signature.requiredPlugins()[i].toLowerCase();

            lowerCaseRequiredModules = new String[signature.requiredModules().length];
            for (int i = 0; i < signature.requiredModules().length; i++)
                lowerCaseRequiredModules[i] = signature.requiredModules()[i].toLowerCase();

            lowerCaseLoadAfterModules = new String[signature.enableAfterModule().length];
            for (int i = 0; i < signature.enableAfterModule().length; i++)
                lowerCaseLoadAfterModules[i] = signature.enableAfterModule()[i].toLowerCase();
        }

        public Class<? extends Module> getModuleClass() {
            return moduleClass;
        }

        public Signature getSignature() {
            return signature;
        }

        public String getLowerCaseName() {
            return lowerCaseName;
        }

        public String[] getLowerCaseRequiredModules() {
            return lowerCaseRequiredModules;
        }

        public String[] getLowerCaseRequiredPlugins() {
            return lowerCaseRequiredPlugins;
        }

        public String[] getLowerCaseLoadAfterModules() {
            return lowerCaseLoadAfterModules;
        }

        public boolean isWillNeverEnable() {
            return willNeverLoad;
        }

        public void setWillNeverEnable(boolean willNeverLoad) {
            this.willNeverLoad = willNeverLoad;
        }
    }
}
