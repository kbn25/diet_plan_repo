Steps:

Diet_Plan_MCP_Server setup----
	1. Change your DB config (DB name, username password) in application.properties
	2. Perform Maven Clean Build
	3. Start the server
	4. Test in postman if server is able to connect from it using --- 
	New MCP Collection > With Latest Postman edition you will get MCP option same as REST API
	With New > MCP
	Then select protocol as HTTP as this server is with SSE mode
	http://localhost:9090/sse
	5. Once you connect, you can see Tools, Prompts and Resources tabs.
	6. Check under Tools tab if your tools are visible
	

Diet_Plan_MCP_Client----
Change GEMINI_API_KEY and do Clean Build
