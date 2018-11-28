package utils;

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
}
