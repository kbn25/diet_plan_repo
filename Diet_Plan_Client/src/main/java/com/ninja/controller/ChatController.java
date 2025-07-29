package com.ninja.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ninja.service.ChatServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/mcp")
public class ChatController {
	
	private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

	@Autowired
	private ChatServiceImpl chatServiceImpl;

	
	@GetMapping("/chat")
	public  ResponseEntity<?> chat(@RequestParam String query) throws Exception
	{
		logger.info("Inside Controller.....");
		return chatServiceImpl.getChatResponse(query);
	}
	

}
