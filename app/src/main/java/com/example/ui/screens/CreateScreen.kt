package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseTertiary
import com.example.ui.viewmodel.PulseStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: PulseStreamViewModel,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val generatedImageBase64 by viewModel.generatedImageBase64.collectAsStateWithLifecycle()
    val generatorError by viewModel.generatorError.collectAsStateWithLifecycle()

    // Post Form States
    var caption by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf("POST") } // "POST", "SHORT", "VIDEO"
    var mediaUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tech") }
    var aspectRatio by remember { mutableStateOf("1:1") }

    // AI Generation States
    var aiPrompt by remember { mutableStateOf("") }
    var useProModel by remember { mutableStateOf(false) }

    // Categories list
    val categories = listOf("Tech", "Fashion", "Music", "Travel", "Design", "Comedy", "General")

    // Aspect Ratios list
    val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")

    // Decode generated base64 image
    val generatedBitmap = remember(generatedImageBase64) {
        if (!generatedImageBase64.isNullOrEmpty()) {
            try {
                val cleanBase64 = if (generatedImageBase64!!.contains(",")) {
                    generatedImageBase64!!.split(",")[1]
                } else {
                    generatedImageBase64!!
                }
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Auto-populate the form media link if an image is generated successfully
    LaunchedEffect(generatedImageBase64) {
        if (!generatedImageBase64.isNullOrEmpty()) {
            mediaUrl = generatedImageBase64!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compose Stream",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Section 1: Post Type Selection ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "STREAM TYPE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("POST", "SHORT", "VIDEO").forEach { type ->
                            val isSelected = postType == type
                            Surface(
                                onClick = {
                                    postType = type
                                    // Change default aspect ratios based on post types
                                    aspectRatio = when (type) {
                                        "SHORT" -> "9:16"
                                        "VIDEO" -> "16:9"
                                        else -> "1:1"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("type_chip_$type"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Section 2: Gemini AI Custom Image Creator (The Core Feature) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PulsePrimary.copy(alpha = 0.3f))
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PulsePrimary
                        )
                        Text(
                            text = "Gemini AI Poster Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Draft studio-quality graphic posts instantly using Gemini text-to-image models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Prompt Input
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("What visual should Gemini draft?") },
                        placeholder = { Text("e.g., A cinematic cyberpunk street with neon signs...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_field"),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    // Aspect Ratio Selector
                    Text(
                        text = "Specify Aspect Ratio:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Horizontally scrollable aspect ratio chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aspectRatios.forEach { ratio ->
                            val isSelected = aspectRatio == ratio
                            FilterChip(
                                selected = isSelected,
                                onClick = { aspectRatio = ratio },
                                label = { Text(ratio) },
                                modifier = Modifier.testTag("aspect_ratio_chip_$ratio"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PulsePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = PulsePrimary
                                )
                            )
                        }
                    }

                    // Model Quality Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (useProModel) "Studio Quality (Pro model)" else "General Use (Flash model)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (useProModel) "Powered by gemini-3-pro-image-preview" else "Powered by gemini-3.1-flash-image-preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useProModel,
                            onCheckedChange = { useProModel = it },
                            modifier = Modifier.testTag("pro_model_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PulsePrimary,
                                checkedTrackColor = PulsePrimary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (aiPrompt.isBlank()) {
                                Toast.makeText(context, "Please write a prompt first!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.generateAIImage(aiPrompt, aspectRatio, useProModel)
                            }
                        },
                        enabled = !isGeneratingImage && aiPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("generate_image_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulsePrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Text("Generate Image with Gemini", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Loading or Error states or preview
                    AnimatedVisibility(visible = isGeneratingImage) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = PulsePrimary)
                            Text(
                                text = "Pulse AI is drafting your graphic...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PulsePrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This usually takes 10 to 15 seconds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!generatorError.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = generatorError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Show generated image preview
                    if (generatedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    remember(aspectRatio) {
                                        val parts = aspectRatio.split(":")
                                        if (parts.size == 2) parts[0].toFloat() / parts[1].toFloat() else 1f
                                    }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, PulsePrimary, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = generatedBitmap.asImageBitmap(),
                                contentDescription = "Generated AI art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Clear button
                            IconButton(
                                onClick = { viewModel.clearGeneratedImage() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(36.dp)
                                    .testTag("clear_ai_image_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Art",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- Section 3: Post Captions & Publishing ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "POST DETAILS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Caption field
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("What's on your mind? ✍️") },
                        placeholder = { Text("Stream your thought or describe your media...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .testTag("caption_input_field"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Media URL Field (displays generated image base64, or allows pasting a link)
                    OutlinedTextField(
                        value = if (mediaUrl.startsWith("data:image")) "[Gemini AI Image Attached]" else mediaUrl,
                        onValueChange = { if (!it.contains("[Gemini AI")) mediaUrl = it },
                        label = { Text("Media Link / Attachment URL (Optional)") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        enabled = !mediaUrl.startsWith("data:image"), // Lock if it's the generated AI image
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_url_field"),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            if (mediaUrl.isNotEmpty()) {
                                IconButton(onClick = { mediaUrl = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear link",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )

                    // Category scroll list
                    Text(
                        text = "Category Topic:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                modifier = Modifier.testTag("category_chip_$category")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream It Button
                    Button(
                        onClick = {
                            if (caption.isBlank() && mediaUrl.isEmpty()) {
                                Toast.makeText(context, "Please write a caption or generate an image!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createPost(
                                    caption = caption,
                                    type = postType,
                                    mediaUrl = mediaUrl,
                                    aspectRatio = aspectRatio,
                                    category = selectedCategory
                                )
                                Toast.makeText(context, "Post Streamed Successfully! 🚀", Toast.LENGTH_SHORT).show()

                                // Reset local form fields
                                caption = ""
                                mediaUrl = ""
                                postType = "POST"
                                selectedCategory = "Tech"
                                aiPrompt = ""
                                viewModel.clearGeneratedImage()

                                // Callback to navigate back to Home
                                onPostCreated()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_post_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                            Text("Stream Post 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
