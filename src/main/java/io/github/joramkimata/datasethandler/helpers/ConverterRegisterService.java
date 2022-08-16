package io.github.joramkimata.datasethandler.helpers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

public class ConverterRegisterService<T> {

	public static void register() {
		ConvertUtils.register(new Converter() {
			@Override
			public <T> T convert(Class<T> aClass, Object o) {
				if (o == null) {
					return null;
				}

				if (o.toString().length() > 10) {
					return aClass.cast(LocalDate.parse(String.valueOf(o).substring(0, 10)));
				}

				return aClass.cast(LocalDate.parse(String.valueOf(o)));
			}
		}, LocalDate.class);

		ConvertUtils.register(new Converter() {
			@Override
			public <T> T convert(Class<T> aClass, Object o) {
				if (o == null) {
					return null;
				}

				if (o.toString().contains("T")) {
					return aClass.cast(LocalDateTime.parse(o.toString()));
				}

				return aClass.cast(Timestamp.class.cast(o).toLocalDateTime());

			}
		}, LocalDateTime.class);

		ConvertUtils.register(new Converter() {
			@Override
			public <T> T convert(Class<T> aClass, Object o) {
				if (o == null) {
					return null;
				}
				return aClass.cast(Long.valueOf(o.toString()));
			}
		}, Long.class);

		ConvertUtils.register(new Converter() {
			@Override
			public <T> T convert(Class<T> aClass, Object o) {
				if (o == null) {
					return null;
				}

				return aClass.cast(new BigDecimal(o.toString()));
			}
		}, BigDecimal.class);

	}

}
