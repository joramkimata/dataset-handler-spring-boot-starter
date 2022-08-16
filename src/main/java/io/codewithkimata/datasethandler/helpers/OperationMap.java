package io.codewithkimata.datasethandler.helpers;

import java.util.HashMap;
import java.util.Map;

import io.codewithkimata.datasethandler.enums.SearchOperation;

public class OperationMap {

	public static Map<String, String> operationMap = new HashMap<String, String>() {
		{
			put("GT", ">");
			put("LT", "<");
			put("GTE", ">=");
			put("LTE", "<=");
			put("NE", "!=");
			put("EQ", "=");
			put("ILK", "ILIKE");
			put("IN", "IN");
			put("NIN", "NOT IN");
			put("LK", "LIKE");
			put("BTN","BETWEEN");

		}
	};

	public static String operation(SearchOperation operation) {
		return operationMap.get(operation.toString());
	}

}
