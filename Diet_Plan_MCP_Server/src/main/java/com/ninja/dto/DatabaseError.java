package com.ninja.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseError 
{
	private LocalDateTime timeStamp;
	private String errorCode;
	private String errorMessage;
	private List<String> errors;

}
