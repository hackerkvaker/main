package com.example.bonfie

import android.content.Context
import com.chaquo.python.Python

object Server {
    @Volatile
    private var isRunning = false

    fun scanPorts(
        ip: String,
        portRange: String,
        delay: Int,
        onResult: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        if (isRunning) return
        isRunning = true
        Thread {
            try {
                val py = Python.getInstance()
                val pyObject = py.getModule("port")

                val results = try {
                    pyObject.callAttr("scan_ports", ip, portRange, delay).toString().split("\n")
                } catch (e: Exception) {
                    onResult("Error: ${e.message}")
                    return@Thread
                }

                results.forEach { result ->
                    if (!isRunning) return@forEach
                    if (result.contains("open")) onResult(result)
                }
            } catch (e: Exception) {
                onResult("Error: ${e.message}")
            } finally {
                isRunning = false
                onComplete()
            }
        }.start()
    }

    fun scanSite(input: String, onResult: (String) -> Unit, context: Context) {
        if (isRunning) return
        isRunning = true
        Thread {
            try {
                val py = Python.getInstance()
                val pyObject = py.getModule("scanner")

                val result = try {
                    pyObject.callAttr("scan_site", input).toString()
                } catch (e: Exception) {
                    onResult("Error: ${e.message}")
                    return@Thread
                }
                onResult(result)
            } catch (e: Exception) {
                onResult("Error: ${e.message}")
            } finally {
                isRunning = false
            }
        }.start()
    }

    fun getLogs(onResult: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        Thread {
            try {
                val py = Python.getInstance()
                val pyObject = py.getModule("db")

                val result = try {
                    pyObject.callAttr("get_all_results").toString()
                } catch (e: Exception) {
                    onResult("Error: ${e.message}")
                    return@Thread
                }

                onResult(result)
            } catch (e: Exception) {
                onResult("Error: ${e.message}")
            } finally {
                isRunning = false
            }
        }.start()
    }
}








