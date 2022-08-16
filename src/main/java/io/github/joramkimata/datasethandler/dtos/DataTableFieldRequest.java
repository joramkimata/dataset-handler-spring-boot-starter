package io.github.joramkimata.datasethandler.dtos;

import org.springframework.data.domain.Sort.Direction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataTableFieldRequest {
	private String fieldName;
	private Boolean isSearchable;
	private Boolean isSortable;
	private String searchValue;
	private Direction orderDirection;
}
