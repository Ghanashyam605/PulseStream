package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.database.ChatConversationEntity
import com.example.data.database.ChatMessageEntity
import com.example.data.database.UserEntity
import com.example.ui.theme.PulsePrimary
import com.example.ui.theme.PulseSecondary
import com.example.ui.viewmodel.PulseStreamViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: PulseStreamViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.allConversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val currentConvo = conversations.find { it.id == activeConversationId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentConvo == null) {
                        Text("Pulse Inbox", fontWeight = FontWeight.Black)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = currentConvo.peerAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentConvo.peerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentConvo.peerId,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeConversationId != null) {
                                viewModel.selectConversation(null)
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (activeConversationId == null) {
                // INBOX LIST VIEW
                Column(modifier = Modifier.fillMaxSize()) {
                    // Suggested Creators Row
                    SuggestedCreatorsSection(
                        users = allUsers.filter { it.id != "@you" },
                        onCreatorClick = { creator ->
                            viewModel.selectConversation("conv_${creator.id}")
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.8.dp)

                    // Inbox Conversations List
                    if (conversations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Your inbox is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start a direct message with a creator above!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("conversation_list"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(conversations, key = { it.id }) { convo ->
                                ConversationListItem(
                                    convo = convo,
                                    onClick = {
                                        viewModel.selectConversation(convo.id)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ACTIVE CONVERSATION THREAD
                if (currentConvo != null) {
                    ChatThreadSection(
                        messages = activeMessages,
                        peerId = currentConvo.peerId,
                        onSendMessage = { text, media ->
                            viewModel.sendMessage(currentConvo.peerId, text, media)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedCreatorsSection(
    users: List<UserEntity>,
    onCreatorClick: (UserEntity) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Suggested Creators",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(users, key = { it.id }) { user ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onCreatorClick(user) }
                        .padding(vertical = 4.dp)
                        .testTag("suggested_chat_${user.id}")
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = user.name.split(" ").firstOrNull() ?: user.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationListItem(
    convo: ChatConversationEntity,
    onClick: () -> Unit
) {
    val timeStr = remember(convo.lastMessageTimestamp) {
        val diff = System.currentTimeMillis() - convo.lastMessageTimestamp
        when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_item_${convo.peerId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = convo.peerAvatar,
            contentDescription = convo.peerName,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = convo.peerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = convo.lastMessage,
                fontSize = 13.sp,
                color = if (convo.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (convo.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (convo.unreadCount > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = convo.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChatThreadSection(
    messages: List<ChatMessageEntity>,
    peerId: String,
    onSendMessage: (String, String?) -> Unit,
    viewModel: PulseStreamViewModel
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var typedText by remember { mutableStateOf("") }
    
    // Media attachment states
    var isMediaPaneOpen by remember { mutableStateOf(false) }
    var attachedUrl by remember { mutableStateOf("") }
    
    // AI Generation states for DMs
    var aiPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("chat_messages_column"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == "@you"
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        tonalElevation = if (isMe) 0.dp else 1.dp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (msg.mediaUrl != null) {
                                AsyncImage(
                                    model = msg.mediaUrl,
                                    contentDescription = "DM attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.05f)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (msg.text.isNotEmpty()) {
                                Text(text = msg.text, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Expandable Media attachment Pane
        AnimatedVisibility(visible = isMediaPaneOpen) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Send Media Attachment",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { isMediaPaneOpen = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Media")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option A: Custom Image URL
                    OutlinedTextField(
                        value = attachedUrl,
                        onValueChange = { attachedUrl = it },
                        label = { Text("Enter Web Image URL") },
                        placeholder = { Text("https://images.unsplash.com/photo-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PulsePrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: Generate AI Image for DM
                    Text(
                        text = "Or Generate with Pulse AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("Type prompt e.g. 'A futuristic city'") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (aiPrompt.isNotBlank()) {
                                    isGenerating = true
                                    coroutineScope.launch {
                                        // Use repository to generate image
                                        val result = viewModel.generateAIImage(aiPrompt, "1:1", false)
                                        // Wait a moment for generation
                                        kotlinx.coroutines.delay(2000)
                                        val base64 = viewModel.generatedImageBase64.value
                                        if (base64 != null) {
                                            attachedUrl = "data:image/jpeg;base64,$base64"
                                            Toast.makeText(context, "AI image generated! Ready to attach.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not generate. Enter URL instead.", Toast.LENGTH_SHORT).show()
                                        }
                                        isGenerating = false
                                        viewModel.clearGeneratedImage()
                                    }
                                }
                            },
                            enabled = !isGenerating && aiPrompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseSecondary
                            ),
                            modifier = Modifier.height(54.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI")
                            }
                        }
                    }
                }
            }
        }

        // Message Input Toolbar
        Surface(
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Media Button
                IconButton(
                    onClick = { isMediaPaneOpen = !isMediaPaneOpen },
                    modifier = Modifier.testTag("attach_media_btn")
                ) {
                    Icon(
                        imageVector = if (isMediaPaneOpen) Icons.Default.Close else Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach media",
                        tint = if (isMediaPaneOpen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Text Input
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Send a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = {
                        val text = typedText.trim()
                        val media = if (attachedUrl.isNotEmpty()) attachedUrl else null
                        if (text.isNotEmpty() || media != null) {
                            onSendMessage(text, media)
                            typedText = ""
                            attachedUrl = ""
                            aiPrompt = ""
                            isMediaPaneOpen = false
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(40.dp)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
