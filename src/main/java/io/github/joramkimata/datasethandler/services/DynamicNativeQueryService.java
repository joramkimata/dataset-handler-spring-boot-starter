package io.github.joramkimata.datasethandler.services;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.mapping.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import io.github.joramkimata.datasethandler.annotations.DataSet;
import io.github.joramkimata.datasethandler.dtos.DataPage;
import io.github.joramkimata.datasethandler.dtos.DataSetColumn;
import io.github.joramkimata.datasethandler.dtos.QueryData;
import io.github.joramkimata.datasethandler.enums.SearchOperation;
import io.github.joramkimata.datasethandler.helpers.ConverterRegisterService;
import io.github.joramkimata.datasethandler.helpers.DateUtils;
import io.github.joramkimata.datasethandler.helpers.OperationMap;
import io.github.joramkimata.datasethandler.inputs.DataRequestInput;
import io.github.joramkimata.datasethandler.inputs.MandatoryFilterInput;
import io.github.joramkimata.datasethandler.specifications.SearchCriteria;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicNativeQueryService<T> {

	@Value("${datahandler.default-page-size:200}")
	private int defaultPageSize;

	@Autowired
	private EntityManager em;

	private Map<String, DataSetColumn> fields = new HashMap<>();

	private Map<String, DataSetColumn> fieldsDb = new HashMap<>();

	private String distinctField = "";

	/**
	 * Gets the data.
	 *
	 * @param input                 the input
	 * @param mustHaveBasicInternal the must have basic internal filters
	 * @param type                  the type
	 * @param withMetaData          the with meta data
	 * @return the data
	 */
	public QueryData<T> getData(DataRequestInput input, Map<String, Object> mustHaveBasicInternal, Class<?> type,
			boolean withMetaData) {

		if (!type.isAnnotationPresent(DataSet.class)) {
			throw new RuntimeException("DataSet annotation not present, Please annotate the class with DataSet");
		}

		this.distinctField = type.getAnnotation(DataSet.class).distinctField();

		int page = (input.getPage() < 1) ? 1 : input.getPage();

		if (withMetaData) {
			return new QueryData<>(
					this.dynamic(input, mustHaveBasicInternal, type, PageRequest.of(page, input.getPageSize())),
					new DataSetAnnotationService().generateMetaData(type));
		}

		return new QueryData<>(
				this.dynamic(input, mustHaveBasicInternal, type,
						PageRequest.of(page,
								(input.getPageSize() > defaultPageSize) ? defaultPageSize : input.getPageSize())),
				new ArrayList<>());

	}

	/**
	 * Dynamic.
	 *
	 * @param input                 the input
	 * @param mustHaveBasicInternal the must have basic internal
	 * @param type                  the type
	 * @param page                  the page
	 * @return the data page
	 */
	public DataPage<T> dynamic(DataRequestInput input, Map<String, Object> mustHaveBasicInternal, Class<?> type,
			PageRequest page) {

		this.fields = this.getMyFieldsDb(type);

		this.fields.entrySet().forEach(e -> {
			this.fieldsDb.put(e.getValue().getDatabaseName(), e.getValue());
		});

//		System.err.println(fields);
		String table = type.getAnnotation(DataSet.class).table();
		int pageSize = (page.getPageSize() > 200) ? 200 : page.getPageSize();

		// picking search field if any
		Set<SearchCriteria> searchFields = input.getFields().stream().filter(f -> f.getIsSearchable()).map(f -> {
			if (f.getSearchValue() != null && !f.getSearchValue().isEmpty()) {
				return new SearchCriteria(f.getFieldName(), f.getSearchValue(), f.getOperation());
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toSet());

		// picking order in search fields if any
		Map<String, Direction> orders = input.getFields().stream().filter(f -> f.getIsSortable())
				.collect(Collectors.toMap(a -> a.getFieldName(), a -> a.getOrderDirection()));

		// internal must have parameters
		Map<String, Object> mustHave = mustHaveBasicInternal.entrySet().stream()
				.collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

		Map<String, Object> mustHaveFilterMap = new HashMap<>();
		if (!input.getMustHaveFilters().isEmpty()) {

			input.getMustHaveFilters().forEach(mh -> {
				Class<?> fieldClass = fields.get(mh.getFieldName()).getType();
//
//				log.info("field: {} value1: {} value2: {} type: {}", mh.getFieldName(), mh.getValue1(), mh.getValue2(),
//						fields.get(mh.getFieldName()));

				try {
					Object value1 = mh.getValue1();
					Object value2 = mh.getValue2();

					if (!fieldClass.equals(String.class)) {

						if (Temporal.class.isAssignableFrom(fieldClass)
								&& ChronoLocalDateTime.class.isAssignableFrom(fieldClass)) {
							value1 = DateUtils.parseDateTime(value1);
							value2 = (value2 != null) ? DateUtils.parseDateTime(value2) : value2;

						} else if (Temporal.class.isAssignableFrom(fieldClass)
								&& ChronoLocalDate.class.isAssignableFrom(fieldClass)) {
							value1 = DateUtils.parseDate(value1);
							value2 = (value2 != null) ? DateUtils.parseDate(value2) : value2;

						} else if (fieldClass.equals(BigDecimal.class)) {
							value1 = new BigDecimal(mh.getValue1());
							value2 = (value2 == null) ? null : new BigDecimal(mh.getValue2());

						}

						else {
							Method method = fieldClass.getDeclaredMethod("valueOf", String.class);
							value1 = method.invoke(fieldClass, mh.getValue1());
							value2 = (value2 == null) ? null : method.invoke(fieldClass, mh.getValue2());
						}

					}

					if (mh.getOperation().equals(SearchOperation.BTN)) {
						mustHaveFilterMap.put(mh.getFieldName() + "mst1", value1);
						mustHaveFilterMap.put(mh.getFieldName() + "mst2", value2);
					} else {
						mustHaveFilterMap.put(mh.getFieldName() + "msf", value1);
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (fieldClass.isEnum()) {
						java.lang.String.valueOf("");
					}
				}

			});

		}

		// end of order by

		String mustHaveSql = this.getMustSql(mustHave, input.getMustHaveFilters());

		String countQuery = this.getCountSql(mustHaveSql, table);

		String searchSql = this.getSearchSql(searchFields);

		String orderString = this.getOrderSql(orders);

//		System.err.println(this.getFullSql(mustHaveSql, orderString, searchSql, table, false));

		Query query = em.createNativeQuery(this.getFullSql(mustHaveSql, orderString, searchSql, table, false),
				Tuple.class);

		Long totalCount = this.getTotalRecords(countQuery, mustHave, mustHaveFilterMap);

		Map<String, Object> parameters = new HashMap<>();

		// assign search parameters parameters
		searchFields.forEach(s -> {

			Object searchValue = s.getValue();

			if ((Arrays.asList(SearchOperation.ILK, SearchOperation.LK)).contains(s.getOperation())) {
				searchValue = "%" + searchValue + "%";
			}
			parameters.put(s.getKey(), searchValue);

		});

		// assign musthave parameters query.setParameter("mst" + s.getKey(),
		// s.getValue())
		mustHave.entrySet().forEach(s -> parameters.put(s.getKey() + "mst", s.getValue()));

		mustHaveFilterMap.entrySet().forEach(s -> parameters.put(s.getKey(), s.getValue()));

//		System.err.println(parameters);

		parameters.entrySet().forEach(p -> query.setParameter(p.getKey(), p.getValue()));
		Integer filtersCount = 0;
		if (!mustHaveFilterMap.isEmpty() || !searchFields.isEmpty()) {

			String resultCountWithoutPaginationSql = this.getFullSql(mustHaveSql, orderString, searchSql, table, true);

//			System.err.println(resultCountWithoutPaginationSql);

//			System.err.println(parameters);

			Query queryResultSetCount = em.createNativeQuery(resultCountWithoutPaginationSql);

			parameters.entrySet().forEach(p -> queryResultSetCount.setParameter(p.getKey(), p.getValue()));

			filtersCount = Integer.valueOf(queryResultSetCount.getSingleResult().toString());
		}
		// set limits
		query.setMaxResults(pageSize);
		query.setFirstResult(pageSize * (page.getPageNumber() - 1));

		List<Tuple> data = query.getResultList();

//		System.err.println(data.size());

		Map<String, String> pojosFields = fields.entrySet().stream()
				.collect(Collectors.toMap(p -> p.getValue().getDatabaseName(), p -> p.getValue().getFieldName()));
		List<T> list = data.stream().map(tuple -> {

			Map<String, Object> map = new HashMap<>();

//			tuple.getElements().stream()
//					.collect(Collectors.toMap(a -> a.getAlias(), a -> tuple.get(a.getAlias(), Object.class)));
//			
//			
			ConverterRegisterService.register();
//			System.err.println("\n\n");

			try {
				T dataInstance = (T) type.newInstance();
				tuple.getElements().forEach(a -> {
					String fld = pojosFields.get(a.getAlias());
					if (fld != null) {

//					if (fld.equals("id")) {
						Object value = tuple.get(a.getAlias());

//						System.err.println("mbona hii poa: "+ConvertUtils.convert(tuple.get(a.getAlias()), Long.class));

//						System.err.println("field: " + a.getJavaType());

						DataSetColumn fieldInfo = fields.get(fld);
//						System.err.println("type: "+fields.get(fld).getType());
						if (fields.get(fld).getType().isEnum()) {
							try {
//								System.err.println("value: " + value);

								value = (value == null) ? null
										: fieldInfo.getType().getMethod("valueOf", String.class).invoke(dataInstance,
												value.toString());
							} catch (Exception e) {
								e.printStackTrace();
							}
							map.put(fld, value);
						} else {
							map.put(fld, ConvertUtils.convert(tuple.get(a.getAlias()), fields.get(fld).getType()));
						}

//						System.err.println("done converting");
//					}
					}

				});
//				System.err.println("\n\n\n");

				BeanUtils.populate(dataInstance, map);
				return dataInstance;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}).filter(Objects::nonNull).collect(Collectors.toList());

		int records = list.size();

		int pages = ((int) (Math.ceil((double) totalCount / pageSize)));

		DataPage<T> dataPage = new DataPage<>();
		dataPage.setTotalPages(pages);
		dataPage.setTotalRecords(totalCount);
		dataPage.setCurrentPage(page.getPageNumber());
		dataPage.setData(list);
		dataPage.setNumberOfRecords(records);
		dataPage.setPageSize(page.getPageSize());
		dataPage.setRecordsFilteredCount(filtersCount);
		dataPage.setFirst(page.getPageNumber() == 1);
		dataPage.setLast(page.getPageSize() == pages);
		dataPage.setHasNext(page.getPageNumber() < pages);
		dataPage.setHasPrevious(page.getPageNumber() > 1);

		return dataPage;
//
//		// set pagenation and return

	}

	/**
	 * Gets the total records.
	 *
	 * @param baseQuery         the base query
	 * @param mustHave          the must have
	 * @param mustHaveFilterMap the must have filter map
	 * @return the total records
	 */
	public Long getTotalRecords(String baseQuery, Map<String, Object> mustHave, Map<String, Object> mustHaveFilterMap) {

//		System.err.println(baseQuery);

		Query query = em.createNativeQuery(baseQuery);

		mustHave.entrySet().forEach(s -> query.setParameter(s.getKey() + "mst", s.getValue()));

		mustHaveFilterMap.entrySet().forEach(s -> query.setParameter(s.getKey(), s.getValue()));

		return Long.valueOf(query.getSingleResult().toString());

	}

	public Object valueToSting(Object val, String key) {
//		System.err.println(key);
		DataSetColumn field = this.fields.get(key);
//		System.err.println(field);
		if (field.getIsEnum()) {
			return val.toString();
		}

		return val;

	}

	/**
	 * Gets the must sql.
	 *
	 * @param mustHaveInternal     the must have internal
	 * @param mustHaveFilterFields the must have filter fields
	 * @return the must sql
	 */
	private String getMustSql(Map<String, Object> mustHaveInternal, List<MandatoryFilterInput> mustHaveFilterFields) {

		String mustHaveSql = "";
//		System.err.println(fields);

		if (!mustHaveInternal.isEmpty()) {
			List<String> predicates = new ArrayList<>();
			mustHaveInternal.entrySet().forEach(s -> {

				if (fields.get(s.getKey()) == null) {
					throw new RuntimeException(s.getKey() + " not found in the fields list");
				}
				if (Collection.class.isAssignableFrom(s.getValue().getClass())
						|| List.class.isAssignableFrom(s.getValue().getClass())) {
					predicates.add(
							String.format("item.%s IN(:%s)", fields.get(s.getKey()).getDatabaseName(), s.getKey() + "mst"));
				} else {
					predicates
							.add(String.format("item.%s=:%s", fields.get(s.getKey()).getDatabaseName(), s.getKey() + "mst"));
				}

			});

			mustHaveFilterFields.forEach(f -> {

				// revert to this incase of errors
//				if (f.getOperation().equals(SearchOperation.BTN)) {
//					predicates.add(String.format("%s %s :%s AND :%s", fields.get(f.getFieldName()).getDatabaseName(),
//							OperationMap.operation(f.getOperation()), f.getFieldName() + "mst1",
//							f.getFieldName() + "mst2"));
//				} else {
//					predicates.add(String.format("%s%s:%s", fields.get(f.getFieldName()).getDatabaseName(),
//							OperationMap.operation(f.getOperation()),
//							fields.get(f.getFieldName()).getDatabaseName() + "msf"));
//				}

				if (f.getOperation().equals(SearchOperation.BTN)) {
					predicates.add(String.format("item.%s %s :%s AND :%s", fields.get(f.getFieldName()).getDatabaseName(),
							OperationMap.operation(f.getOperation()), f.getFieldName() + "mst1",
							f.getFieldName() + "mst2"));
				} else {

					if (fields.get(f.getFieldName()).getIsEnum()) {
//						System.err.println("ENUM jamana" + f.getClass());
						predicates.add(
								String.format("item.%s%sCAST(:%s AS TEXT)", fields.get(f.getFieldName()).getDatabaseName(),
										OperationMap.operation(f.getOperation()), f.getFieldName() + "msf"));
					} else {
						predicates.add(String.format("item.%s%s:%s", fields.get(f.getFieldName()).getDatabaseName(),
								OperationMap.operation(f.getOperation()), f.getFieldName() + "msf"));
					}
				}

			});
			mustHaveSql = "  (" + String.join(" AND ", predicates) + ") ";
		}
		return mustHaveSql;
	}

	/**
	 * Gets the order sql.
	 *
	 * @param ordersMap the orders map
	 * @return the order sql
	 */
	private String getOrderSql(Map<String, Direction> ordersMap) {
		// order by
		List<String> orders = ordersMap.entrySet().stream()
				.map(m -> String.format("item.%s %s", fields.get(m.getKey()).getDatabaseName(), m.getValue().toString()))
				.collect(Collectors.toList());

		String orderString = "";

		if (!orders.isEmpty()) {
			orderString = "ORDER BY " + String.join(",", orders);
		}

		return orderString;
	}

	/**
	 * Gets the full sql.
	 *
	 * @param mustHaveSql  the must have sql
	 * @param orderString  the order string
	 * @param searchSql    the search sql
	 * @param type         the type
	 * @param fullSQLCount the full SQL count
	 * @return the full sql
	 */
	private String getFullSql(String mustHaveSql, String orderString, String searchSql, String table,
			boolean fullSQLCount) {
		String baseQuery;

		if (fullSQLCount) {
			baseQuery = String.format("SELECT %s FROM %s item ",
					(this.distinctField.isEmpty()) ? "count(item)" : "distinct(count(item." + distinctField + ")) ", table);
		} else {
			baseQuery = String.format("SELECT %s item.* FROM %s item ",
					(this.distinctField.isEmpty()) ? "" : "distinct(item." + distinctField + ") as dist_field, ", table);

		}

		baseQuery = baseQuery + ((!mustHaveSql.isEmpty() || !searchSql.isEmpty()) ? " WHERE " : "");

		baseQuery = baseQuery + mustHaveSql + ((!mustHaveSql.isEmpty() && !searchSql.isEmpty()) ? " AND " : "")
				+ ((!searchSql.isEmpty()) ? searchSql : "");
		if (!fullSQLCount) {
			baseQuery += (" " + orderString);
		}

		return baseQuery;
	}

	/**
	 * Gets the count sql.
	 *
	 * @param mustHaveSql the must have sql
	 * @param type        the type
	 * @return the count sql
	 */
	private String getCountSql(String mustHaveSql, String table) {
		String countQuery = String.format("SELECT %s FROM %s item ",(this.distinctField.isEmpty())?"count(item.id)":"distinct(count(item."+distinctField+")) ", table);

		countQuery = countQuery + ((!mustHaveSql.isEmpty()) ? " WHERE " + mustHaveSql : "");

		return countQuery;
	}

	/**
	 * Gets the search sql.
	 *
	 * @param searchFields the search fields
	 * @return the search sql
	 */
	private String getSearchSql(Set<SearchCriteria> searchFields) {
		String searchSql = "";

		if (!searchFields.isEmpty()) {

			List<String> predicates = new ArrayList<>();
			searchFields.forEach(s -> {
				String str = "";
				str += String.format(" LOWER(CAST(item.%s as text)) ", fields.get(s.getKey()).getDatabaseName());
				str += OperationMap.operation(s.getOperation());
				if (!(Arrays.asList(SearchOperation.ILK, SearchOperation.LK)).contains(s.getOperation())) {
					str += String.format(":%s ", s.getKey());
				} else {
					str += String.format(" LOWER(CAST(:%s as text))", s.getKey());
				}

				predicates.add(str);

			});

			searchSql = " (" + String.join(" OR ", predicates) + ") ";

		}

		return searchSql;
	}

	/**
	 * Enum string search.
	 *
	 * @param field       the field
	 * @param searchValue the search value
	 * @param dbFields    the db fields
	 * @return the string
	 */
	private String enumStringSearch(String field, String searchValue, Map<String, Class<?>> dbFields) {

		List<String> values = Arrays.asList(dbFields.get(field).getEnumConstants()).stream().map(i -> i.toString())
				.collect(Collectors.toList());
		Double score = 0.0;
		String highestScoreEnumConstant = "";
		for (String value : values) {
			Double tmp = StringUtils.getJaroWinklerDistance(searchValue.toLowerCase(), value.toLowerCase());
			tmp *= 100;

			if (value.toLowerCase().contains(searchValue.toLowerCase())) {
				tmp += 50;
			}

			if (tmp > score && tmp > 45) {
				score = tmp;
				highestScoreEnumConstant = value;
			}
		}

		return highestScoreEnumConstant;
	}

	/**
	 * Gets the my fields.
	 *
	 * @param type the type
	 * @return the my fields
	 */
	private Map<String, Class<?>> getMyFields(Class<?> type) {

		try {
			return new DataSetAnnotationService().generateMetaData(type).stream()
					.collect(Collectors.toMap(a -> a.getFieldName(), a -> a.getType()));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not get class details");

		}
	}

	/**
	 * Gets the my fields db.
	 *
	 * @param type the type
	 * @return the my fields db
	 */
	private Map<String, DataSetColumn> getMyFieldsDb(Class<?> type) {

		try {
			return new DataSetAnnotationService().generateMetaData(type).stream()
					.collect(Collectors.toMap(a -> a.getFieldName(), a -> a));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not get class details");

		}
	}

}
