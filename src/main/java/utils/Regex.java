package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
	public boolean contains(String word, String[] target) {
		
		boolean isContained = false;

		for(int i =0; i<target.length; i++) {
			String cns = target[i];
			String lwr_cns = cns.toLowerCase();
			Pattern p = Pattern.compile("(^"+ word +"(\\'s|\\?|$)| "+word+"(\\'s|\\?|$)|^"+word+"(\\'s|\\?| )| "+word+"(\\'s|\\?| ))");
			Matcher m = p.matcher(lwr_cns.toLowerCase());
			if(m.find()) {
				isContained = true;
				break;
			}
		}

		return isContained;
	}
	
	public boolean containsWord(String word, String target) {
		String lwr_cns = target.toLowerCase();
		Pattern p = Pattern.compile("(^"+ word +"(\\'s|\\?|$)| "+word+"(\\'s|\\?|$)|^"+word+"(\\'s|\\?| )| "+word+"(\\'s|\\?| ))");
		Matcher m = p.matcher(lwr_cns.toLowerCase());
		if(m.find()) {
			return true;
		}
		return false;
	}
}
