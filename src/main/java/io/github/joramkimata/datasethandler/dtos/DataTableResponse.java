package io.github.joramkimata.datasethandler.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataTableResponse<T> {

	private int draw = 0;
	private Long recordsTotal;
	private Long recordsFiltered;
	private T data;


}
