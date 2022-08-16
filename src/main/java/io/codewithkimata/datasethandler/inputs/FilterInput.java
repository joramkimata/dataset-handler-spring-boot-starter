package io.codewithkimata.datasethandler.inputs;

import java.util.ArrayList;
import java.util.List;

import io.codewithkimata.datasethandler.enums.SearchOperation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilterInput {
	private String fieldName;
	private String value1;
	private SearchOperation operation = SearchOperation.ILK;
	private String value2;
	private List<String> inValues = new ArrayList<>();
	private Boolean isNull = false;
}
