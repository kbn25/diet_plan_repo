package com.ninja.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.postgresql.util.PSQLException;

import com.ninja.dto.DatabaseError;
import com.ninja.dto.ErrorField;
import com.ninja.dto.RequestError;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
	/**
	 * Handles Request Validations / Exceptions
	 * @param exception
	 * @return Request Error List
	 */
	@ExceptionHandler(value = {MethodArgumentNotValidException.class})	
	public ResponseEntity<RequestError> handleValidationException(MethodArgumentNotValidException invalidException) 
	{
		List<ErrorField> errors = invalidException.getBindingResult()
									.getFieldErrors().stream()
									.map(x -> new ErrorField(x.getField(),x.getDefaultMessage()))
									.collect(Collectors.toList());
				
		
		RequestError errorResponse = new RequestError(LocalDateTime.now(),HttpStatus.BAD_REQUEST.toString(),"Request Validation Failed", errors);
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
		
	}
	
	
	/**
	 * Handles Database side exceptions
	 * @param exception
	 * @return Database error messages
	 * ConstraintViolationException:
	 */
	@ExceptionHandler(PSQLException.class)
	public ResponseEntity<DatabaseError> handleValidationException(PSQLException exception)
	{
		List<String> errors = new ArrayList<String>();
		exception.iterator().forEachRemaining(e -> errors.add(e.getMessage()));
		
		DatabaseError dbError = new DatabaseError(LocalDateTime.now(), 
										HttpStatus.INTERNAL_SERVER_ERROR.toString(),"Database Validation Failed ",errors);
		
		return new ResponseEntity<>(dbError, HttpStatus.INTERNAL_SERVER_ERROR);

	}
	
}
