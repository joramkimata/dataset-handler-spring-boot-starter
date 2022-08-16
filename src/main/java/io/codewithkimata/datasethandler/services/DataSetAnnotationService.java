package io.codewithkimata.datasethandler.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.MappedSuperclass;

import io.codewithkimata.datasethandler.annotations.DataSet;
import io.codewithkimata.datasethandler.annotations.DataSetField;
import io.codewithkimata.datasethandler.annotations.IgnoreDatasetField;
import io.codewithkimata.datasethandler.dtos.DataSetColumn;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataSetAnnotationService {

//	@Cacheable(value = "metadatadh", key = "#clazz", unless = "#result == null")
	public List<DataSetColumn> generateMetaData(Class<?> clazz) {
		
//		log.info("hot called class metadata");

		if (clazz.isAnnotationPresent(DataSet.class)) {

			boolean isSearchable = clazz.getAnnotation(DataSet.class).isSearchable();
			boolean isSortable = clazz.getAnnotation(DataSet.class).isSortable();

			Set<DataSetColumn> cols = new HashSet<>();

			if (clazz.getSuperclass() != null && clazz.getSuperclass().isAnnotationPresent(MappedSuperclass.class)) {

				Class<?> superClazz = clazz.getSuperclass();

				cols.addAll(Arrays.asList(superClazz.getDeclaredFields()).stream().map(field -> {

					if (field.isAnnotationPresent(IgnoreDatasetField.class)) {
						return null;
					}

					DataSetField annotationField = field.getAnnotation(DataSetField.class);

					DataSetColumn dataField = new DataSetColumn();
					dataField.setIsSearchable(isSearchable);
					dataField.setIsSortable(isSortable);
					dataField.setFieldName(field.getName());
					dataField.setType(this.convertPrimitive(field.getType()));
					dataField.setIsEnum(field.getType().isEnum());
					dataField.setDatabaseName((annotationField != null) ? annotationField.fieldName() : "");
					if (dataField.getDatabaseName().isEmpty()) {
						dataField.setDatabaseName(dataField.getFieldName());
					}

					return dataField;
				}).filter(Objects::nonNull).collect(Collectors.toSet()));
			}

			cols.addAll(Arrays.asList(clazz.getDeclaredFields()).stream().map(field -> {

				if (field.isAnnotationPresent(IgnoreDatasetField.class)) {
					return null;
				}

				DataSetField annotationField = field.getAnnotation(DataSetField.class);

				if (annotationField != null && annotationField.isObject()) {

					cols.addAll(Arrays.asList(field.getType().getDeclaredFields()).stream().map(f -> {
						String fnameString = String.format("%s.%s", field.getName(), f.getName());
						DataSetColumn dataField = new DataSetColumn();
						dataField.setIsSearchable(isSearchable);
						dataField.setIsSortable(isSortable);
						dataField.setFieldName(fnameString);
						dataField.setType(this.convertPrimitive(f.getType()));
						dataField.setIsEnum(f.getType().isEnum());
						dataField.setDatabaseName(fnameString);
						if (dataField.getDatabaseName().isEmpty()) {
							dataField.setDatabaseName(fnameString);
						}

						return dataField;
					}).collect(Collectors.toList()));
					return null;
				}

				DataSetColumn dataField = new DataSetColumn();
				dataField.setIsSearchable(isSearchable);
				dataField.setIsSortable(isSortable);

				dataField.setFieldName(field.getName());
				dataField.setType(this.convertPrimitive(field.getType()));
				dataField.setDatabaseName((field.getAnnotation(DataSetField.class) != null)
						? field.getAnnotation(DataSetField.class).fieldName()
						: "");
				if (dataField.getDatabaseName().isEmpty()) {
					dataField.setDatabaseName(dataField.getFieldName());
				}
				dataField.setIsEnum(field.getType().isEnum());
				if (field.isAnnotationPresent(DataSetField.class)) {
					field.setAccessible(true);
					String fieldName = this.getFieldName(field);
					if (!fieldName.isEmpty()) {
						dataField.setDatabaseName(fieldName);
					}

					dataField.setIsSearchable(this.getIsSearchable(field));
					dataField.setIsSortable(this.getIsSortable(field));
				}

				return dataField;
			}).filter(Objects::nonNull).collect(Collectors.toSet()));

			return cols.stream().collect(Collectors.toList());

		} else {
			log.error("Dataset not set Try putting @DataSet on your class");
		}
		return new ArrayList<>();
	}

	private String getFieldName(Field field) {
		return field.getAnnotation(DataSetField.class).fieldName();
	}

	private boolean getIsSortable(Field field) {
		return field.getAnnotation(DataSetField.class).isSortable();
	}

	private boolean getIsSearchable(Field field) {
		return field.getAnnotation(DataSetField.class).isSearchable();
	}

	private Class<?> convertPrimitive(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}

		if (type.getTypeName().equals("int")) {
			return Integer.class;
		} else if (type.getTypeName().equals("boolean")) {
			return Boolean.class;
		} else if (type.getTypeName().equals("long")) {
			return Long.class;
		}

		throw new RuntimeException("not listed primitive type");
	}
}
