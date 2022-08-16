package io.codewithkimata.datasethandler.specifications;

import org.springframework.data.domain.Sort.Direction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SortField {
	private String field;
	private Direction direction = Direction.ASC;
	public SortField(String field, Direction direction) {
		super();
		this.field = field;
		this.direction = direction;
	}
	
	
}
