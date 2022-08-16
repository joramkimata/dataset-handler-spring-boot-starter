package io.github.joramkimata.datasethandler.services;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import io.github.joramkimata.datasethandler.dtos.DataPage;
import io.github.joramkimata.datasethandler.dtos.QueryData;
import io.github.joramkimata.datasethandler.enums.SearchOperation;
import io.github.joramkimata.datasethandler.helpers.DateUtils;
import io.github.joramkimata.datasethandler.helpers.OperationMap;
import io.github.joramkimata.datasethandler.inputs.DataRequestInput;
import io.github.joramkimata.datasethandler.inputs.FilterInput;
import io.github.joramkimata.datasethandler.inputs.MandatoryFilterInput;
import io.github.joramkimata.datasethandler.specifications.SearchCriteria;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Setter
public class DynamicQueryService<T> {

    private String baseCustomQuery = "";

    private String baseCustomCountQuery = "";

    private String orderPrefix = "";
    @Value("${datahandler.default-page-size:200}")
    private int defaultPageSize;
    @Autowired
    private EntityManager em;

    public void futaCustomBase() {
        this.baseCustomQuery = "";

        this.baseCustomCountQuery = "";

        this.orderPrefix = "";
    }

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
                .collect(Collectors.toMap(a -> (this.orderPrefix + a.getFieldName()), a -> a.getOrderDirection()));

        // internal must have parameters
        Map<String, Object> mustHave = mustHaveBasicInternal.entrySet().stream()
                .collect(Collectors.toMap(a -> a.getKey(), a -> a.getValue()));

        Map<String, Object> mustHaveFilterMap = new HashMap<>();
        Map<String, Class<?>> fields = this.getMyFields(type);

