package com.ninja;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ninja.service.LfvAndLchfBasedDietService;
//import com.ninja.service.LfvAndLchfBasedDietService;
import com.ninja.service.MealPlanningService;


@SpringBootApplication
public class DietPlanMcpServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(DietPlanMcpServerApplication.class);
	
	public static void main(String[] args) {
		logger.debug("Main Client Application Starting...");
		SpringApplication.run(DietPlanMcpServerApplication.class, args);
	}

	/**
	 * Configure the MCP tool callback provider. This bean automatically discovers
	 * and registers all @Tool annotated methods in the application context as MCP
	 * tools.
	 * 
	 * @return MethodToolCallbackProvider for automatic tool registration
	 */
	@Bean
	public ToolCallbackProvider mealPlanTools(MealPlanningService mealPlanningService, LfvAndLchfBasedDietService lchfBasedDietService) {
		logger.debug("Registering the tools...");
		return MethodToolCallbackProvider.builder().toolObjects(mealPlanningService, lchfBasedDietService).build();
	}
}
