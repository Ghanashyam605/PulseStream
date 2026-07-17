package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.database.PostEntity
import com.example.data.database.UserEntity
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseTertiary
import com.example.ui.viewmodel.PulseStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PulseStreamViewModel,
    modifier: Modifier = Modifier
) {
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val moderationQueue by viewModel.moderationQueue.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Posts, 1 = Shorts
    var showAnalytics by remember { mutableStateOf(false) }
    var showModeratorConsole by remember { mutableStateOf(false) }

    // Profile of logged in user "@you"
    val myUser = allUsers.find { it.id == "@you" } ?: UserEntity(
        id = "@you",
        name = "Pulse Streamer",
        avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
        bio = "Pulse Creator streaming dynamic visual media & AI-generated art portfolios. ✨🎨🚀",
        followersCount = 4200,
        followingCount = 120,
        isFollowing = false
    )

    // Filter approved posts created by "@you"
    val myPosts = remember(allPosts) {
        allPosts.filter { it.creatorId == "@you" && it.type != "SHORT" && it.moderationStatus == "APPROVED" }
    }
    val myShorts = remember(allPosts) {
        allPosts.filter { it.creatorId == "@you" && it.type == "SHORT" && it.moderationStatus == "APPROVED" }
    }

    if (showAnalytics) {
        AnalyticsDashboardScreen(
            myPosts = myPosts,
            myShorts = myShorts,
            onBackClick = { showAnalytics = false }
        )
    } else if (showModeratorConsole) {
        ModeratorConsoleScreen(
            moderationQueue = moderationQueue,
            onApprove = { postId -> viewModel.approvePost(postId) },
            onBlock = { postId -> viewModel.blockPost(postId) },
            onBackClick = { showModeratorConsole = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = myUser.id,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Creator",
                                tint = PulsePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_toggle_btn")
                        ) {
                            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
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
            ) {
                // --- Profile details Header ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = myUser.avatarUrl,
                        contentDescription = "My Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = myUser.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = myUser.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Follower Stats Counters ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatColumn(count = myPosts.size + myShorts.size, label = "Stream Posts")
                        ProfileDivider()
                        ProfileStatColumn(count = myUser.followersCount, label = "Followers")
                        ProfileDivider()
                        ProfileStatColumn(count = myUser.followingCount, label = "Following")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Creator Tools Panel ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAnalytics = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("profile_analytics_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showModeratorConsole = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("profile_mod_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mod Console", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Profile Content Tabs ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Streams (${myPosts.size})", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("profile_streams_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shorts (${myShorts.size})", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("profile_shorts_tab")
                    )
                }

                // --- Grid View of My Posts/Shorts ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val currentGridList = if (selectedTab == 0) myPosts else myShorts

                    if (currentGridList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts streamed yet. Go draft some on the Create tab! 🚀",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("profile_media_grid")
                        ) {
                            items(currentGridList, key = { it.id }) { post ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(if (selectedTab == 0) 1f else 0.562f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val isBase64 = post.mediaUrl.startsWith("data:image") || post.mediaUrl.length > 300
                                    val bitmap = remember(post.mediaUrl) {
                                        if (isBase64) {
                                            try {
                                                val cleanBase64 = if (post.mediaUrl.contains(",")) post.mediaUrl.split(",")[1] else post.mediaUrl
                                                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            } catch (e: Exception) { null }
                                        } else { null }
                                    }

                                    if (bitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = post.mediaUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "❤️ ${post.likesCount}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Creator Analytics sub-screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    myPosts: List<PostEntity>,
    myShorts: List<PostEntity>,
    onBackClick: () -> Unit
) {
    var selectedRange by remember { mutableStateOf("7 Days") }
    val ranges = listOf("7 Days", "30 Days", "All Time")

    val factor = when (selectedRange) {
        "7 Days" -> 1
        "30 Days" -> 4
        else -> 10
    }

    val totalLikes = (myPosts.sumOf { it.likesCount } + myShorts.sumOf { it.likesCount }) * factor + (24 * factor)
    val totalViews = (myPosts.sumOf { it.viewsCount } + myShorts.sumOf { it.viewsCount }) * factor + (150 * factor)
    val totalShares = (myPosts.sumOf { it.sharesCount } + myShorts.sumOf { it.sharesCount }) * factor + (12 * factor)
    val totalComments = (myPosts.sumOf { it.commentsCount } + myShorts.sumOf { it.commentsCount }) * factor + (8 * factor)
    val followerGrowth = 14 * factor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Portfolio Analytics", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("analytics_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Range chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ranges.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Text("Overview Performance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsMetricCard(title = "Video Views", count = totalViews, sub = "+${12 * factor}% vs last period", modifier = Modifier.weight(1f))
                AnalyticsMetricCard(title = "Pulse Likes", count = totalLikes, sub = "+${15 * factor}% vs last period", modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsMetricCard(title = "DMs & Comments", count = totalComments, sub = "+${8 * factor}% vs last period", modifier = Modifier.weight(1f))
                AnalyticsMetricCard(title = "Saves & Shares", count = totalShares, sub = "+${20 * factor}% vs last period", modifier = Modifier.weight(1f))
            }

            // Impressions Chart Block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Feed Impressions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barHeights = listOf(40.dp, 60.dp, 35.dp, 80.dp, 55.dp, 95.dp, 75.dp)
                        barHeights.forEachIndexed { i, h ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(h * (factor * 0.15f + 0.85f).coerceIn(0.5f, 1.2f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (i == 5) PulseSecondary else PulsePrimary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("D${i+1}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Text("Audience Demographics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Follower Growth: +$followerGrowth followers this period", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    DemographicProgressRow(label = "18–24 years", progress = 0.54f, color = PulsePrimary)
                    DemographicProgressRow(label = "25–34 years", progress = 0.32f, color = PulseSecondary)
                    DemographicProgressRow(label = "35–44 years", progress = 0.10f, color = PulseTertiary)
                    DemographicProgressRow(label = "Other", progress = 0.04f, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DemographicProgressRow(label: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    count: Int,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            val displayCount = remember(count) {
                if (count >= 1000) String.format("%.1fk", count / 1000f) else count.toString()
            }
            Text(text = displayCount, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = sub, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PulsePrimary)
        }
    }
}

// --- Moderator review panel ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorConsoleScreen(
    moderationQueue: List<PostEntity>,
    onApprove: (String) -> Unit,
    onBlock: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse Mod Review Queue", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("mod_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (moderationQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            color = PulsePrimary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PulsePrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Moderation Queue Clear!",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All active streams are compliant. Automated checks are shielding Pulse Stream in real-time.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("moderator_queue_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(moderationQueue, key = { it.id }) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = post.creatorAvatar,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(post.creatorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(post.creatorId, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "FLAGGED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Reason: ${post.flagReason ?: "Flagged by automatic checks"}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(text = post.caption, fontSize = 13.sp, maxLines = 3)

                                if (post.mediaUrl.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = post.mediaUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onApprove(post.id)
                                            Toast.makeText(context, "Post approved to public feed.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PulsePrimary),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Approve Feed", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    }

                                    Button(
                                        onClick = {
                                            onBlock(post.id)
                                            Toast.makeText(context, "Post blocked and deleted.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Delete & Block", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatColumn(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val displayCount = remember(count) {
            if (count >= 1000) {
                String.format("%.1fk", count / 1000f)
            } else {
                count.toString()
            }
        }

        Text(
            text = displayCount,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileDivider() {
    Divider(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}