        if (!input.getMustHaveFilters().isEmpty()) {
//			Map<String, Class<?>> fields = this.getMyFields(type);

            input.getMustHaveFilters().forEach(mh -> {
                Class<?> fieldClass = fields.get(mh.getFieldName());
                mh.setTmpClass(fieldClass);

//                log.info("field: {}, fieldClass: {} value1: {} value2: {} type: {}", mh.getFieldName(), fieldClass, mh.getValue1(), mh.getValue2(),
//                        fields.get(mh.getFieldName()));

                try {
                    Object value1 = mh.getValue1();
                    Object value2 = mh.getValue2();
                    List<Object> values = new ArrayList<>();

                    if (!fieldClass.equals(String.class)) {

                        if (mh.getIsNull()) {

                            value1 = null;

                        } else if (Temporal.class.isAssignableFrom(fieldClass)
                                && ChronoLocalDateTime.class.isAssignableFrom(fieldClass)) {
                            value1 = DateUtils.parseDateTime(value1);
                            value2 = (value2 != null) ? DateUtils.parseDateTime(value2) : value2;

                        } else if (Temporal.class.isAssignableFrom(fieldClass)
                                && ChronoLocalDate.class.isAssignableFrom(fieldClass)) {
                            value1 = DateUtils.parseDate(value1);
                            value2 = (value2 != null) ? DateUtils.parseDate(value2) : value2;

                        } else if (fieldClass.isArray()) {
                            Class<?> cls = (fieldClass.isArray())
                                    ? Class.forName(fieldClass.getTypeName().replace("[]", ""))
                                    : fieldClass;
                            Method method = cls.getDeclaredMethod("valueOf", String.class);
                            value1 = method.invoke(cls, mh.getValue1());
                        } else {

                            if (fieldClass.equals(BigDecimal.class)) {
                                // check if value is list
                                if (!mh.getInValues().isEmpty()) {
                                    mh.getInValues().forEach(i -> {

                                        try {
                                            values.add(new BigDecimal(i));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } else {

                                    value1 = new BigDecimal(mh.getValue1());
                                    value2 = (value2 == null) ? null : new BigDecimal(mh.getValue2());
                                }

                            } else {


                                Method method = fieldClass.getDeclaredMethod("valueOf", String.class);

                                // check if value is list
                                if (!mh.getInValues().isEmpty()) {
                                    mh.getInValues().forEach(i -> {

                                        try {
                                            values.add(method.invoke(fieldClass, i));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } else {

                                    value1 = method.invoke(fieldClass, mh.getValue1());
                                    value2 = (value2 == null) ? null : method.invoke(fieldClass, mh.getValue2());
                                }

                            }
                        }

                    }

                    if (mh.getOperation().equals(SearchOperation.BTN)) {
                        mustHaveFilterMap.put("mst1" + mh.getFieldName(), value1);
                        mustHaveFilterMap.put("mst2" + mh.getFieldName(), value2);
                    } else if (Arrays.asList(SearchOperation.IN, SearchOperation.NIN).contains(mh.getOperation())) {
                        mustHaveFilterMap.put("msf" + mh.getFieldName(), values);
                    } else {
                        mustHaveFilterMap.put("msf" + mh.getFieldName(), value1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (fieldClass.isEnum()) {
//                        java.lang.String.valueOf("");
                    }
                }

            });

        }

        // end of order by

        String mustHaveSql = this.getMustSql(mustHave, input.getMustHaveFilters());

        String countQuery = this.getCountSql(mustHaveSql, type);

        String searchSql = this.getSearchSql(searchFields);

        String orderString = this.getOrderSql(orders);

//		this.filterMapping(input.getOptionalFilters(), fields);

//		String optionalSql = this.getOptionalSql(this.filterMapping(input.getOptionalFilters(), fields), fields);

        TypedQuery<?> query = em.createQuery(this.getFullSql(mustHaveSql, orderString, searchSql, type, false), type);

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
        mustHave.entrySet().forEach(s -> parameters.put("mst" + s.getKey(), s.getValue()));

        mustHaveFilterMap.entrySet().forEach(s -> parameters.put(s.getKey(), s.getValue()));


        parameters.entrySet().forEach(p -> query.setParameter(p.getKey().replace(".", ""), p.getValue()));
        Integer filtersCount = 0;
        if (!mustHaveFilterMap.isEmpty() || !searchFields.isEmpty()) {

            String resultCountWithoutPaginationSql = this.getFullSql(mustHaveSql, orderString, searchSql, type, true);

            TypedQuery<?> queryResultSetCount = em.createQuery(resultCountWithoutPaginationSql, Long.class);

            parameters.entrySet()
                    .forEach(p -> queryResultSetCount.setParameter((p.getKey().replace(".", "")), p.getValue()));

            filtersCount = Integer.valueOf(queryResultSetCount.getSingleResult().toString());
        }

        // set limits
        query.setMaxResults(pageSize);
        query.setFirstResult(pageSize * (page.getPageNumber() - 1));

        List<T> list = (List<T>) query.getResultList();
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
        TypedQuery<Long> query = em.createQuery(baseQuery, Long.class);
        mustHave.entrySet()
                .forEach(s -> query.setParameter(StringUtils.replace(("mst" + s.getKey()), ".", ""), s.getValue()));

        mustHaveFilterMap.entrySet()
                .forEach(s -> query.setParameter(StringUtils.replace((s.getKey()), ".", ""), s.getValue()));

        return query.getSingleResult();

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
        List<String> predicates = new ArrayList<>();
        if (!mustHaveInternal.isEmpty()) {

            mustHaveInternal.entrySet().forEach(s -> {

                if (s.getValue() instanceof Collection<?>) {

                    predicates.add(
                            String.format("%s IN (:%s)", s.getKey(), "mst" + StringUtils.replace(s.getKey(), ".", "")));
                } else {
                    predicates
                            .add(String.format("%s=:%s", s.getKey(), "mst" + StringUtils.replace(s.getKey(), ".", "")));

                }
            });
        }

        mustHaveFilterFields.forEach(f -> {

            if (f.getOperation().equals(SearchOperation.BTN)) {
                predicates.add(String.format("%s %s :%s AND :%s", this.orderPrefix + f.getFieldName(),
                        OperationMap.operation(f.getOperation()),
                        "mst1" + StringUtils.replace(f.getFieldName(), ".", ""),
                        "mst2" + f.getFieldName().replace(".", "")));
            } else if (f.getTmpClass().isArray()) {
                predicates.add(String.format(":%s=FUNCTION('ANY',%s)",
                        ("msf" + StringUtils.replace(f.getFieldName(), ".", "")), this.orderPrefix + f.getFieldName()));
            } else if (!f.getClass().isArray()
                    && Arrays.asList(SearchOperation.IN, SearchOperation.NIN).contains(f.getOperation())
                    && !f.getInValues().isEmpty()) {
                predicates.add(String.format("%s %s(:%s)", this.orderPrefix + f.getFieldName(),
                        OperationMap.operation(f.getOperation()),
                        "msf" + StringUtils.replace(f.getFieldName(), ".", "")));
            } else {
                predicates.add(String.format("%s %s:%s", this.orderPrefix + f.getFieldName(),
                        OperationMap.operation(f.getOperation()),
                        "msf" + StringUtils.replace(f.getFieldName(), ".", "")));
            }

        });
        mustHaveSql = "  (" + String.join(" AND ", predicates) + ") ";
        return mustHaveSql;
    }

    private String getOptionalSql(Map<String, Object> optionalFilter, List<FilterInput> fields) {

        String optionalSQL = "";
        List<String> predicates = new ArrayList<>();
        if (!optionalFilter.isEmpty()) {

            optionalFilter.entrySet().forEach(s -> {
                predicates.add(String.format("%s=:%s", s.getKey(), "mst" + s.getKey()));
            });
        }

        fields.forEach(f -> {

            if (f.getOperation().equals(SearchOperation.BTN)) {
                predicates.add(
                        String.format("%s %s :%s AND :%s", f.getFieldName(), OperationMap.operation(f.getOperation()),
                                "mst1" + StringUtils.replace(f.getFieldName(), ".", ""),
                                "mst2" + f.getFieldName().replace(".", "")));
            } else if (Arrays.asList(SearchOperation.IN, SearchOperation.NIN).contains(f.getOperation())
                    && !f.getInValues().isEmpty()) {
                predicates.add(String.format("%s %s(:%s)", f.getFieldName(), OperationMap.operation(f.getOperation()),
                        "msf" + StringUtils.replace(f.getFieldName(), ".", "")));
            } else {
                predicates.add(String.format("%s %s:%s", f.getFieldName(), OperationMap.operation(f.getOperation()),
                        "msf" + StringUtils.replace(f.getFieldName(), ".", "")));
            }

        });
        optionalSQL = "  (" + String.join(" OR ", predicates) + ") ";

        return optionalSQL;
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
                .map(m -> String.format("%s %s", m.getKey(), m.getValue().toString())).collect(Collectors.toList());

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
    private String getFullSql(String mustHaveSql, String orderString, String searchSql, Class<?> type,
                              boolean fullSQLCount) {
        String baseQuery;
        if (fullSQLCount) {
            baseQuery = String.format("SELECT count(item) FROM %s item ", type.getSimpleName());
            baseQuery = (this.baseCustomCountQuery.isEmpty()) ? baseQuery : this.baseCustomCountQuery;
        } else {
            baseQuery = String.format("SELECT item FROM %s item ", type.getSimpleName());
            baseQuery = (this.baseCustomQuery.isEmpty()) ? baseQuery : this.baseCustomQuery;

        }

        baseQuery = baseQuery
                + (((!mustHaveSql.isEmpty() || !searchSql.isEmpty()) && this.baseCustomCountQuery.isEmpty()) ? " WHERE "
                : "");

        baseQuery = baseQuery + ((mustHaveSql.isEmpty()) ? "" : " AND ") + mustHaveSql
                + ((!mustHaveSql.isEmpty() && !searchSql.isEmpty()) ? " AND " : "")
                + ((!searchSql.isEmpty()) ? searchSql : "");

        baseQuery = baseQuery.replace("WHERE  AND", " WHERE ");
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
    private String getCountSql(String mustHaveSql, Class<?> type) {
        String countQuery = String.format("SELECT count(item) FROM %s item ", type.getSimpleName());
        
        countQuery = (this.baseCustomCountQuery.isEmpty()) ? countQuery : this.baseCustomCountQuery;

        if (this.baseCustomCountQuery.isEmpty()) {
            countQuery = countQuery + ((!mustHaveSql.isEmpty()) ? " WHERE " + mustHaveSql : "");
        } else {
            countQuery = countQuery + ((!mustHaveSql.isEmpty()) ? " AND " + mustHaveSql : "");
        }

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
                str += String.format(" LOWER(CAST(%s as text)) ", this.orderPrefix + s.getKey());
                str += OperationMap.operation(s.getOperation());
                if (!(Arrays.asList(SearchOperation.ILK, SearchOperation.LK)).contains(s.getOperation())) {
                    str += String.format(":%s ", s.getKey().replace(".", ""));
                } else {
                    str += String.format(" LOWER(CAST(:%s as text))", s.getKey().replace(".", ""));
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
                    .collect(Collectors.toMap(a -> a.getFieldName(), a -> a.getType(), (a1, a2) -> a1));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not get class details");

        }
    }

    private Map<String, Object> filterMapping(List<FilterInput> optionalFilters, Map<String, Class<?>> fields) {
        Map<String, Object> filterMap = new HashMap<>();

        if (!optionalFilters.isEmpty()) {

            optionalFilters.forEach(mh -> {
                Class<?> fieldClass = fields.get(mh.getFieldName());

//				log.info("field: {} value1: {} value2: {} type: {}", mh.getFieldName(), mh.getValue1(), mh.getValue2(),
//						fields.get(mh.getFieldName()));

                try {
                    Object value1 = mh.getValue1();
                    Object value2 = mh.getValue2();
                    List<Object> values = new ArrayList<>();

                    if (!fieldClass.equals(String.class)) {

                        if (mh.getIsNull()) {

                            value1 = null;

                        } else if (Temporal.class.isAssignableFrom(fieldClass)
                                && ChronoLocalDateTime.class.isAssignableFrom(fieldClass)) {
                            value1 = DateUtils.parseDateTime(value1);
                            value2 = (value2 != null) ? DateUtils.parseDateTime(value2) : value2;

                        } else if (Temporal.class.isAssignableFrom(fieldClass)
                                && ChronoLocalDate.class.isAssignableFrom(fieldClass)) {
                            value1 = DateUtils.parseDateTime(value1);
                            value2 = (value2 != null) ? DateUtils.parseDate(value2) : value2;

                        } else {
                            Method method = fieldClass.getDeclaredMethod("valueOf", String.class);
                            // check if value is list
                            if (!mh.getInValues().isEmpty()) {
                                mh.getInValues().forEach(i -> {
                                    try {
                                        values.add(method.invoke(fieldClass, i));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {

                                value1 = method.invoke(fieldClass, mh.getValue1());
                                value2 = (value2 == null) ? null : method.invoke(fieldClass, mh.getValue2());
                            }
                        }

                    }

                    if (mh.getOperation().equals(SearchOperation.BTN)) {
                        filterMap.put("mst1" + mh.getFieldName(), value1);
                        filterMap.put("mst2" + mh.getFieldName(), value2);
                    } else if (Arrays.asList(SearchOperation.IN, SearchOperation.NIN).contains(mh.getOperation())) {
                        filterMap.put("msf" + mh.getFieldName(), values);
                    } else {
                        filterMap.put("msf" + mh.getFieldName(), value1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (fieldClass.isEnum()) {
                    }
                }

            });

        }

        return filterMap;
    }

    public String getBaseCustomQuery() {
        return baseCustomQuery;
    }

    public String getBaseCustomCountQuery() {
        return baseCustomCountQuery;
    }

    public String getOrderPrefix() {
        return orderPrefix;
    }

}
