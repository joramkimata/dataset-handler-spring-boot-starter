package io.github.joramkimata.datasethandler.specifications;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QueryObject {
	private Predicate predicate;
	private Sort sort;
}
