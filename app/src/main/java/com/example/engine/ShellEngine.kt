package com.example.engine

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import java.io.File

private const val TAG = "ShellEngine"

class ShellEngine(private val context: Context) {

    private var process: Process? = null
    var outputCallback: ((String) -> Unit)? = null
    var isRunning = false
        private set

    val isSessionActive: Boolean get() = isRunning

    fun startShell() {
        if (isRunning) {
            appendOutput("[ShellEngine] Session already active.\n")
            return
        }

        val shellPath = "/system/bin/sh"
        val args = listOf(shellPath)

        appendOutput("[ShellEngine] ── Native Shell ──────────────────────────────────\n")
        appendOutput("[ShellEngine] ${args.joinToString(" ")}\n")
        appendOutput("[ShellEngine] ────────────────────────────────────────────────\n")

        try {
            val binPath = setupToolchain()
            process = ProcessBuilder(args).apply {
                directory(context.filesDir)
                redirectErrorStream(true) // Merge stdout and stderr for shell

                environment().apply {
                    put("HOME", context.filesDir.absolutePath)
                    put("TMPDIR", context.cacheDir.absolutePath)
                    put("TERM", "xterm-256color")
                    put("LD_LIBRARY_PATH", context.applicationInfo.nativeLibraryDir)
                    put("PATH", "$binPath:/system/bin:/system/xbin:${context.applicationInfo.nativeLibraryDir}")
                }
            }.start().also { proc ->
                isRunning = true
                pipeStream(proc.inputStream, "")
                watchExit(proc)
            }
        } catch (e: Exception) {
            appendOutput("[ShellEngine] FATAL — ProcessBuilder threw: ${e.message}\n")
            Log.e(TAG, "ProcessBuilder failure", e)
            isRunning = false
        }
    }

    fun sendInput(text: String) {
        if (!isRunning) { appendOutput("[ShellEngine] No active session.\n"); return }
        try {
            process?.outputStream?.let { os ->
                os.write((text + "\n").toByteArray())
                os.flush()
            }
        } catch (e: Exception) {
            appendOutput("[ShellEngine] Input error: ${e.message}\n")
        }
    }

    fun stop() {
        process?.destroy()
        process = null
        isRunning = false
        appendOutput("[ShellEngine] Session stopped.\n")
    }

    fun executeCommand(command: String, workingDir: String, onOutput: (String) -> Unit) {
        Thread {
            try {
                val binPath = setupToolchain()
                val proc = ProcessBuilder("/system/bin/sh", "-c", command).apply {
                    directory(File(workingDir).takeIf { it.exists() } ?: context.filesDir)
                    redirectErrorStream(true)
                    environment().apply {
                        put("HOME", context.filesDir.absolutePath)
                        put("TMPDIR", context.cacheDir.absolutePath)
                        put("TERM", "xterm-256color")
                        put("LD_LIBRARY_PATH", context.applicationInfo.nativeLibraryDir)
                        put("PATH", "$binPath:/system/bin:/system/xbin:${context.applicationInfo.nativeLibraryDir}")
                    }
                }.start()
                
                proc.inputStream.bufferedReader().forEachLine { line ->
                    onOutput(line + "\n")
                }
                proc.waitFor()
            } catch (e: Exception) {
                onOutput("[ShellEngine] Exception: ${e.message}\n")
            }
        }.apply { isDaemon = true; start() }
    }

    private fun pipeStream(stream: java.io.InputStream, prefix: String) {
        Thread {
            try {
                stream.bufferedReader().forEachLine { line ->
                    appendOutput("$prefix$line\n")
                }
            } catch (_: Exception) {}
        }.apply { isDaemon = true; start() }
    }

    private fun watchExit(proc: Process) {
        Thread {
            val code = proc.waitFor()
            isRunning = false
            appendOutput("[ShellEngine] Exited — code $code\n")
        }.apply { isDaemon = true; start() }
    }

    private fun setupToolchain(): String {
        val binDir = File(context.filesDir, "bin").apply {
            if (!mkdirs() && !exists()) {
                Log.w(TAG, "mkdirs() failed and bin directory is missing at $absolutePath")
            }
        }
        val libToybox = File(context.applicationInfo.nativeLibraryDir, "libtoybox.so")
        val toyboxLink = File(binDir, "toybox")
        
        if (!libToybox.exists()) {
            Log.w(TAG, "libtoybox.so missing at ${libToybox.absolutePath}")
            return binDir.absolutePath
        }
        
        val desiredTarget = libToybox.absolutePath
        val currentTarget = try {
            Os.readlink(toyboxLink.absolutePath)
        } catch (e: ErrnoException) {
            null
        } catch (e: Exception) {
            null
        }
        
        if (currentTarget != desiredTarget) {
            if (toyboxLink.exists()) {
                toyboxLink.delete()
            }
            try {
                Os.symlink(desiredTarget, toyboxLink.absolutePath)
                val proc = ProcessBuilder(toyboxLink.absolutePath, "--install", "-s", binDir.absolutePath)
                    .directory(binDir)
                    .start()
                proc.waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup toybox symlinks: ${e.message}", e)
            }
        }
        
        val libMake = File(context.applicationInfo.nativeLibraryDir, "libmake.so")
        if (libMake.exists()) {
            val makeLink = File(binDir, "make")
            if (makeLink.exists()) makeLink.delete()
            try {
                Os.symlink(libMake.absolutePath, makeLink.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create make symlink: ${e.message}")
            }
        }
        return binDir.absolutePath
    }

    private fun appendOutput(line: String) {
        outputCallback?.invoke(line)
    }
}
