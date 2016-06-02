package pl.wedt.reuters.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Michał Żakowski
 */
public class PropertiesLoader {
    private Properties properties;

    public PropertiesLoader(String path) {
        try {
            InputStream is = new FileInputStream(path);
            properties = new Properties();
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getProperties() {
        return properties;
    }
}
