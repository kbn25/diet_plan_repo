package com.ninja.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ninja.service.ChatServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/mcp")
public class ChatController {
	
	@Autowired
	private ChatServiceImpl chatServiceImpl;

	
	@GetMapping("/chat")
	public  ResponseEntity<?> chat(@RequestParam String query) throws JsonMappingException, JsonProcessingException
	{
		return chatServiceImpl.getChatResponse(query);
	}
	
	@GetMapping("/generateMealPlan")
	public  ResponseEntity<?> generateMealPlan(			
			@RequestParam(defaultValue = "40") int age,
			@RequestParam(defaultValue = "20.0") float bmi,
			@RequestParam(defaultValue = "LCHF") String dietType,
			@RequestParam(defaultValue = "Type 2") String diabeticType,
			@RequestParam(defaultValue = "Indian") String cusineType,
			@RequestParam (defaultValue="Dairy") String allergens,
			@RequestParam String otherFoodRestrictions,
			@RequestParam String otherMedicalConditions) throws JsonMappingException, JsonProcessingException
	{
	
		return chatServiceImpl.getSevenDayMealPlan(age,bmi,dietType,diabeticType,cusineType,allergens,otherFoodRestrictions,otherMedicalConditions);
	}

}
