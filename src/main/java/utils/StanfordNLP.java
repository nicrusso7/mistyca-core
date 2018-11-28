package utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.InvalidPropertiesFormatException;

import org.json.JSONArray;
import org.json.JSONObject;

public class StanfordNLP {
	
	public String parseSentence(String language, String sentence) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		String url = null;

		Settings settings = new Settings();
		url = settings.getStanfordNLPCoreSettingsValue(language);

		BufferedReader in;
		StringBuffer response = null;
		String deps = null;

		try {
			//add param
			String params = URLEncoder.encode(sentence, "UTF-8");

			URL obj = new URL(url + params);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");

			in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));

			String inputLine;
			response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();
			String json_res = response.toString();
			//parse

			switch(language) {
			case"en-GB":
				JSONObject sdmodel_json = new JSONObject(json_res);
				JSONArray sentence_array = sdmodel_json.getJSONArray("sentences");
				JSONObject enhancedDeps_json = sentence_array.getJSONObject(0);
				JSONArray tokens_array = enhancedDeps_json.getJSONArray("enhancedPlusPlusDependencies");
				for(int j = 0; j<tokens_array.length(); j++) {

					JSONObject token = tokens_array.getJSONObject(j);

					String dep_name = token.getString("dep");
					if(deps == null) {
						deps = dep_name;
					}
					else {
						deps += "," + dep_name;
					}
				}
				break;
			case"it-IT":
				JSONObject sdmodel = new JSONObject(json_res);
				JSONArray sentences_ = sdmodel.getJSONArray("sentences");
				JSONObject deps_json = sentences_.getJSONObject(0);
				JSONArray tokens_ = deps_json.getJSONArray("collapsed-dependencies");
				for(int j = 0; j<tokens_.length(); j++) {

					JSONObject token = tokens_.getJSONObject(j);

					String dep_name = token.getString("dep");
					if(deps == null) {
						deps = dep_name;
					}
					else {
						deps += "," + dep_name;
					}
				}
				break;
			}

		}
		catch(Exception ex) {
			//server offline
		}

		return deps.toLowerCase();
	}
}
