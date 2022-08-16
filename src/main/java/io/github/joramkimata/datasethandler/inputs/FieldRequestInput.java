package io.github.joramkimata.datasethandler.inputs;

import org.springframework.data.domain.Sort.Direction;

import io.github.joramkimata.datasethandler.enums.SearchOperation;
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
