package io.codewithkimata.datasethandler.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataPage<T> {

	private int totalPages;
	private long totalRecords;
	private int pageSize;
	private long numberOfRecords;
	private int currentPage;
	private int recordsFilteredCount;
	private List<T> data;
	private boolean isLast;
	private boolean isFirst;
	private boolean hasNext;
	private boolean hasPrevious;
	
	
	private List<DataSetColumn> metaData=new ArrayList<>();
	

	public DataPage(DataPage<T> page) {
		
		this.currentPage = page.getCurrentPage();
		this.totalPages = page.getTotalPages();
		this.totalRecords = page.getTotalRecords();
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
		this.hasNext = page.isHasNext();
		this.pageSize=page.getPageSize();
		this.hasPrevious = page.isHasPrevious();
		this.numberOfRecords = page.getNumberOfRecords();
		this.recordsFilteredCount=page.getRecordsFilteredCount();
//		this.data = page.getContent();

	}

	public DataPage(DataPage<T> page, List<DataSetColumn> metaData) {

		this.currentPage = page.getCurrentPage();
		this.totalPages = page.getTotalPages();
		this.totalRecords = page.getTotalRecords();
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
		this.hasNext = page.isHasNext();
		this.hasPrevious = page.isHasPrevious();
		this.pageSize=page.getPageSize();
		this.numberOfRecords = page.getNumberOfRecords();
		this.recordsFilteredCount=page.getRecordsFilteredCount();
		this.metaData=metaData;
//		this.data = page.getContent();

	}



}
