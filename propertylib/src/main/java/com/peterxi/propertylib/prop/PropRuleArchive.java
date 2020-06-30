package com.peterxi.propertylib.prop;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.peterxi.propertylib.prop.PropArchive;
import com.peterxi.propertylib.prop.PropertyManager;

/*
 * <?xml version="1.0" encoding="utf-8"?>
 * <root module="sys" prop="config">
 *   <param name="hardware">sys:hardware</param>
 *   <rule>default_value</rule>
 *   <rule hardware="mstar">mstar_value</rule>
 * </root>
 */

public class PropRuleArchive extends PropArchive {

    private static final String KEY_ROOT = "root";
    private static final String KEY_MODULE = "module";
    private static final String KEY_PROP = "prop";
    private static final String KEY_VALUE = "value";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_PARAM = "param";
    private static final String KEY_NAME = "name";
    private static final String KEY_RULE = "rule";

    private abstract class XmlNodeParser {
        abstract void parse(Node node);
    }

    protected static final String TAG = "PropRuleArchive";
    
    @Override
    public void load(InputStream in, final Map<String, Map<String, String>> props)
            throws IOException {
        final PropertyManager pm = PropertyManager.getInstance();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(in);
            parseNode(document, new XmlNodeParser() {
                @Override
                void parse(Node node) {
                    String name = node.getNodeName();
                    NamedNodeMap attrs = node.getAttributes();
                    if (name.equals(KEY_ROOT)) {
                        String module = attrs.getNamedItem(KEY_MODULE).getNodeValue();
                        String prop = attrs.getNamedItem(KEY_PROP).getNodeValue();
                        putPropset(props, KEY_ROOT).put(KEY_MODULE, module);
                        putPropset(props, KEY_ROOT).put(KEY_PROP, prop);
                    } else {
                        Map<String, String> params = putPropset(props, KEY_PARAMS);
                        if (name.equals(KEY_PARAM)) {
                            String param = attrs.getNamedItem(KEY_NAME).getNodeValue();
                            String value = node.getTextContent();
                            value = pm.getPropertyValue(null, value);
                            //Log.d(TAG, "load param " + param + "=" + value);
                            params.put(param, value);
                        } else if (name.equals(KEY_RULE)) {
                            String propValue = node.getTextContent();
                            for (int i = 0; i < attrs.getLength(); ++i) {
                                Node attr = attrs.item(i);
                                String cond = attr.getNodeName();
                                String value = attr.getNodeValue();
                                String env = params.get(cond);
                                if (env == null && !params.containsKey(cond)) {
                                    env = pm.getPropertyValue(null, cond);
                                    env = env == cond ? null : env;
                                    params.put(cond, env);
                                }
                                if (value != null && !value.equals(env)) {
                                    propValue = null;
                                    //Log.d(TAG, "load not match " + cond + "=" + value);
                                    break;
                                }
                            }
                            if (propValue != null) {
                                putPropset(props, KEY_ROOT).put(KEY_VALUE, propValue);
                            }
                        } else if (name.startsWith("#")) {
                        } else {
                            Log.w(TAG, "load unknown tag " + name);
                        }
                    }
                }
            }); // parseNode
            Log.d(TAG, "load"+props);
            Map<String, String> root = putPropset(props, KEY_ROOT);
            putPropset(props, root.get(KEY_MODULE)).put(root.get(KEY_PROP), root.get(KEY_VALUE));
            props.remove(KEY_ROOT);
            props.remove(KEY_PARAMS);
        } catch (Exception e) {
            Log.w(TAG, "load", e);
        }
    }

    private void parseNode(Node node, XmlNodeParser parser) {
        parser.parse(node);
        if (node.hasChildNodes()) {
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                parseNode(list.item(i), parser);
            }
        }
    }

    @Override
    public void save(OutputStream out, Map<String, Map<String, String>> props)
            throws IOException {
        throw new UnsupportedOperationException();
    }

}
