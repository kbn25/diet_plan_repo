package com.ninja.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestError 
{
	private LocalDateTime timeStamp;
	private String errorCode;
	private String errorMessage;
	private List<ErrorField> errors;
	
}
