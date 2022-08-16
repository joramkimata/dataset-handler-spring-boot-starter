package io.codewithkimata.datasethandler.specifications;

import io.codewithkimata.datasethandler.enums.SearchOperation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchCriteria {
	private String key;
	private Object value;
	private SearchOperation operation;

	public SearchCriteria(String key, Object value, SearchOperation operation) {
		super();
		this.key = key;
		this.value = value;
		this.operation = operation;
	}

}
