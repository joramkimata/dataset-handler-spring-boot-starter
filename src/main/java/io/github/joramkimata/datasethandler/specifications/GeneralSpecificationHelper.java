package io.github.joramkimata.datasethandler.specifications;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.google.common.collect.ImmutableMap;

import io.github.joramkimata.datasethandler.enums.SearchOperation;

@Deprecated
public class GeneralSpecificationHelper<T> {

	public static QueryObject getQueryObject(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			Set<SortField> sortFields, Set<SearchCriteria> criterias,
			ImmutableMap<String, Class<? extends Object>> dbFields) {

		QueryObject object = new QueryObject();

		List<Order> orders = sortFields.stream().map(s -> {
			if (s.getDirection().equals(Direction.ASC)) {
				return Order.asc(s.getField());

			} else {
				return Order.desc(s.getField());
			}

		}).collect(Collectors.toList());

		object.setSort(Sort.by(orders));
		object.setPredicate(GeneralSpecificationHelper.getPredicate(root, builder, criterias, dbFields));

		return object;
	}

	public static QueryObject getQueryObject(Root<?> root, CriteriaBuilder builder, Set<SortField> sortFields,
			Set<SearchCriteria> criterias, ImmutableMap<String, Class<? extends Object>> dbFields) {

		QueryObject object = new QueryObject();

		object.setPredicate(GeneralSpecificationHelper.getPredicate(root, builder, criterias, dbFields));

		return object;
	}

	public static QueryObject getQueryObject(Set<SortField> sortFields) {

		QueryObject object = new QueryObject();

		List<Order> orders = sortFields.stream().map(s -> {
			if (s.getDirection().equals(Direction.ASC)) {
				return Order.asc(s.getField());

			} else {
				return Order.desc(s.getField());
			}

		}).collect(Collectors.toList());

		object.setSort(Sort.by(orders));

		return object;
	}

	public static QueryObject getQueryObject(Root<?> root, CriteriaBuilder builder, Set<SearchCriteria> criterias,
			ImmutableMap<String, Class<? extends Object>> dbFields) {

		QueryObject object = new QueryObject();

		object.setPredicate(GeneralSpecificationHelper.getPredicate(root, builder, criterias, dbFields));

		return object;
	}

	public static Sort getSorting(Set<SortField> sortFields) {
		List<Order> orders = sortFields.stream().map(s -> {
			if (s.getDirection().equals(Direction.ASC)) {
				return Order.asc(s.getField());

			} else {
				return Order.desc(s.getField());
			}

		}).collect(Collectors.toList());

		return Sort.by(orders);

	}

	public static Predicate getPredicate(Root<?> root, CriteriaBuilder builder, Set<SearchCriteria> criterias,
			ImmutableMap<String, Class<? extends Object>> dbFields) {

		List<Predicate> predicates = criterias.stream().map(criteria -> {

			Class<? extends Object> fieldClass = dbFields.get(criteria.getKey());

			Object searchValue = criteria.getValue();

			if (fieldClass != null && fieldClass.isEnum()) {

				criteria.setOperation(SearchOperation.EQ);

				if (criteria.getValue() != null && !criteria.getValue().toString().isEmpty()) {

					criteria.setValue(GeneralSpecificationHelper.enumStringSearch(criteria.getKey(),
							criteria.getValue().toString(), dbFields));
					searchValue = criteria.getValue();

				} else {
					searchValue = null;
				}

//					
				if (searchValue != null && searchValue != "") {
					try {
						searchValue = fieldClass.getDeclaredMethod("valueOf", String.class).invoke(fieldClass,
								criteria.getValue());

					} catch (Exception e) {
						e.printStackTrace();
						searchValue = null;
						return null;
					}
				} else {

					return null;
				}

			}
			if (criteria.getOperation().equals(SearchOperation.GT)) {
				return builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
			} else if (criteria.getOperation().equals(SearchOperation.LT)) {
				return builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
			} else if (criteria.getOperation().equals(SearchOperation.GTE)) {
				return builder.greaterThanOrEqualTo(root.get(criteria.getKey()), criteria.getValue().toString());
			} else if (criteria.getOperation().equals(SearchOperation.LTE)) {
				return builder.lessThanOrEqualTo(root.get(criteria.getKey()), criteria.getValue().toString());
			} else if (criteria.getOperation().equals(SearchOperation.NE)) {
				return builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
			} else if (criteria.getOperation().equals(SearchOperation.EQ)) {
				return builder.equal(root.get(criteria.getKey()), searchValue);
			} else if (criteria.getOperation().equals(SearchOperation.ILK)) {
				return builder.like(builder.lower(root.get(criteria.getKey())),
						"%" + criteria.getValue().toString().toLowerCase() + "%");
			} else if (criteria.getOperation().equals(SearchOperation.LK)) {
				return builder.like(builder.lower(root.get(criteria.getKey())),
						criteria.getValue().toString().toLowerCase());
			} else if (criteria.getOperation().equals(SearchOperation.ME)) {
				return builder.like(builder.lower(root.get(criteria.getKey())),
						criteria.getValue().toString().toLowerCase() + "%");
			} else if (criteria.getOperation().equals(SearchOperation.MS)) {
				return builder.like(builder.lower(root.get(criteria.getKey())),
						"%" + criteria.getValue().toString().toLowerCase());
			} else if (criteria.getOperation().equals(SearchOperation.IN)) {
				return builder.in(root.get(criteria.getKey())).value(criteria.getValue());
			} else if (criteria.getOperation().equals(SearchOperation.NIN)) {
				return builder.not(root.get(criteria.getKey())).in(criteria.getValue());
			} else {
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());

		return builder.or(predicates.toArray(new Predicate[0]));
	}

//	
//	public static List<DataTableFieldRequest> getFields() {
//		
//		
//		
//	}
//	

	private static String enumStringSearch(String field, String searchValue,
			ImmutableMap<String, Class<? extends Object>> dbFields) {

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

}