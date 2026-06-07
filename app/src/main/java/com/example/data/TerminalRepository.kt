package com.example.data

import com.example.data.model.TerminalLine
import com.example.data.model.TerminalLineType
import com.example.engine.ShellEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TerminalRepository(private val shellEngine: ShellEngine) {

    val isSessionActive: Boolean get() = shellEngine.isSessionActive

    fun startShell(onOutput: (String) -> Unit) {
        shellEngine.outputCallback = onOutput
        shellEngine.startShell()
    }

    suspend fun executeCommand(command: String, workingDir: String, onOutput: (TerminalLine) -> Unit) {
        withContext(Dispatchers.IO) {
            shellEngine.executeCommand(command, workingDir) { output ->
                onOutput(TerminalLine(output, TerminalLineType.STDOUT))
            }
        }
    }

    fun sendInput(text: String) {
        shellEngine.sendInput(text)
    }

    fun killProcess() {
        shellEngine.stop()
    }
}
