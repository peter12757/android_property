package com.peterxi.propertylib.prop;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PropXmlArchive extends PropArchive {

    private static final String TAG = "PropXmlArchive";
    
    public PropXmlArchive() {
    }
    
    private abstract class XmlNodeParser {
        abstract void parse(Node node);
    }
    
    @Override
    public void load(final InputStream in, 
            final Map<String, Map<String, String>> props) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(in);
            parseNode(document, new XmlNodeParser() {
                private Map<String, String> mPropset;
                @Override
                void parse(Node node) {
                    String name = node.getNodeName();
                    if (name.equals("properties")) {
                    } else if (name.equals("comment")) {
                        Log.d(TAG, "load comment " + node.getTextContent());
                    } else if (name.equals("include")) {
                        Log.d(TAG, "load include " + node.getTextContent());
                        include(in, node.getTextContent(), props);
                    } else if (name.equals("propset")) {
                        String pn = node.getAttributes().getNamedItem("name").getNodeValue();
                        mPropset = putPropset(props, pn);
                    } else if (name.equals("prop")) {
                        String pk = node.getAttributes().getNamedItem("key").getNodeValue();
                        String value = node.getTextContent();
                        mPropset.put(pk, value);
                    } else if (name.startsWith("#")) {
                    } else {
                        Log.w(TAG, "load unknown tag " + name);
                    }
                }
            });
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
    public void save(OutputStream os, 
            Map<String, Map<String, String>> props) throws IOException {
        PrintStream printStream = new PrintStream(os, false, "UTF-8");

        printStream.print("<?xml version=\"1.0\" encoding=\"");
        printStream.print("UTF-8");
        printStream.println("\"?>");

        printStream.println("<properties>");

        printStream.print("  <comment>");
        printStream.print(substitutePredefinedEntries("Generate by prop of basedroid"));
        printStream.println("</comment>");

        for (Entry<String, Map<String, String>> entry2 : props.entrySet()) {
            printStream.println("  <propset name=\"" + entry2.getKey() + "\">");
            for (Entry<String, String> entry : entry2.getValue().entrySet()) {
                String keyValue = (String) entry.getKey();
                String entryValue = (String) entry.getValue();
                printStream.print("    <prop key=\"");
                printStream.print(substitutePredefinedEntries(keyValue));
                printStream.print("\">");
                printStream.print(substitutePredefinedEntries(entryValue));
                printStream.println("</prop>");
            }
            printStream.println("  </propset>");
        }
        printStream.println("</properties>");
        printStream.flush();
        printStream.close();
    }

    private String substitutePredefinedEntries(String s) {
        // substitution for predefined character entities to use them safely in XML.
        s = s.replaceAll("&", "&amp;");
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        s = s.replaceAll("'", "&apos;");
        s = s.replaceAll("\"", "&quot;");
        return s;
    }

}
