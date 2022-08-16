package io.codewithkimata.datasethandler.inputs;

import org.springframework.data.domain.Sort.Direction;

import io.codewithkimata.datasethandler.enums.SearchOperation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FieldRequestInput {
	private String fieldName;
	private String searchValue;
	private Boolean isSearchable = true;
	private Boolean isSortable = true;
	private Direction orderDirection = Direction.ASC;
	private SearchOperation operation = SearchOperation.ILK;
	private String searchValue2;
}
