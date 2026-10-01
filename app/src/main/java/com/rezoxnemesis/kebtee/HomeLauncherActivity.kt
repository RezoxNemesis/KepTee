package com.rezoxnemesis.kebtee
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

private data class HomeApp(val label: String, val packageName: String, val className: String, val icon: Bitmap)

class HomeLauncherActivity : ComponentActivity() {
    private val night = Color(0xFF070B18)
    private val panel = Color(0xFF121A2D)
    private val cyan = Color(0xFF22D3EE)
    private val violet = Color(0xFF8B5CF6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 24)
        val apps = loadApps()
        setContent {
            var search by remember { mutableStateOf("") }
            val filtered = remember(search, apps) {
                if (search.isBlank()) apps else apps.filter { it.label.contains(search.trim(), ignoreCase = true) }
            }
            BackHandler(enabled = true) { }
            MaterialTheme(colorScheme = darkColorScheme(primary = violet, secondary = cyan, background = night, surface = panel)) {
                Column(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10172D), night, Color(0xFF090D1C))))
                        .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("KebTee", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("YOUR SPACE. YOUR APPS.", color = cyan, fontSize = 10.sp, letterSpacing = 1.7.sp)
                        }
                        Surface(color = Color(0xFF182440), shape = RoundedCornerShape(18.dp)) {
                            Text("${apps.size} APPS", color = Color(0xFFB9C7E5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { startActivity(Intent(this@HomeLauncherActivity, MainActivity::class.java)) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Open KebTee customization dashboard", tint = cyan)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Find your next move.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Your installed apps, one tap away.", color = Color(0xFFADB8D1), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
                    OutlinedTextField(
                        value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Search apps", color = Color(0xFF8997B8)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = cyan) },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = cyan, unfocusedBorderColor = Color(0xFF293451),
                            cursorColor = cyan, focusedContainerColor = panel, unfocusedContainerColor = panel
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    if (filtered.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No apps match that search.", color = Color(0xFFADB8D1), textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4), modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            items(filtered, key = { it.packageName + "/" + it.className }) { app ->
                                Column(Modifier.fillMaxWidth().clickable { launchApp(app) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(color = Color(0xFF17213A), shape = RoundedCornerShape(20.dp), modifier = Modifier.size(58.dp)) {
                                        Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label, modifier = Modifier.padding(9.dp))
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(app.label, color = Color(0xFFE8EDFA), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    Text("Hold nothing back. Make this home yours.", color = Color(0xFF7786A8), fontSize = 10.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadApps(): List<HomeApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL).mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            try {
                HomeApp(info.loadLabel(packageManager).toString(), activity.packageName, activity.name, info.loadIcon(packageManager).toBitmap(96, 96))
            } catch (_: Exception) { null }
        }.distinctBy { it.packageName + "/" + it.className }.sortedBy { it.label.lowercase() }
    }

    private fun launchApp(app: HomeApp) {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.className)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }
}
