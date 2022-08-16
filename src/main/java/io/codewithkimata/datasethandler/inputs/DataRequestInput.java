package io.codewithkimata.datasethandler.inputs;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataRequestInput {
	private int page = 0;
	private int pageSize = 10;
	private List<FieldRequestInput> fields = new ArrayList<>();
	private List<MandatoryFilterInput> mustHaveFilters = new ArrayList<>();

}
