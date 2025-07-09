package com.ninja.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorField 
{
	private String field;
	private String message;
}
