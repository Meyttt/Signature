package ru.voskhod.signature.tests;

import java.io.*;
import java.util.Properties;

public class Config {
    public static Properties prop;

    public Config(String filename) throws IOException {
        this.prop = initProperties(filename);
    }

    public String get(String key)  {
        String val = prop.getProperty(key);
        try {
            return new String(val.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return val;
    }

    private Properties initProperties(String filename) throws IOException {
        Properties p = new Properties();
        InputStream inputStream = new FileInputStream(filename);
        if (inputStream != null) {
            p.load(inputStream);
        } else {
            throw new FileNotFoundException("Config file not found: " + filename);
        }
        return p;
    }
}