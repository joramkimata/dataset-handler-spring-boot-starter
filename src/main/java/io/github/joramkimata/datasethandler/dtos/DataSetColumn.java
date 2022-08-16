package io.github.joramkimata.datasethandler.dtos;

import io.leangen.graphql.annotations.GraphQLIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataSetColumn {

	private String fieldName;
	private Boolean isSearchable = true;
	private Boolean isSortable = true;
	private Boolean isEnum = false;
	public boolean isDisplayedOnList = true;
	private String databaseName;
	@GraphQLIgnore
	public Class<?> type;
	
	@GraphQLIgnore
	public Class<?> getType() {
		return type;
	}

}
