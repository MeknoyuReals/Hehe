package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LoaderViewModel
import com.example.viewmodel.LoaderViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // ViewModel State Flow Collections
    val scriptInput by viewModel.scriptInput.collectAsStateWithLifecycle()
    val customLabel by viewModel.customLabel.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val generatedScript by viewModel.generatedScript.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val savedScripts by viewModel.savedScripts.collectAsStateWithLifecycle()

    var copySuccess by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Pulsing green/red status light animation
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. HEADER HERO BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NeonViolet.copy(alpha = 0.15f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Logo Image or Fallback Glow Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(24.dp, shape = CircleShape, ambientColor = NeonViolet, spotColor = NeonViolet)
                            .clip(CircleShape)
                            .border(2.dp, NeonViolet, CircleShape)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "MEKNOYU LOADER",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Roblox Script Generation & Utility Tool",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. LIVE SYSTEM STATUS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Status Koneksi",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Indicator Pulse Badge
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
                            text = if (isLoaded) "LOADED" else "UNLOADED",
                            color = if (isLoaded) NeonGreen else NeonRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 3. SCRIPT INPUT & LOADER BUTTONS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Masukkan Nomer / ID Script",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { viewModel.onInputChange(it) },
                        placeholder = {
                            Text(
                                "Contoh: 10459 atau main_hub",
                                color = TextSecondary.copy(alpha = 0.6f)
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
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (scriptInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onInputChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus input",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    )

                    // Actions Row: Loader & Save Favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main LOADER Button (glowing neon gradient background)
                        Button(
                            onClick = { viewModel.loadScript() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = NeonViolet, spotColor = NeonViolet)
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Load icon",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "LOADER",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Add to Favorites Star Icon Button
                        IconButton(
                            onClick = {
                                if (scriptInput.trim().isNotEmpty()) {
                                    showSaveDialog = true
                                } else {
                                    Toast.makeText(context, "Masukkan nomer script dulu!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(1.dp, Color(0xFF2C2F42), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Simpan Favorit",
                                tint = NeonPink
                            )
                        }
                    }
                }
            }
        }

        // 4. ANIMATED GENERATED CODE OUTPUT PANEL
        item {
            AnimatedVisibility(
                visible = isLoaded && generatedScript != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                generatedScript?.let { code ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan)
                                    )
                                    Text(
                                        text = "Meknoyu Script Code",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }

                                Text(
                                    text = "Ready to Paste",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Raw Script Box (Monospace terminal style)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF2C2F42), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .testTag("output_code")
                            ) {
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = NeonCyan,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Copy & Unloader Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // "Copy to Clipboard" Action Button
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(code))
                                        scope.launch {
                                            copySuccess = true
                                            delay(2000)
                                            copySuccess = false
                                        }
                                        Toast.makeText(context, "Script disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (copySuccess) NeonGreen else NeonCyan
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(45.dp)
                                        .testTag("copy_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copySuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy icon",
                                            tint = if (copySuccess) Color.White else DarkBg
                                        )
                                        Text(
                                            text = if (copySuccess) "TERSALIN!" else "SALIN SCRIPT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (copySuccess) Color.White else DarkBg
                                        )
                                    }
                                }

                                // "Unloader" Button to disable/reset the script loader
                                Button(
                                    onClick = { viewModel.unloadScript() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, NeonRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(45.dp)
                                        .testTag("unloader_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Unload icon",
                                            tint = NeonRed
                                        )
                                        Text(
                                            text = "UNLOADER",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NeonRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. FAVORITES / LOGS HISTORY
        item {
            Text(
                text = "Script Favorit Anda",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (savedScripts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Empty",
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada script favorit yang disimpan",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedScripts) { script ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A2E3D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.loadFromFavorite(script) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonViolet.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Script icon",
                                    tint = NeonViolet
                                )
                            }

                            Column {
                                Text(
                                    text = script.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "https://meknoyu.com/Loader/${script.scriptIdOrUrl}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Load Checkmark Button
                            IconButton(
                                onClick = { viewModel.loadFromFavorite(script) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Load script",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Favorite Script Button
                            IconButton(
                                onClick = { viewModel.deleteFavorite(script) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = NeonRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. ROBLOX TUTORIAL / ACCORDION GUIDE
        item {
            Text(
                text = "Panduan Eksekusi Roblox",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2F42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Cara Menjalankan Script di Roblox",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Divider(color = Color(0xFF2C2F42))

                    val steps = listOf(
                        "Salin script dari tombol SALIN SCRIPT diatas.",
                        "Buka Roblox dan jalankan game pilihan Anda.",
                        "Buka aplikasi Executor (seperti Delta, Arceus X, Codex, Hydrogen, Fluxus).",
                        "Tempel/Paste script loadstring ke dalam tab editor di Executor.",
                        "Klik tombol Run / Execute di Executor.",
                        "Untuk menghentikan script, salin kode unloader atau tekan tombol UNLOADER di aplikasi ini."
                    )

                    steps.forEachIndexed { index, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = step,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // dialog popup to save favorites
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Simpan ke Favorit",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Masukkan nama / label untuk script dengan ID '$scriptInput':",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { viewModel.onLabelChange(it) },
                        placeholder = { Text("Contoh: Hub Utama Meknoyu") },
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
