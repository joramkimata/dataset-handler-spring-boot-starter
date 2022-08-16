package io.github.joramkimata.datasethandler.inputs;

import java.util.ArrayList;
import java.util.List;

import io.github.joramkimata.datasethandler.enums.SearchOperation;
import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MandatoryFilterInput {
	private String fieldName;
	private String value1;
	private SearchOperation operation = SearchOperation.ILK;
	private String value2;
	private List<String> inValues = new ArrayList<>();
	private Boolean isNull = false;
	@GraphQLIgnore
	private Class<?> tmpClass;
}
