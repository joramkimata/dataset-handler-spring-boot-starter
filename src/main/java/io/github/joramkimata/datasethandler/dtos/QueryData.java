package io.github.joramkimata.datasethandler.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryData<T> {
	private DataPage<T> pagedData;
	private List<DataSetColumn> metaData = new ArrayList<>();
}
