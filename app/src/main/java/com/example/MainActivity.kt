package com.example

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.LoaderScript
import com.example.data.LoaderScriptRepository
import com.example.ui.theme.*
import com.example.viewmodel.LoaderViewModel
import com.example.viewmodel.LoaderViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = LoaderScriptRepository(database.loaderScriptDao())
        
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBg
                ) { innerPadding ->
                    LoaderAppScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Global helper functions for asset management and export
fun readAssetContent(context: Context, fileName: String): String {
    return try {
        context.assets.open(fileName).use { input ->
            input.bufferedReader().use { it.readText() }
        }
    } catch (e: Exception) {
        "Gagal membaca file dari assets: ${e.message}"
    }
}

fun writeSingleFileToDownloads(context: Context, fileName: String, content: String): String? {
    try {
        // Modern approach using MediaStore for Android 10+ (Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/MeknoyuLoader")
            }
            
            // Delete existing to avoid duplicate name appends (e.g. meknoyu_hub (1).lua)
            try {
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, "Download/MeknoyuLoader/")
                resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
            } catch (e: Exception) {
                // Ignore delete errors
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray())
                }
                return "Penyimpanan Publik: /sdcard/Download/MeknoyuLoader/$fileName"
            }
        } else {
            // Legacy approach for older devices
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val customFolder = File(publicDir, "MeknoyuLoader")
            if (!customFolder.exists()) {
                customFolder.mkdirs()
            }
            val outFile = File(customFolder, fileName)
            FileOutputStream(outFile).use { output ->
                output.write(content.toByteArray())
            }
            return outFile.absolutePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // Always write to the App's External Files Dir as a solid, fail-safe backup!
    // This folder is guaranteed 100% writable on all Android versions and fully openable in ZArchiver!
    try {
        val backupFolder = File(context.getExternalFilesDir(null), "MeknoyuLoader")
        if (!backupFolder.exists()) {
            backupFolder.mkdirs()
        }
        val outFile = File(backupFolder, fileName)
        FileOutputStream(outFile).use { output ->
            output.write(content.toByteArray())
        }
        return "Penyimpanan Aplikasi: /sdcard/Android/data/${context.packageName}/files/MeknoyuLoader/$fileName"
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun exportAllAssetsToDownloads(context: Context): List<String> {
    val results = mutableListOf<String>()
    val assetsList = listOf(
        "meknoyu_hub.lua",
        "blox_fruits_autofarm.lua",
        "universal_fly.lua",
        "aimlock_universal.lua",
        "meknoyu_loader_guide.txt"
    )
    
    for (fileName in assetsList) {
        val content = readAssetContent(context, fileName)
        val path = writeSingleFileToDownloads(context, fileName, content)
        if (path != null) {
            results.add(path)
        }
    }
    return results
}

fun shareTextContent(context: Context, title: String, textContent: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, textContent)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Kode Script"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan script: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderAppScreen(
    repository: LoaderScriptRepository,
    modifier: Modifier = Modifier,
    viewModel: LoaderViewModel = viewModel(factory = LoaderViewModelFactory(repository))
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Tab state
    var activeTab by remember { mutableStateOf(0) }

    // ViewModel State Flow Collections
    val scriptInput by viewModel.scriptInput.collectAsStateWithLifecycle()
    val customLabel by viewModel.customLabel.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val generatedScript by viewModel.generatedScript.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val savedScripts by viewModel.savedScripts.collectAsStateWithLifecycle()

    var copySuccess by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Script Hub dialog details
    var selectedViewScript by remember { mutableStateOf<String?>(null) }
    var selectedViewScriptContent by remember { mutableStateOf("") }

    // Export Dialog state
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedPathsList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Column(modifier = Modifier.background(DarkBg)) {
                // 1. TOP HEADER BRANDING
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(NeonViolet.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(16.dp, shape = CircleShape, ambientColor = NeonViolet, spotColor = NeonViolet)
                                .clip(CircleShape)
                                .border(1.5.dp, NeonViolet, CircleShape)
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.meknoyu_logo),
                                contentDescription = "Meknoyu Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column {
                            Text(
                                text = "MEKNOYU LOADER",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Roblox Script Manager & ZArchiver Ready",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 2. MATERIAL DESIGN 3 TAB NAVIGATION
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SurfaceDark,
                    contentColor = NeonViolet,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = NeonViolet,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(color = Color(0xFF232533)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = "Loader", modifier = Modifier.size(16.dp))
                                Text("LOADER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = NeonViolet,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Bundle", modifier = Modifier.size(16.dp))
                                Text("BUNDLE FILE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = NeonViolet,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "ZArchiver", modifier = Modifier.size(16.dp))
                                Text("ZARCHIVER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = NeonViolet,
                        unselectedContentColor = TextSecondary
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        // 3. MAIN CONTENTS BY SELECTED TAB
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: THE MAIN LOADSTRING GENERATOR
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            Button(
                                onClick = { viewModel.unloadAll() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("unload_all_button")
                            ) {
                                Text("UNLOAD ALL", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Live Status Connection Banner
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Status Pemuat",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = statusMessage,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isLoaded) NeonGreen.copy(alpha = 0.1f)
                                                else NeonRed.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isLoaded) NeonGreen.copy(alpha = alphaPulse)
                                                    else NeonRed.copy(alpha = alphaPulse)
                                                )
                                        )
                                        Text(
                                            text = if (isLoaded) "AKTIF" else "IDLE",
                                            color = if (isLoaded) NeonGreen else NeonRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Code input box
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Tempel Loadstring / Nomer Script",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    OutlinedTextField(
                                        value = scriptInput,
                                        onValueChange = { viewModel.onInputChange(it) },
                                        placeholder = {
                                            Text(
                                                "Contoh: hub1, fruits, fly, 10459",
                                                color = TextSecondary.copy(alpha = 0.5f),
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("script_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonViolet,
                                            unfocusedBorderColor = Color(0xFF2C2F42),
                                            focusedContainerColor = SurfaceDark,
                                            unfocusedContainerColor = SurfaceDark,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        trailingIcon = {
                                            if (scriptInput.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.onInputChange("") }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Hapus",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.loadScript() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            contentPadding = PaddingValues(),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .shadow(8.dp, RoundedCornerShape(10.dp), ambientColor = NeonViolet, spotColor = NeonViolet)
                                                .testTag("loader_button")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(NeonViolet, NeonPink)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    Text("LOADER", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (scriptInput.trim().isNotEmpty()) {
                                                    showSaveDialog = true
                                                } else {
                                                    Toast.makeText(context, "Masukkan ID Script dulu!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, Color(0xFF2C2F42), RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Simpan", tint = NeonPink, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Generated loadstring display
                        item {
                            AnimatedVisibility(
                                visible = isLoaded && generatedScript != null,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                generatedScript?.let { code ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                                                    Text("Sintaks Loadstring Siap", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                                }
                                                Text("Meknoyu Active", fontSize = 10.sp, color = TextSecondary)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                                                    .border(1.dp, Color(0xFF232533), RoundedCornerShape(10.dp))
                                                    .padding(12.dp)
                                                    .testTag("output_code")
                                            ) {
                                                Text(
                                                    text = code,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = NeonCyan,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(code))
                                                        scope.launch {
                                                            copySuccess = true
                                                            delay(2000)
                                                            copySuccess = false
                                                        }
                                                        Toast.makeText(context, "Script disalin! Paste di Roblox.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (copySuccess) NeonGreen else NeonCyan
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1.2f)
                                                        .height(44.dp)
                                                        .testTag("copy_button")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (copySuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                                            contentDescription = null,
                                                            tint = if (copySuccess) Color.White else DarkBg,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (copySuccess) "TERSALIN" else "SALIN KODE",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = if (copySuccess) Color.White else DarkBg
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { viewModel.unloadScript() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                                    border = BorderStroke(1.dp, NeonRed),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(0.9f)
                                                        .height(44.dp)
                                                        .testTag("unloader_button")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                                                        Text("UNLOADER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonRed)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Favorites Title
                        item {
                            Text(
                                text = "Daftar Script Favorit",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Saved scripts rendering
                        if (savedScripts.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Belum ada script favorit yang disimpan", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(savedScripts) { script ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2A2E3D)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.loadFromFavorite(script) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NeonViolet.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                                            }

                                            Column {
                                                Text(script.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("ID: ${script.scriptIdOrUrl}", fontSize = 10.sp, color = TextSecondary)
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.loadFromFavorite(script) },
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceDark)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Load", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteFavorite(script) },
                                                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceDark)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = NeonRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: LOCAL LUA BUNDLE FILE LIST
                    val assetsList = remember {
                        listOf(
                            Triple("meknoyu_hub.lua", "Meknoyu Hub Premium V1", "Koleksi cheat menu lengkap Roblox mobile"),
                            Triple("blox_fruits_autofarm.lua", "Blox Fruits AutoFarm", "Script otomatis farming exp dan level"),
                            Triple("universal_fly.lua", "Universal Fly Hack", "Bisa terbang di semua game Roblox"),
                            Triple("aimlock_universal.lua", "Silent Aim Universal", "Kunci otomatis bidikan ke kepala lawan"),
                            Triple("meknoyu_loader_guide.txt", "Meknoyu Execution Guide", "File panduan lengkap cara pakai script")
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = NeonPink, modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("Bundel Script APK Fisik", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Semua script ini tersimpan asli di dalam APK dan siap diekstrak!", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        items(assetsList) { (fileName, title, desc) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (fileName.endsWith(".lua")) Icons.Default.Code else Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (fileName.endsWith(".lua")) NeonCyan else NeonGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(fileName, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                            }
                                        }

                                        // Extension badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (fileName.endsWith(".lua")) NeonCyan.copy(alpha = 0.15f)
                                                    else NeonGreen.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (fileName.endsWith(".lua")) "LUA" else "TXT",
                                                color = if (fileName.endsWith(".lua")) NeonCyan else NeonGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(desc, fontSize = 12.sp, color = TextSecondary)

                                    HorizontalDivider(color = Color(0xFF232533))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // View script button
                                        TextButton(
                                            onClick = {
                                                val content = readAssetContent(context, fileName)
                                                selectedViewScript = fileName
                                                selectedViewScriptContent = content
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text("BACA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Quick copy button
                                        TextButton(
                                            onClick = {
                                                val content = readAssetContent(context, fileName)
                                                clipboardManager.setText(AnnotatedString(content))
                                                Toast.makeText(context, "$fileName berhasil disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = NeonViolet),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text("SALIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Export single button
                                        TextButton(
                                            onClick = {
                                                val content = readAssetContent(context, fileName)
                                                val path = writeSingleFileToDownloads(context, fileName, content)
                                                if (path != null) {
                                                    Toast.makeText(context, "Berhasil simpan ke: $path", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Gagal mengekspor file!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text("EKSPOR HP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Share Button
                                        IconButton(
                                            onClick = {
                                                val content = readAssetContent(context, fileName)
                                                shareTextContent(context, title, content)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonPink, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: ZARCHIVER DIRECT EXPORTER & EDUCATION
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = NeonPink, modifier = Modifier.size(24.dp))
                                        Text(
                                            text = "Integrasi ZArchiver Sempurna",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Text(
                                        text = "Aplikasi ini memuat 5 file script asli Roblox di dalam kodenya. Anda bisa langsung memindahkan seluruh script ini ke luar APK agar bisa dibaca langsung oleh ZArchiver dan dimasukkan ke folder executor Roblox (seperti Delta/ArceusX)!",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val paths = exportAllAssetsToDownloads(context)
                                            if (paths.isNotEmpty()) {
                                                exportedPathsList = paths
                                                showExportSuccessDialog = true
                                            } else {
                                                Toast.makeText(context, "Gagal mengekspor file script!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .shadow(8.dp, RoundedCornerShape(10.dp), ambientColor = NeonPink, spotColor = NeonPink)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(NeonPink, NeonViolet)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                                                Text(
                                                    text = "EKSPOR SEMUA KE STORAGE (ZARCHIVER)",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tutorial List
                        item {
                            Text(
                                text = "Langkah Install & Buka di ZArchiver:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        val steps = listOf(
                            "Tekan tombol EKSPOR SEMUA diatas untuk memindahkan file keluar dari APK.",
                            "Buka aplikasi ZArchiver di HP Android Anda.",
                            "Masuk ke folder Download lalu cari folder MeknoyuLoader.",
                            "Atau masuk ke folder Android/data/com.aistudio.meknoyuloader.xbswty/files/MeknoyuLoader/ di ZArchiver.",
                            "Di sana Anda akan melihat semua file .lua dan .txt lengkap, tidak ada yang bohong!",
                            "Gunakan ZArchiver untuk menyalin file .lua tersebut ke folder executor Roblox Anda, atau buka file tersebut untuk menyalin kodenya."
                        )

                        items(steps.size) { index ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(NeonPink.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = NeonPink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = steps[index],
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // A. IN-APP SCRIPT VIEWER DIALOG
    if (selectedViewScript != null) {
        AlertDialog(
            onDismissRequest = { selectedViewScript = null },
            title = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedViewScript ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedViewScript = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    // Raw Code Box with Vertical/Horizontal Scroll
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceDark, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2C2F42), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = selectedViewScriptContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeonCyan,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(selectedViewScriptContent))
                            Toast.makeText(context, "Script disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DarkBg, modifier = Modifier.size(14.dp))
                            Text("SALIN KODE", fontSize = 11.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            selectedViewScript?.let { fileName ->
                                val path = writeSingleFileToDownloads(context, fileName, selectedViewScriptContent)
                                if (path != null) {
                                    Toast.makeText(context, "Tersimpan ke: $path", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan file!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("SIMPAN FILE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }

    // B. EXPORT SUCCESS DIALOG
    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                    Text("Ekspor Berhasil!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Semua file script (.lua & .txt) telah berhasil disalin keluar dari APK ke penyimpanan internal Anda!",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Lokasi Folder ZArchiver:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )

                    // Path string display box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2C2F42), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "1. /sdcard/Download/MeknoyuLoader/",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                            Text(
                                text = "2. /sdcard/Android/data/com.aistudio.meknoyuloader.xbswty/files/MeknoyuLoader/",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }
                    }

                    Text(
                        text = "Silakan buka aplikasi ZArchiver, masuk ke salah satu folder di atas, dan semua script siap dieksekusi di Roblox!",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("/sdcard/Download/MeknoyuLoader/"))
                        Toast.makeText(context, "Path disalin!", Toast.LENGTH_SHORT).show()
                        showExportSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Text("Salin Path & Tutup", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // C. SAVE TO FAVORITES PROMPT DIALOG
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Simpan ke Favorit",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan nama / label untuk script dengan ID '$scriptInput':",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { viewModel.onLabelChange(it) },
                        placeholder = { Text("Contoh: Hub Utama Meknoyu", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = Color(0xFF2C2F42),
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentScriptToFavorites()
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
