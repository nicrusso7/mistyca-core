package skills.connectors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

import org.json.JSONObject;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import utils.Settings;

public class AWSLambdaConnector {
	
	
	public static final String AWS_REGION_SETTINGS_KEY = "AWS_REGION";
	public static final String AWS_IAM_SETTINGS_KEY = "AWS_LAMBDA_ACCESS_KEY";
	public static final String AWS_IAM_SECRET_SETTINGS_KEY = "AWS_LAMBDA_SECRET_KEY";
	
	//input formatted json
	//return {"code":"","message":""} as String[]
	public String[] invoke(String json_request, String skill_name) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		//define the AWS Region
		Settings settings = new Settings();
		Regions region = Regions.fromName(settings.getConnectorsSettingsValue(AWS_REGION_SETTINGS_KEY));

		//instantiate credentials
		BasicAWSCredentials credentials = new 
				BasicAWSCredentials(settings.getConnectorsSettingsValue(AWS_IAM_SETTINGS_KEY), settings.getConnectorsSettingsValue(AWS_IAM_SECRET_SETTINGS_KEY));

		//add creds
		AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))                                     
				.withRegion(region);

		//build client
		AWSLambda client = builder.build();
		
		//attach request args
		JSONObject req_json = new JSONObject(json_request);
		JSONObject formatted_req = new JSONObject();
		formatted_req.put("body", req_json.toString());

		//create an InvokeRequest with parameters
		InvokeRequest req = new InvokeRequest()
				.withFunctionName(skill_name)
				.withPayload(formatted_req.toString());

		//invoke the function
		InvokeResult result = client.invoke(req);
		String response_message = null;
		//check response code
		if(result.getStatusCode() == 200) { 
			response_message = new String(result.getPayload().array());
		}
		return new String[] {String.valueOf(result.getStatusCode()),response_message};
	}
}
