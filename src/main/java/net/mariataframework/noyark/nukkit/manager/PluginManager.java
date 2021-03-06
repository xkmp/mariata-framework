package net.mariataframework.noyark.nukkit.manager;


import cn.nukkit.command.Command;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import net.mariataframework.noyark.nukkit.annotations.FallBackPrefix;
import net.mariataframework.noyark.nukkit.annotations.StartNow;
import net.mariataframework.noyark.nukkit.core.FrameworkCore;
import net.mariataframework.noyark.nukkit.utils.Message;
import net.mariataframework.noyark.nukkit.utils.ReflectSet;
import net.mariataframework.noyark.nukkit.plugin.MariataClassLoader;


import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PluginManager implements JarManager{

    private static PluginManager manager;

    public  Map<Class<?>,Object> instances = new HashMap<>();

    static {
        manager = new PluginManager();
    }


    private final List<String> plugins = new ArrayList<>();

    private final List<String> jars = new ArrayList<>();

    private final List<Class<?>> mainClass = new ArrayList<>();

    private PluginManager(){

    }

    public void start(PluginBase base,boolean loadClass) throws Exception{

        File dataFolder = new File(base.getDataFolder()+"/plugin");
        if(!dataFolder.exists()){
            dataFolder.mkdirs();
        }
        File[] jarFile = dataFolder.listFiles();
        if(jarFile!=null){
            for(File file:jarFile){
                if(file.getName().endsWith(".jar")){
                    this.getJars().add(file.toString().replace(".jar",""));
                    this.loadingPlugins(file,base,loadClass);
                }
            }
        }
    }

    public static JarManager getManager(){
        return manager;
    }

    public void loadClasses(String[] rootPackage, String dirFile, URLClassLoader loader, String name) {
        Message.loading(name, Plugin.class);
        new ReflectSet(rootPackage, dirFile).loadAnnotation(loader, name,(obj,clz)-> {
            PluginManager.getManager().loadClass(obj,clz);
        });
    }

    public void loadClass(Object obj,Class<?> clz){
        if(!(obj instanceof PluginBase)&&obj instanceof Listener&&instances.get(obj.getClass())==null){
            try{
                FrameworkCore.getInstance().getServer().getPluginManager().registerEvents((Listener) obj,FrameworkCore.getInstance());
                instances.put(obj.getClass(),obj);
            }catch (IllegalStateException e){
                Message.println(e.getMessage());
            }
        }
        if(obj instanceof Command&&instances.get(obj.getClass())==null){
            FallBackPrefix fallBackPrefix = clz.getDeclaredAnnotation(FallBackPrefix.class);
            String prefix = "";
            if(fallBackPrefix != null){
                prefix = fallBackPrefix.value();
            }
            FrameworkCore.getInstance().getServer().getCommandMap().register(prefix,(Command)obj);
            instances.put(obj.getClass(),obj);
        }
        if(obj instanceof Task&&instances.get(obj.getClass())==null){
            if(clz.getDeclaredAnnotation(StartNow.class)!=null){
                FrameworkCore.getInstance().getServer().getScheduler().scheduleTask((Task)obj);
                instances.put(obj.getClass(),obj);
            }
        }
        if(obj instanceof Runnable&&instances.get(obj.getClass())==null){
            if(clz.getDeclaredAnnotation(StartNow.class)!=null){
                FrameworkCore.getInstance().getServer().getScheduler().scheduleTask(FrameworkCore.getInstance(),(Runnable) obj);
                instances.put(obj.getClass(),obj);
            }
        }

    }


    public List<String> getJars(){
        return jars;
    }

    public List<String> getPlugins(){
        return plugins;
    }


    public List<Class<?>> getMainClass(){
        return mainClass;
    }

    public Map<Class<?>,Object> getInstances(){
        return instances;
    }

    private void loadingPlugins(File file,PluginBase base,boolean loadClass) throws Exception{
        MariataClassLoader.getClassLoader().loadPlugin(file,base,this,loadClass);
    }
}
