package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.PostEntity
import com.example.data.database.UserEntity
import com.example.ui.components.CommentsSheet
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSecondary
import com.example.ui.theme.PulseTertiary
import com.example.ui.viewmodel.PulseStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: PulseStreamViewModel,
    modifier: Modifier = Modifier
) {
    val shortsPosts by viewModel.shortsPosts.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val activePostIdForComments by viewModel.activePostIdForComments.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black // Immersive pitch black background for video player screen
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (shortsPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shorts streaming right now.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Vertical scrolling list acting as full screen video swiper
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("shorts_lazy_column")
                ) {
                    itemsIndexed(shortsPosts, key = { _, post -> post.id }) { _, post ->
                        val creator = allUsers.find { it.id == post.creatorId }
                        val isFollowing = creator?.isFollowing ?: false

                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .background(Color.Black)
                        ) {
                            // Immersive Background Post Image
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = "Shorts visual background",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Scrim overlay to make caption readable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // --- RIGHT INTERACTIVE BAR (Avatar, Likes, Comments, Share) ---
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Creator Profile Circle
                                Box(
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    AsyncImage(
                                        model = post.creatorAvatar,
                                        contentDescription = "Creator profile",
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (!isFollowing && post.creatorId != "@you") {
                                        IconButton(
                                            onClick = { viewModel.toggleFollow(post.creatorId, isFollowing) },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(PulsePrimary, shape = CircleShape)
                                                .align(Alignment.BottomCenter)
                                                .border(1.dp, Color.Black, CircleShape)
                                                .testTag("shorts_follow_${post.creatorId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Follow creator",
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Like Action
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.toggleLike(post.id, post.isLiked) },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(44.dp)
                                            .testTag("shorts_like_${post.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like Short",
                                            tint = if (post.isLiked) PulseSecondary else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = post.likesCount.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Comment Action
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.setActivePostForComments(post.id) },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(44.dp)
                                            .testTag("shorts_comment_${post.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ModeComment,
                                            contentDescription = "Comment Short",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = post.commentsCount.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Share Action
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "Link copied to clipboard! 🔗", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Short",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Share",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // --- BOTTOM OVERLAY DETAILS (Creator ID, Caption, Tags) ---
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.75f)
                                    .padding(start = 16.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = post.creatorName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = post.creatorId,
                                        color = PulsePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = post.caption,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Category neon tag
                                Surface(
                                    color = PulseTertiary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, PulseTertiary)
                                ) {
                                    Text(
                                        text = post.category,
                                        color = PulseTertiary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Slide Overlay Comments Dialog ---
    if (activePostIdForComments != null) {
        CommentsSheet(
            comments = activeComments,
            onDismiss = { viewModel.setActivePostForComments(null) },
            onAddComment = { text ->
                viewModel.addComment(activePostIdForComments!!, text)
            }
        )
    }
}
