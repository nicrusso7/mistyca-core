package skills;

import skills.connectors.AWSLambdaConnector;

public class SkillHandler {

	public String[] launchSkill(String connector_name, String skill_name, String args_json) {
		String[] response = null;
//		try {
//			Class<?> c = Class.forName("skills.connectors." + connector_name);
//			Constructor<?> cons = c.getConstructor();
//			Object object = cons.newInstance();
//			response = (String[]) object.getClass().getMethod("invoke", String.class, String.class).invoke(object, args_json, skill_name);
//		} 
//		catch (Exception e) {
//			//connector doesn't work
//			response = new String[2];
//			response[0] = "500";
//		}
		//TODO more connectors..
		
		AWSLambdaConnector connector = new AWSLambdaConnector();
		try {
			response = connector.invoke(args_json, skill_name);
		} catch (Exception e) {
			//connector doesn't work
			response = new String[2];
			response[0] = "500";
			response[1] = e.getMessage();
		}
		return response;
	}
}
