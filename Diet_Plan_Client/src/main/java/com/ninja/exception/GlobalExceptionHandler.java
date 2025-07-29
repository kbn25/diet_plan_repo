package com.ninja.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	/**
	 * Handles Request Validations / Exceptions
	 * @param exception
	 * @return Request Error List
	 */
	@ExceptionHandler(value = {MethodArgumentNotValidException.class})	
	public void handleValidationException(MethodArgumentNotValidException invalidException) 
	{
		logger.error(HttpStatus.BAD_REQUEST + "\n" + invalidException.getMessage());
	}
	
	
	/**
	 * Handles Database side exceptions
	 * @param exception
	 * @return Database error messages
	 * ConstraintViolationException:
	 */
	@ExceptionHandler(Exception.class)
	public void handleValidationException(Exception exception)
	{
		logger.error(exception.getMessage());

	}
	
}
