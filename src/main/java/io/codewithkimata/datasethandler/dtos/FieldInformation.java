package io.codewithkimata.datasethandler.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldInformation {
    private String fieldName;
    private String databaseName;
    private Class<?> type;
}
