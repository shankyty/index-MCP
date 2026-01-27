package com.github.shankyty.unity.services

import com.github.shankyty.unity.mcp.McpServer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class McpGlobalService : Disposable {

    private val mcpServer = McpServer()

    init {
        mcpServer.start()
    }

    override fun dispose() {
        mcpServer.stop()
    }

    companion object {
        val instance: McpGlobalService
            get() = service()
    }
}
