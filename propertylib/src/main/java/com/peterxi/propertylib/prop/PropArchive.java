package com.peterxi.propertylib.prop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.peterxi.propertylib.prop.util.ClassWrapper;

public abstract class PropArchive {
    
    private static final String TAG = null;

    static final Map<String, Class<? extends PropArchive>> mArchiveClasses = 
            new HashMap<String, Class<? extends PropArchive>>();
    
    static final Map<String, PropArchive> mArchives = 
            new HashMap<String, PropArchive>();
    
    static {
        mArchiveClasses.put("ini", PropIniArchive.class);
        mArchiveClasses.put("xml", PropXmlArchive.class);
        mArchiveClasses.put("json", PropJsonArchive.class);
        mArchiveClasses.put("rule", PropRuleArchive.class);
    }
    
    public static void save(Map<String, Map<String, String>> props, File file, String format) {
        PropArchive ar = getArchive(format);
        if (ar != null)
            ar.save(file, props);
    }

    public static void load(Map<String, Map<String, String>> props, File file, String format) {
        PropArchive ar = getArchive(format);
        if (ar != null)
            ar.load(file, props);
    }

    protected static Map<String, String> putPropset(Map<String, Map<String, String>> props, 
            String name) {
        Map<String, String> prop = props.get(name);
        if (prop == null) {
            prop = new TreeMap<String, String>();
            props.put(name, prop);
        }
        return prop;
    }
    
    protected static void merge(Map<String, Map<String, String>> props, 
            Map<String, Map<String, String>> props2, boolean override) {
        for (Entry<String, Map<String, String>> e : props2.entrySet()) {
            merge2(putPropset(props, e.getKey()), e.getValue(), override);
        }
    }
    
    protected static void merge2(Map<String, String> propset,
            Map<String, String> propset2, boolean override) {
        if (override) {
            propset.putAll(propset2);
            return;
        }
        Iterator<Entry<String, String>> iter = propset2.entrySet().iterator();
        for (; iter.hasNext(); ) {
            Entry<String, String> e = iter.next();
            if (propset.containsKey(e.getKey()))
                iter.remove();
            else
                propset.put(e.getKey(), e.getValue());
        }
    }
    
    private static synchronized PropArchive getArchive(String format) {
        PropArchive ar = mArchives.get(format);
        if (ar != null)
            return ar;
        Class<? extends PropArchive> arc = mArchiveClasses.get(format);
        if (arc == null)
            return null;
        return ClassWrapper.wrap(arc).newInstance();
    }
    
    private Map<InputStream, File> mFileStreamMap = 
            new ConcurrentHashMap<InputStream, File>();
    
    public void load(File file, 
            Map<String, Map<String, String>> props) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            mFileStreamMap.put(in, file);
            load(in, props);
        } catch (Exception e) {
            Log.d(TAG, "load", e);
        } finally {
            if (in != null) {
                mFileStreamMap.remove(in);
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void save(File file, 
            Map<String, Map<String, String>> props) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            save(out, props);
        } catch (Exception e) {
            Log.d(TAG, "save", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public abstract void load(InputStream in, 
            Map<String, Map<String, String>> props) throws IOException;
    
    public abstract void save(OutputStream out, 
            Map<String, Map<String, String>> props) throws IOException;
    
    protected void include(InputStream in, String path, 
            Map<String, Map<String, String>> props) {
        Log.d(TAG, "include " + path);
        File file = fileFromStream(in);
        if (file == null) {
            return;
        }
        load(new File(file.getParentFile(), path), props);
    }

    protected File fileFromStream(InputStream in) {
        return mFileStreamMap.get(in);
    }
        
}
