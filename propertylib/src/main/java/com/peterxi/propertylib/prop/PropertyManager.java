package com.peterxi.propertylib.prop;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.FileObserver;
import android.util.Log;

import com.peterxi.propertylib.prop.base.PropertySet;
import com.peterxi.propertylib.prop.base.PropertySetClass;

public class PropertyManager {
    
    private static final String TAG = "PropertyManager";
    
    private static PropertyManager sInstance;
    
    public static PropertyManager getInstance(){
        return sInstance;
    }
    
    public static synchronized PropertyManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PropertyManager(context);
        }
        return sInstance;
    }
    
    private Context mContext;
    
    private final Map<String, Map<String, String>> mArchiveProps =
            new TreeMap<String, Map<String, String>>();
    
    private final Map<String, PropContext> mNamedContexts = 
            new HashMap<String, PropContext>();
    
    private final Map<Object, PropContext> mContextMap = 
            new HashMap<Object, PropContext>();
    
    private PropertyOnline mOnlineConfig;
    private FileUpdateListener mFileUpdateListener;

    public static class PropContext{
        private Object mContext;
        private Map<String, PropertySet> mProps;
        private List<String> mLinks = new ArrayList<String>();


        @Override
        public String toString() {
            return "PropContext{" +
                    "mContext=" + mContext +
                    ", mProps=" + mProps +
                    ", mLinks=" + mLinks +
                    '}';
        }

        public Object getContext() {
            return mContext;
        }
        public String getName() {
            return mLinks.isEmpty() ? String.valueOf(mContext) : mLinks.get(0);
        }
        public PropertySet getPropertySet(String name) {
            return mProps.get(name);
        }
        public Map<String, PropertySet> getProps() {
            return mProps == null ? null : Collections.unmodifiableMap(mProps);
        }
    }
    
    private PropertyManager(Context context) {
        mContext = context;
        registerContext(null, null);

    }
    
    public synchronized void register(Object context, String name, PropertySet set) {
        PropContext c = registerContext(context, true);
        c.mProps.put(name, set);
        for (String l : c.mLinks) {
            Map<String, String> props = mArchiveProps.get(
                    l == null ? name : l + "/" + name);
            if (props != null)
                set.applyProperties(props);
        }
    }
    
    /**
     * register global propertyset
     * @param name
     * @param set
     */
    public void register(String name, PropertySet set){
        register(null, name, set);
    }
    
    public synchronized void unregister(Object context, String name){
        PropContext c = mContextMap.get(context);
        if (c != null && c.mProps != null) {
            c.mProps.remove(name);
        }
    }
    
    public synchronized void registerContext(String name, Object context) {
        PropContext c = registerContext(context, false);
        PropContext o = mNamedContexts.put(name, c);
        c.mLinks.add(name);
        if (c.mProps != null) {
            for (Entry<String, PropertySet> e : c.mProps.entrySet()) {
                Map<String, String> props = mArchiveProps.get(name + "/" + e.getKey());
                if (props != null)
                    e.getValue().applyProperties(props);
            }
        }
        if (o != null) {
            o.mLinks.remove(name);
            if (o.mLinks.isEmpty() && o.mProps == null)
                mContextMap.remove(context);
        }
    }
    
    public synchronized void unregisterContext(String name) {
        PropContext c = mNamedContexts.remove(name);
        if (c != null) {
            c.mLinks.remove(name);
            if (c.mLinks.isEmpty() && c.mProps == null)
                mContextMap.remove(c.mContext);
        }
    }

    public synchronized void unregisterContext(Object context) {
        PropContext c = mContextMap.get(context);
        if (c != null) {
            c.mProps = null;
            if (c.mLinks.isEmpty())
                mContextMap.remove(context);
        }
    }
    
    public PropContext getContext(Object context) {
        return mContextMap.get(context);
    }
    
    public PropContext getContext(String name) {
        return mNamedContexts.get(name);
    }
    
    public Map<String, PropContext> getNamedContexts() {
        return Collections.unmodifiableMap(mNamedContexts);
    }
    
    public synchronized void extractRespective(Object dest, Object[] contexts) {
        PropContext d = registerContext(dest, true);
        for (Object ctx : contexts) {
            PropContext c = mContextMap.get(ctx);
            for (Entry<String, PropertySet> e : c.mProps.entrySet()) {
                if (e.getValue() instanceof RespectivePropertySet) {
                    if (!d.mProps.containsKey(e.getKey())) {
                        d.mProps.put(e.getKey(), 
                                ((RespectivePropertySet) e.getValue()).getRespectiveSet());
                    }
                }
            }
        }
        if (d.mProps.isEmpty()) {
            d.mProps = null;
            if (d.mLinks.isEmpty())
                mContextMap.remove(dest);
        }
    }
    
    private PropContext registerContext(Object context, boolean needProps) {
        PropContext c = mContextMap.get(context);
        if (c == null) {
            c = new PropContext();
            c.mContext = context;
            mContextMap.put(context, c);
        }
        if (c.mProps == null && needProps) {
            c.mProps = new ConcurrentHashMap<String, PropertySet>();
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    public synchronized <E extends PropertySet> E getPropertySet(String name, 
            Object... contexts){
        PropertySet result = null;
        if (contexts == null) {
            return getPropertySet(name);
        }
        for (int i = 0; i < contexts.length; i++) {
            PropContext c = mContextMap.get(contexts[i]);
            if (c != null && c.mProps != null) {
                result = c.mProps.get(name);
                if (result != null)
                    break;
            }
        }
        if (result == null) {
            result = getPropertySet(name);
        }
        return (E) result;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized <E extends PropertySet> E getPropertySet(String name){
        PropContext c = mContextMap.get(null);
        if (c == null || c.mProps == null)
            return null;
        return (E) c.mProps.get(name);
    }
    
    /**
     * propset:propkey
     * net.eth:mac:/://g   mac中将所有的 ： 替换为 ""
     * net.eth:mac:toLower 转为小写
     * 属性集：属性：转换规则
     */
    public String getPropertyValue(String prop, Object... context) {
        return getPropertyValue(context, prop);
    }
    
    public String getPropertyValue(Object[] context, String prop) {
        String[] strings = prop.split(":", 3);
        if (strings.length <= 1)
            return prop;
        String name = strings[0];
        String key = strings[1];
        PropertySet propertySet = getPropertySet(name, context);
        if (propertySet == null) {
            Log.v(TAG, "propset " + name + " not found!");
            return null;
        }
        String propValue = propertySet.getProp(key);
        if (propValue == null) {
            Log.v(TAG, "prop " + key + " not found!");
            return null;
        }
        /*  包含转换规则  toLower、toUpper、 replace规则（/target/replacement/g）g指全部替换 **/
        if (strings.length == 3) {
            String ruler = strings[2];
            if (ruler.startsWith("/")) {  // 替换规则 /://g
                String[] replaces = ruler.split("/");
                if (replaces.length >= 3) { // "/://g"  split后 4段
                    String target = replaces[1];    // :
                    String replacement = replaces[2]; // 空字符串
                    if (replaces.length == 4 && "g".equals(replaces[3])) {
                        propValue = propValue.replaceAll(target, replacement);
                    } else {
                        propValue = propValue.replaceFirst(target, replacement);
                    }
                }
            } else if (ruler.startsWith("{") && ruler.endsWith("}")) {
                String dflt = null;
                for (String kv : ruler.substring(1, ruler.length() - 1).split(", ?", -1)) {
                    String[] parts = kv.split(":");
                    if (parts.length == 2 && propValue.equals(parts[0])) {
                        propValue = parts[1];
                        dflt = null;
                        break;
                    } else if (parts.length == 1 && propValue.equals(parts[0])) {
                        propValue = null;
                        dflt = null;
                        break;
                    } else if (parts.length == 2 && "*".equals(parts[0])) {
                        dflt = parts[1];
                    }
                }
                if (dflt != null)
                    propValue = dflt;
            } else if ("toLower".equals(ruler)) {
                propValue = propValue.toLowerCase();
            } else if ("toUpper".equals(ruler)) {
                propValue = propValue.toUpperCase();
            }
        }
        return propValue;
    }

    @Override
    public String toString() {
        return "PropertyManager{" +
                "mContext=" + mContext +
                ", mArchiveProps=" + mArchiveProps +
                ", mNamedContexts=" + mNamedContexts +
                ", mContextMap=" + mContextMap +
                ", mOnlineConfig=" + mOnlineConfig +
                ", mFileUpdateListener=" + mFileUpdateListener +
                '}';
    }

    private Object getWildContext(String ctx) {
        try {
            int index = Integer.parseInt(ctx);
            return mContextMap.keySet().toArray()[index];
        } catch (NumberFormatException e) {
        }
        Object ctx2 = null;
        for (Entry<String, PropContext> n : mNamedContexts.entrySet()) {
            if (n.getKey() == null)
                continue;
            Object o = n.getValue().mContext;
            if (n.getKey().contains(ctx)) {
                if (ctx2 == null)
                    ctx2 = o;
                else if (ctx2 != o)
                    throw new RuntimeException(
                            "multiple contexts hit '" + ctx + "': " + ctx2 + " and " + o);
            }
        }
        for (Object o : mContextMap.keySet()) {
            if (String.valueOf(o).contains(ctx)) {
                if (ctx2 == null)
                    ctx2 = o;
                else if (ctx2 != o)
                    throw new RuntimeException(
                            "multiple contexts hit '" + ctx + "': " + ctx2 + " and " + o);
            }
        }
        return ctx2;
    }

    public void loadAll(String name, boolean sys) {
        List<File> dirs = new ArrayList<File>();
        File appCache = new File(mContext.getCacheDir(), "prop");
        File assCache = new File(appCache, "assets");
        File olnCache = new File(appCache, "online");
        File extCache = null;
        //try {
        //    extCache = mContext.getExternalCacheDir();
        //} catch (Exception e) {
        //    Log.w(TAG, "importPlugins", e);
        //}
        // if (extCache == null)
        //     extCache = new File(appCache, "external");
        // else
        //     extCache = new File(extCache, "prop");
        // if (sys)
        //     dirs.add(new File("/system/etc"));
        // dirs.add(assCache);
        // dirs.add(olnCache);
        // dirs.add(appCache);
        // dirs.add(extCache);
        // List<File> usbs = Mount.getMountPaths(mContext);
        // if (usbs != null)
        //     dirs.addAll(usbs);
        // appCache.mkdirs();
        // File lockFile = new File(appCache, "lock");
        // extractAssets(name, assCache, lockFile);
        // for (File dir : dirs) {
        //     Log.d(TAG, "loadAll: search from " + dir);
        //     for (String format : PropArchive.mArchiveClasses.keySet()) {
        //         File file = new File(dir, name + "." + format);
        //         if (file.exists()) {
        //             Log.d(TAG, "loadAll: load " + file);
        //             PropArchive.load(mArchiveProps, file, format);
        //         }
        //     }
        // }
        // applyProperties(mArchiveProps);
        // olnCache.mkdirs();
        // mOnlineConfig = new PropertyOnline(this, olnCache);
        // mFileUpdateListener = new FileUpdateListener(olnCache);
        // mFileUpdateListener.startWatching();
    }

    private class FileUpdateListener extends FileObserver {
        
        private File mPath;

        public FileUpdateListener(File path) {
            super(path.getAbsolutePath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO);
            mPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            final int action = event & FileObserver.ALL_EVENTS;
            File file = new File(mPath, path);
            switch (action) {
                case FileObserver.CLOSE_WRITE:
                case FileObserver.MOVED_TO:
                    load(file, true);
            }
        }
    }

    public void load(File file) {
        load(file, false);
    }

    public void load(File file, boolean override) {
        load(file, file.getName().replaceAll(".*\\.", ""), override);
    }
    
    public void load(File file, String format, boolean override) {
        Log.d(TAG, "load " + file);
        Map<String, Map<String, String>> props = 
                new TreeMap<String, Map<String, String>>();
        PropArchive.load(props , file, format);
        PropArchive.merge(mArchiveProps, props, override);
        applyProperties(props);
    }

    // public void loadAssets(String file, boolean override) {
    //     File dest = new File(mContext.getCacheDir(), "prop/assets/" + file);
    //     Assets.extract(mContext, "prop/" + file, dest);
    //     load(dest, override);
    // }

    public void save(File file) {
        save(file, file.getName().replaceAll(".*\\.", ""));
    }
    
    public void save(File file, String format) {
        Map<String, Map<String, String>> props =
                new TreeMap<String, Map<String, String>>();
        for (Entry<String, PropContext> e : mNamedContexts.entrySet()) {
            PropContext c = e.getValue();
            if (c.mProps == null)
                continue;
            String ctx = e.getKey();
            for (Entry<String, PropertySet> e1 : c.mProps.entrySet()) {
                Map<String, String> map = new TreeMap<String, String>();
                props.put(ctx == null ? e1.getKey() : ctx  + "/" + e1.getKey(), map);
                e1.getValue().storeProperties(map);
            }
        }
        PropArchive.save(props, file, format);
    }
    
    public void updateOnline(String name) {
        mOnlineConfig.update(name);
    }
    
    private void applyProperties(Map<String, Map<String, String>> props) {
        for (Entry<String, Map<String, String>> e : props.entrySet()) {
            String ctx = e.getKey();
            String name = null;
            int n = ctx.lastIndexOf('/');
            if (n >= 0) {
                name = ctx.substring(n + 1);
                ctx = ctx.substring(0, n);
            } else {
                name = ctx;
                ctx = null;
            }
            PropContext c = mNamedContexts.get(ctx);
            if (c != null) {
                if (c.mProps != null) {
                    PropertySet s = c.mProps.get(name);
                    if (s != null)
                        s.applyProperties(e.getValue());
                }
            }
        }
    }
    
    // private void extractAssets(String name, File dstPath, File lockFile) {
    //     Log.d(TAG, "extractAssets");
    //     FileLock lock = FileLock.lock(lockFile);
    //     try {
    //         dstPath.mkdirs();
    //         AssetManager manager = mContext.getAssets();
    //         for (String prop : manager.list("prop")) {
    //             if (!prop.startsWith(name))
    //                 continue;
    //             String name2 = "prop/" + prop;
    //             File dst = new File(dstPath, prop);
    //             Assets.extract(mContext, name2, dst);
    //         }
    //     } catch (Exception e) {
    //         Log.w(TAG, "extractAssets", e);
    //     }
    //     lock.release();
    // }

    // private static class PropCommand extends Command {
    //
    //     @Override
    //     protected String usage() {
    //         return "Usage: \n"
    //                 + "  prop set [-c context]... <propertyset> <propkey> <value>\n"
    //                 + "  prop get [-c context]... <propertyset> <propkey>\n";
    //     }
    //
    //     @Override
    //     protected String optString() {
    //         return "c:";
    //     }
    //
    //     @Override
    //     public void run(Getopt opts, BufferedReader reader, PrintWriter writer) {
    //         PropertyManager mgr = PropertyManager.getInstance();
    //         int c;
    //         List<Object> ctxs = new ArrayList<Object>();
    //         while ((c = opts.getopt()) != -1) {
    //             switch (c) {
    //             case 'c':
    //                 Object ctx = mgr.getWildContext(opts.getOptarg());
    //                 writer.println(">> context " + ctx);
    //                 ctxs.add(ctx);
    //                 break;
    //             }
    //         }
    //         List<String> argv = opts.getArgv();
    //         if (argv.isEmpty()) {
    //             opts.setError("missing subcommand");
    //             return;
    //         }
    //         String cmd = argv.get(0);
    //         if ("get".equals(cmd)) {
    //             if (argv.size() != 3) {
    //                 opts.setError("not exactly two args for get");
    //                 return;
    //             }
    //             String set = argv.get(1);
    //             String key = argv.get(2);
    //             PropertySet pset = mgr.getPropertySet(set, ctxs.toArray());
    //             if (pset == null) {
    //                 writer.println("!!property set " + set + " not found");
    //                 return;
    //             }
    //             writer.println(set + ":" + key + "=" + pset.getProp(key));
    //         } else if ("set".equals(cmd)) {
    //             if (argv.size() != 4) {
    //                 opts.setError("not exactly three args for set");
    //                 return;
    //             }
    //             String set = argv.get(1);
    //             String key = argv.get(2);
    //             String val = argv.get(3);
    //             PropertySet pset = mgr.getPropertySet(set, ctxs.toArray());
    //             if (pset == null) {
    //                 writer.println("!!property set " + set + " not found");
    //                 return;
    //             }
    //             pset.setProp(key, val);
    //             writer.println(set + ":" + key + "=" + pset.getProp(key));
    //         } else if ("load".equals(cmd)) {
    //             if (argv.size() != 3) {
    //                 opts.setError("not exactly two args for load");
    //                 return;
    //             }
    //             String format = argv.get(1);
    //             String file = argv.get(2);
    //             mgr.load(new File(file), format, true);
    //         } else if ("save".equals(cmd)) {
    //             if (argv.size() != 3) {
    //                 opts.setError("not exactly two args for save");
    //                 return;
    //             }
    //             String format = argv.get(1);
    //             String file = argv.get(2);
    //             mgr.save(new File(file), format);
    //         }
    //     }
    // }

}
