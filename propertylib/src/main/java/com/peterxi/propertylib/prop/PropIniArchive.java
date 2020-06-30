package com.peterxi.propertylib.prop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/*
 * [/debuglog]=
 * DUMP_OBJECT=true
 * GLOBAL_PRIORITY=verbose
 * SHOW_TOAST=true
 */

public class PropIniArchive extends PropArchive {

    public PropIniArchive() {
    }
    
    @Override
    public void load(final InputStream in, 
            final Map<String, Map<String, String>> props) throws IOException {
        Properties prop = new Properties() {
            private static final long serialVersionUID = 1L;
            private Map<String, String> mProps = new HashMap<String, String>();
            @Override
            public synchronized Object put(Object key, Object value) {
                String k = (String) key;
                String v = (String) value;
                if (k.startsWith("[") && k.endsWith("]")) {
                    String s = k.substring(1, k.length() - 1);
                    mProps = putPropset(props, s);
                    return null;
                } else if (k.startsWith("[") && v.endsWith("]")) {
                    k = k.substring(1, k.length());
                    v = v.substring(0, v.length() - 1);
                    if (k.equals("include")) {
                        PropIniArchive.this.include(in, v, props);
                        return null;
                    } else {
                        return null;
                    }
                }
                return mProps.put(k, v);
            }
        };
        Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
        prop.load(reader);
        reader.close();
    }

    @Override
    public void save(OutputStream out, 
            final Map<String, Map<String, String>> props) throws IOException {
        Properties prop = new Properties() {
            private static final long serialVersionUID = 1L;
            private Iterator<Map.Entry<String, Map<String, String>>> mIter = 
                    props.entrySet().iterator();
            private Iterator<Map.Entry<String, String>> mIter2 = null ;
            public synchronized java.util.Set<Map.Entry<Object,Object>> entrySet() {
                return new AbstractSet<Map.Entry<Object,Object>>() {
                    @Override
                    public Iterator<java.util.Map.Entry<Object, Object>> iterator() {
                        return new Iterator<Map.Entry<Object,Object>>() {
                            @Override
                            public void remove() {
                            }
                            @Override
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            public Map.Entry<Object, Object> next() {
                                if (mIter2 != null && mIter2.hasNext()) {
                                    return (Map.Entry<Object, Object>) (Map.Entry) mIter2.next();
                                } else if (mIter.hasNext()) {
                                    final Map.Entry<String, Map<String, String>> map = mIter.next();
                                    mIter2 = map.getValue().entrySet().iterator();
                                    return new Map.Entry<Object, Object>() {
                                        @Override
                                        public Object getKey() {
                                            return "[" + map.getKey() + "]";
                                        }
                                        @Override
                                        public Object getValue() {
                                            return "";
                                        }
                                        @Override
                                        public Object setValue(Object object) {
                                            return null;
                                        }
                                    };
                                } else {
                                    return null;
                                }
                            }
                            @Override
                            public boolean hasNext() {
                                return (mIter2 != null && mIter2.hasNext()) || mIter.hasNext();
                            }
                        }; // Iterator
                    } // iterator
                    @Override
                    public int size() {
                        return 0;
                    }
                }; // AbstractSet
            }
        };
        Writer writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));
        prop.store(writer, "Generate by prop of basedroid");
        writer.flush();
        writer.close();
    }

}
