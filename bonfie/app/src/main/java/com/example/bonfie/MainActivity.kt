package com.example.bonfie

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONArray
import org.json.JSONObject

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent { MainScreen() }
    }
}

val customFont = FontFamily(Font(resId = R.font.d))

@Composable
fun MainScreen() {
    var selectedMenu by remember { mutableStateOf<String?>(null) }
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF090A12), Color(0xFF161A28), Color(0xFF20263D))
    )

    Box(Modifier.fillMaxSize().background(backgroundBrush), contentAlignment = Alignment.Center) {
        if (selectedMenu == null) {
            MenuScreen { selectedMenu = it }
        } else {
            AnimatedWindow(selectedMenu!!) { selectedMenu = null }
        }
    }
}

@Composable
fun MenuScreen(onSelect: (String) -> Unit) {
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MAIN MENU", fontSize = 32.sp, color = Color(0xFFEAA4E4), fontFamily = customFont)
        listOf("Port Scanner", "Site & IP Scanner", "Log").forEach { label ->
            MenuButton(label) { onSelect(label) }
        }

        MenuButton("Exit") {
            (context as? Activity)?.finish()
        }
    }
}

@Composable
fun MenuButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(5.dp)
            .fillMaxWidth(0.6f)
            .height(50.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, color = Color.White, fontFamily = customFont)
    }
}

@Composable
fun AnimatedWindow(title: String, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.75f)
            .wrapContentHeight()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(15.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 20.sp, color = Color.White, fontFamily = customFont)
            Text("✖", fontSize = 20.sp, color = Color.White, modifier = Modifier.clickable { onClose() })
        }
        Spacer(Modifier.height(20.dp))
        when (title) {
            "Port Scanner" -> {
                PortScannerContent()
            }
            "Site & IP Scanner" -> {
                SiteScannerContent()
            }
            "Log" -> {
                LogContent()
            }
        }
    }
}




@Composable
fun LogContent() {
    val logResults = remember { mutableStateOf("") }

    LaunchedEffect(true) {
        Server.getLogs { result ->
            logResults.value = result
        }
    }

    if (logResults.value.isEmpty()) {
        Text("No logs available", fontSize = 18.sp, color = Color.White, fontFamily = customFont)
    } else {
        val formattedResult = formatLogResponse(logResults.value)

        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(end = 8.dp).fillMaxHeight(),
                contentPadding = PaddingValues(8.dp)
            ) {
                val ipLogs = formattedResult.first
                items(ipLogs) { ipLog ->
                    Text(ipLog, fontSize = 16.sp, color = Color.White, fontFamily = customFont)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(start = 8.dp).fillMaxHeight(),
                contentPadding = PaddingValues(8.dp)
            ) {
                val websiteLogs = formattedResult.second
                items(websiteLogs) { websiteLog ->
                    Text(websiteLog, fontSize = 16.sp, color = Color.White, fontFamily = customFont)
                }
            }
        }
    }
}

fun formatLogResponse(response: String): Pair<List<String>, List<String>> {
    return try {
        val jsonObject = JSONObject(response)

        val websites = jsonObject.optJSONArray("website") ?: JSONArray()
        val ipAddresses = jsonObject.optJSONArray("ip") ?: JSONArray()

        val websiteLogs = mutableListOf<String>()
        val ipLogs = mutableListOf<String>()

        if (websites.length() > 0) {
            for (i in 0 until websites.length()) {
                val website = websites.getJSONObject(i)
                websiteLogs.add(
                    "URL: ${website.optString("url", "N/A")}, " +
                            "Status Code: ${website.optInt("status_code", -1)}, " +
                            "Content Type: ${website.optString("content_type", "N/A")}, " +
                            "WHOIS Info: ${website.optString("whois_info", "N/A")}, " +
                            "Timestamp: ${website.optString("timestamp", "N/A")}"
                )
            }
        } else {
            websiteLogs.add("No Website Logs found.")
        }

        if (ipAddresses.length() > 0) {
            for (i in 0 until ipAddresses.length()) {
                val ip = ipAddresses.getJSONObject(i)
                ipLogs.add(
                    "IP Address: ${ip.optString("ip_address", "N/A")}, " +
                            "ASN: ${ip.optInt("asn", -1)}, " +
                            "Organization: ${ip.optString("organization", "N/A")}, " +
                            "Country: ${ip.optString("country", "N/A")}, " +
                            "City: ${ip.optString("city", "N/A")}, " +
                            "Region: ${ip.optString("region", "N/A")}, " +
                            "Timezone: ${ip.optString("timezone", "N/A")}, " +
                            "Timestamp: ${ip.optString("timestamp", "N/A")}"
                )
            }
        } else {
            ipLogs.add("No IP Logs found.")
        }

        Pair(ipLogs, websiteLogs)
    } catch (e: Exception) {
        Pair(listOf("Error formatting log response: ${e.message}"), listOf())
    }
}

@Composable
fun SiteScannerContent() {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InputField("Enter IP or URL", input) { input = it }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            StartButton(isScanning) {
                if (!isScanning) {
                    isScanning = true
                    result = ""
                    Server.scanSite(input, { newResult ->
                        result = newResult
                        isScanning = false
                    }, context)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ResultField(result)
    }
}

@Composable
fun PortScannerContent() {
    var ip by remember { mutableStateOf("") }
    var ports by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var speed by remember { mutableFloatStateOf(5f) }
    var isScanning by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            InputField("Enter IP or URL", ip) { ip = it }
            Spacer(Modifier.width(10.dp))
            InputField("Port Range (e.g. 20-80)", ports) { ports = it }
        }
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StartButton(isScanning) {
                if (!isScanning) {
                    isScanning = true
                    result = ""
                    Server.scanPorts(ip, ports, speed.toInt(), { newResult ->
                        result += "\n$newResult"
                    }) { isScanning = false }
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Speed: ${speed.toInt()}", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFEAA4E4),
                        activeTrackColor = Color(0xFFEAA4E4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.width(200.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ResultField(result)
    }
}

@Composable
fun StartButton(isScanning: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(50.dp)
            .width(80.dp)
            .background(
                if (isScanning) Color.Gray else Color(0xFFEAA4E4),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !isScanning) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(if (isScanning) "Scanning..." else "Start", fontSize = 16.sp, color = Color.White, fontFamily = customFont)
    }
}

@Composable
fun ResultField(result: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopStart
    ) {
        Text(result, fontSize = 14.sp, color = Color.White, fontFamily = customFont)
    }
}

@Composable
fun InputField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .height(50.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.White.copy(alpha = 0.6f))
                }
                innerTextField()
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White)
    )
}
