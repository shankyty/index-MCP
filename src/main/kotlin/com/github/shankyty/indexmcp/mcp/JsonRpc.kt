package com.github.shankyty.indexmcp.mcp

import com.google.gson.JsonElement

data class JsonRpcRequest(
    val jsonrpc: String,
    val method: String,
    val params: JsonElement?,
    val id: JsonElement?
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val result: Any? = null,
    val error: JsonRpcError? = null,
    val id: JsonElement?
)

data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

// MCP Specific Models

data class InitializeParams(
    val protocolVersion: String,
    val capabilities: Map<String, Any>?,
    val clientInfo: ClientInfo?
)

data class ClientInfo(
    val name: String,
    val version: String
)

data class InitializeResult(
    val protocolVersion: String,
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo
)

data class ServerCapabilities(
    val tools: Map<String, Any>? = null
)

data class ServerInfo(
    val name: String,
    val version: String
)

data class Tool(
    val name: String,
    val description: String?,
    val inputSchema: Map<String, Any>
)

data class ListToolsResult(
    val tools: List<Tool>
)

data class CallToolParams(
    val name: String,
    val arguments: Map<String, Any>?
)

data class CallToolResult(
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String?
)
