package io.github.joramkimata.datasethandler.helpers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtils {

	private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

	private DateUtils() {
		throw new RuntimeException("This is utility class, Dont instantiate");
	}

	public static LocalDateTime toTime(Long time) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
	}

	public static Long toEpoch(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	public static LocalDate parseDate(Object dateString) {

		if (dateString == null) {
			return null;
		}

		if (dateString.toString().length() == 10) {
			return LocalDate.parse(dateString.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
		}

		return LocalDate.parse(dateString.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
	}

	public static LocalDateTime parseDateTime(Object dateTimeString) {
		if (dateTimeString == null) {
			return null;
		}

		if (dateTimeString.toString().length() == 10) {
			return LocalDate.parse(dateTimeString.toString(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
		}

		if (!dateTimeString.toString().contains("T")) {
			return LocalDateTime.parse(dateTimeString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
		}

		return LocalDateTime.parse(dateTimeString.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	public static Date getNextYearDate(Date dateOfFirstAppointment) {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateOfFirstAppointment);
		calendar.add(Calendar.YEAR, 1);
		Date nextYear = calendar.getTime();
		return nextYear;
	}

	public static int getCurrentYear() {
		LocalDateTime now = LocalDateTime.now();
		return now.getYear();
	}

	public static LocalDate getDateAfterNDays(Long long1, LocalDate date) {
		return date.plusDays(long1);
	}

	public static boolean isValidEmailAddress(String email) {
		String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
		java.util.regex.Matcher m = p.matcher(email);
		return m.matches();
	}

}