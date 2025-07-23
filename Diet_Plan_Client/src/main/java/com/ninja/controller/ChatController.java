package com.ninja.controller;


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
	

}
