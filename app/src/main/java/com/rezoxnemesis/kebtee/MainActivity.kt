package com.rezoxnemesis.kebtee

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush as UiBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rezoxnemesis.kebtee.wallpaper.KebTeeLiveWallpaperService

private val Night = Color(0xFF070B18)
private val Panel = Color(0xFF11182B)
private val Violet = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF22D3EE)
private val Pink = Color(0xFFEC4899)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Violet,
                    secondary = Cyan,
                    tertiary = Pink,
                    background = Night,
                    surface = Panel
                )
            ) {
                KebTeeHome(onLiveWallpaper = { openWallpaperPicker() })
            }
        }
    }

    private fun openWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, KebTeeLiveWallpaperService::class.java)
            )
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}

private data class Feature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun KebTeeHome(onLiveWallpaper: () -> Unit) {
    val features = listOf(
        Feature("Live Wallpaper Studio", "Animated scenes for your screen", Icons.Default.Wallpaper, Cyan),
        Feature("Theme Studio", "Colors, styles and visual presets", Icons.Default.Palette, Violet),
        Feature("Home Experience", "A more personal home screen", Icons.Default.PhoneAndroid, Pink),
        Feature("Volume Lab", "Explore custom panel concepts", Icons.Default.Tune, Cyan),
        Feature("Notification Style", "A companion notification dashboard", Icons.Default.Notifications, Violet),
        Feature("Effects & Motion", "Smooth, battery-aware visuals", Icons.Default.Bolt, Pink)
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UiBrush.verticalGradient(listOf(Color(0xFF10172D), Night, Color(0xFF090D1C))))
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).background(
                        UiBrush.linearGradient(listOf(Cyan, Violet, Pink)),
                        RoundedCornerShape(17.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("KebTee", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("YOUR PHONE, YOUR STYLE", fontSize = 10.sp, letterSpacing = 2.sp, color = Cyan)
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Make it yours.", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "A customization studio for your everyday screen.",
                fontSize = 14.sp,
                color = Color(0xFFADB8D1),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(26.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .background(UiBrush.linearGradient(listOf(Color(0xFF202D54), Color(0xFF17152F), Color(0xFF11182B))))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, tint = Cyan, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("KebTee Aurora", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Your first live wallpaper", fontSize = 12.sp, color = Color(0xFFBBC6E0))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "A flowing aurora with glowing particles, designed to pause when not visible.",
                        fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFD8E0F5)
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onLiveWallpaper,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Preview & Apply Wallpaper")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("CUSTOMIZATION LABS", fontSize = 11.sp, letterSpacing = 1.8.sp, color = Color(0xFF98A6C8), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(features) { feature -> FeatureCard(feature = feature, onClick = onLiveWallpaper) }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Panel,
        border = BorderStroke(1.dp, Color(0xFF283451))
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(40.dp).background(feature.accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, contentDescription = null, tint = feature.accent)
            }
            Column {
                Text(feature.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(feature.subtitle, color = Color(0xFF9EABC9), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}
