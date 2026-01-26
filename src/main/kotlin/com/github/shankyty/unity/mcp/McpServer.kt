package com.github.shankyty.unity.mcp

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sun.net.httpserver.HttpServer
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.intellij.openapi.project.ProjectManager

class McpServer(private val port: Int = 20245) {
    private var server: HttpServer? = null
    private val gson = Gson()

    fun start() {
        try {
            server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
            server?.createContext("/mcp") { exchange ->
                if ("POST" == exchange.requestMethod) {
                    val inputStream: InputStream = exchange.requestBody
                    val requestBody = inputStream.bufferedReader().use { it.readText() }

                    val response = try {
                        handleRequest(requestBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val error = JsonRpcError(-32603, "Internal error", e.message)
                        gson.toJson(JsonRpcResponse(id = null, error = error))
                    }

                    val responseBytes = response.toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                    val os: OutputStream = exchange.responseBody
                    os.write(responseBytes)
                    os.close()
                } else {
                    exchange.sendResponseHeaders(405, -1)
                }
            }
            server?.executor = Executors.newSingleThreadExecutor()
            server?.start()
            println("MCP Server started on port $port")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleRequest(json: String): String {
        val request = try {
            gson.fromJson(json, JsonRpcRequest::class.java)
        } catch (e: Exception) {
            return gson.toJson(JsonRpcResponse(id = null, error = JsonRpcError(-32700, "Parse error")))
        }

        if (request.id == null && request.method != "notifications/initialized") {
             // Notification handling could be here
        }

        val result: Any? = try {
            when (request.method) {
                "initialize" -> handleInitialize(request.params)
                "notifications/initialized" -> null
                "tools/list" -> handleListTools()
                "tools/call" -> handleCallTool(request.params)
                else -> null
            }
        } catch (e: Exception) {
            return gson.toJson(JsonRpcResponse(id = request.id, error = JsonRpcError(-32000, e.message ?: "Unknown error")))
        }

        if (result == null && request.method != "notifications/initialized") {
             return gson.toJson(JsonRpcResponse(id = request.id, error = JsonRpcError(-32601, "Method not found")))
        }

        return gson.toJson(JsonRpcResponse(id = request.id, result = result))
    }

    private fun handleInitialize(@Suppress("UNUSED_PARAMETER") params: JsonElement?): InitializeResult {
        return InitializeResult(
            protocolVersion = "2024-11-05",
            capabilities = ServerCapabilities(
                tools = mapOf("listChanged" to false)
            ),
            serverInfo = ServerInfo(
                name = "intellij-unity-mcp",
                version = "0.0.1"
            )
        )
    }

    private fun handleListTools(): ListToolsResult {
        return ListToolsResult(
            tools = listOf(
                Tool(
                    name = "java_search_classes",
                    description = "Search for Java classes by name",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf("type" to "string")
                        ),
                        "required" to listOf("query")
                    )
                ),
                Tool(
                    name = "java_get_class_source",
                    description = "Get the source code of a Java class",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "className" to mapOf("type" to "string")
                        ),
                        "required" to listOf("className")
                    )
                ),
                Tool(
                    name = "java_list_methods",
                    description = "List methods of a Java class",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "className" to mapOf("type" to "string")
                        ),
                        "required" to listOf("className")
                    )
                )
            )
        )
    }

    private fun handleCallTool(params: JsonElement?): CallToolResult {
        if (params == null) throw IllegalArgumentException("Params required")
        val callParams = gson.fromJson(params, CallToolParams::class.java)

        val project = ProjectManager.getInstance().openProjects.firstOrNull()
            ?: throw IllegalStateException("No open project found")

        return when (callParams.name) {
            "java_search_classes" -> {
                val query = callParams.arguments?.get("query") as? String ?: ""
                val classes = JavaTools.searchClasses(query, project)
                CallToolResult(listOf(Content("text", classes.joinToString("\n"))))
            }
            "java_get_class_source" -> {
                val className = callParams.arguments?.get("className") as? String ?: throw IllegalArgumentException("className required")
                val source = JavaTools.getClassSource(className, project) ?: "Class not found"
                CallToolResult(listOf(Content("text", source)))
            }
             "java_list_methods" -> {
                val className = callParams.arguments?.get("className") as? String ?: throw IllegalArgumentException("className required")
                val methods = JavaTools.listMethods(className, project)
                CallToolResult(listOf(Content("text", methods.joinToString("\n"))))
            }
            else -> throw IllegalArgumentException("Unknown tool: ${callParams.name}")
        }
    }

    fun stop() {
        server?.stop(0)
        (server?.executor as? java.util.concurrent.ExecutorService)?.shutdown()
        println("MCP Server stopped")
    }
}
