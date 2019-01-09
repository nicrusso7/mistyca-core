package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class Settings {

	public Settings() { }
	
	public String getConnectorsSettingsValue(String constant_name) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String iconConfigPath = rootPath + "connectors.xml";
		Properties iconProps = new Properties();
		iconProps.loadFromXML(new FileInputStream(iconConfigPath));
		return iconProps.getProperty(constant_name);
	}
	
	public String getStanfordNLPCoreSettingsValue(String constant_name) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String iconConfigPath = rootPath + "stanfordNLPCore.xml";
		Properties iconProps = new Properties();
		iconProps.loadFromXML(new FileInputStream(iconConfigPath));
		return iconProps.getProperty(constant_name);
	}
	
	public String getSettingsValue(String constant_name) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String iconConfigPath = rootPath + "settings.xml";
		Properties iconProps = new Properties();
		iconProps.loadFromXML(new FileInputStream(iconConfigPath));
		return iconProps.getProperty(constant_name);
	}
}
