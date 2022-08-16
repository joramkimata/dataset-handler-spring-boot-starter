package io.codewithkimata.datasethandler.helpers;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ERPHelper {


	public static void print(Object obj) {
		if (obj == null) {
			log.error("You've passed null object (from erp helper)");
		} else {

			ObjectMapper mapper = new ObjectMapper();
			String className = "";

			className = obj.getClass().getSimpleName();

			System.out.println("--------------------------" + className
					+ "----------------------------------------------------------");
			try {
				System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
			} catch (JsonProcessingException e) {

				System.err.println("########################ERROR from erphelper########################");
				e.printStackTrace();
			}
			System.out.println(
					"-----------------------------------------------------------------------------------------------------");
		}
	}

	public static void print(Object obj, String headerMsg) {

		if (obj == null) {
			log.error("You've passed null object (from erp helper)");
		} else {
			try {
				ObjectMapper mapper = new ObjectMapper();
				System.out.println("");
				System.out.println("");
				String className;

				className = obj.getClass().getSimpleName();

				String msg = "--------------------------" + className + " (" + headerMsg
						+ ")----------------------------------------------------------";

				System.out.println(msg);

				System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));

			} catch (Exception e) {
				System.err.println("########################ERROR from erphelper########################");
				e.printStackTrace();
			}

			System.out.println(
					"------------------------------  End of " + headerMsg + " ------------------------------------");
			System.out.println("");
			System.out.println("");
		}
	}

	public static String resourceMessage(Object id) {
		return " resource-id=" + id.toString();
	}

	public static String logging(String institutionId, String userIdentity, String msg) {
		return "#1 from #2 #3 ".replace("#1", userIdentity).replace("#2", institutionId).replace("#3", msg);
	}

	public static String accessMessage(String actionPastTense, Object entryId) {
		return " #1 a resource with id #2".replace("#1", actionPastTense).replace("#2", entryId.toString());
	}

	public static String accessMessageList(String actionPastTense, String extraInfo) {
		return " #1 a list resources (#2)".replace("#1", actionPastTense).replace("#2", extraInfo);
	}

	public static void printParams(Object... objects) {

		ERPHelper.print(objects, "List of parameters");

	}

	public static void printParams(String title, Object... objects) {

		ERPHelper.print(objects, title);

	}

}
