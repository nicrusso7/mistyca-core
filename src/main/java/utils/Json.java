package utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Json {
	
	private final String ARGS_MARKER = "{=}";

	public String fillJSON(String[] args) {
		JSONObject template = new JSONObject();
		for(String arg : args) {
			//split value and key
			String[] pair = arg.split(ARGS_MARKER);
			//add them to json request
			if(template.has(pair[0])) {
				int index = template.getJSONObject(pair[0]).keySet().size() + 1;
				//add to template
				template.getJSONObject(pair[0]).put(String.valueOf(index), pair[1]);
			}
			else {
				JSONObject keyword_class = new JSONObject();
				keyword_class.put("0", pair[1]);
				template.put(pair[0], keyword_class);
			}
		}
		return template.toString();
	}
	
	public Map<String,String> parseJSONObject(String json) {
		JSONObject target_json = new JSONObject(json);
		Map<String,String> result = new HashMap<String,String>();
		target_json.keySet().stream().forEach(key-> {
			result.put(key,target_json.get(key).toString());
		});
		return result;
	}
	
	public Map<String,String> parseJSONObject(String json, String target) {
		JSONObject template = new JSONObject(json);
		Map<String,String> result = new HashMap<String,String>();
		JSONObject target_json = template.getJSONObject(target);	
		target_json.keySet().stream().forEach(key-> {
			result.put(key,target_json.get(key).toString());
		});
		return result;
	}
	
	public String[] parseJSONArray(String target) {
		JSONArray array = new JSONArray(target);
		List<String> result = new LinkedList<String>();
		array.forEach(value-> { result.add(value.toString()); });
		String[] array_result = new String[result.size()];
		array_result = result.toArray(array_result);
		return array_result;
	}
	
	public Map<Integer, Map<String,String>> parseJSONArray(String json, String target) {
		JSONObject template = new JSONObject(json);
		JSONArray array = template.getJSONArray(target);
		Map<Integer, Map<String,String>> result = new HashMap<Integer,Map<String,String>>();
		for(int i=0;i<array.length();i++) {
			Map<String,String> args = new HashMap<String,String>();
			JSONObject target_json = array.getJSONObject(i);
			target_json.keySet().stream().forEach(key-> {
				String value = target_json.get(key).toString();
				args.put(key,value);
			});
			result.put(i,args);
		}
		
		return result;
	}
}
