package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.ImageConfig
import com.example.data.api.RetrofitClient
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class PulseStreamRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val commentDao = db.commentDao()
    private val notificationDao = db.notificationDao()
    private val chatDao = db.chatDao()

    // --- Flow Observers ---
    val allPosts: Flow<List<PostEntity>> = postDao.getAllPosts()
    val trendingPosts: Flow<List<PostEntity>> = postDao.getTrendingPosts()
    val followingPosts: Flow<List<PostEntity>> = postDao.getFollowingPosts()
    val shortsPosts: Flow<List<PostEntity>> = postDao.getShorts()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val allConversations: Flow<List<ChatConversationEntity>> = chatDao.getAllConversations()
    val moderationQueue: Flow<List<PostEntity>> = postDao.getModerationQueue()

    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> = commentDao.getCommentsForPost(postId)
    fun getMessagesForConversation(convoId: String): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForConversation(convoId)

    // --- Direct Messaging Operations ---
    suspend fun sendMessage(peerId: String, text: String, mediaUrl: String? = null) {
        val convoId = "conv_$peerId"
        val peer = userDao.getUserById(peerId) ?: UserEntity(peerId, peerId.removePrefix("@"), "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200", "", 0, 0)
        
        // Ensure conversation entry exists
        val existingConvo = chatDao.getConversationById(convoId)
        if (existingConvo == null) {
            val newConvo = ChatConversationEntity(
                id = convoId,
                peerId = peerId,
                peerName = peer.name,
                peerAvatar = peer.avatarUrl,
                lastMessage = text,
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0
            )
            chatDao.insertConversation(newConvo)
        } else {
            chatDao.insertConversation(existingConvo.copy(
                lastMessage = text,
                lastMessageTimestamp = System.currentTimeMillis()
            ))
        }

        // Insert message
        val msgId = "msg_${UUID.randomUUID()}"
        val userMsg = ChatMessageEntity(
            id = msgId,
            conversationId = convoId,
            senderId = "@you",
            text = text,
            mediaUrl = mediaUrl,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMsg)

        // Trigger simulation reply from the peer asynchronously
        simulatePeerReply(peerId, convoId)
    }

    private suspend fun simulatePeerReply(peerId: String, convoId: String) {
        val replies = mapOf(
            "@tech_guru" to listOf(
                "That sounds fantastic! I'm actually editing my next tech review right now.",
                "Yes, folders are definitely the future. Let's stay tuned!",
                "Check out this cool setup I put together! 🛠️",
                "I completely agree. Appreciate your feedback!"
            ),
            "@sound_wave" to listOf(
                "Thank you! Modular synths have such a unique texture.",
                "I'm streaming again this Friday under the stars. Hope to see you there! 🎹",
                "Awesome! Let's collab on some audio streams sometime.",
                "Let high frequencies bring all the good vibes! ✨"
            ),
            "@style_vision" to listOf(
                "Washed denim and neon represents the ultimate contrast this season! 👖🎨",
                "I'm flying to Milan next week for the launch. Extremely excited!",
                "Fashion is wearable art, no doubt."
            ),
            "@globe_trotter" to listOf(
                "Greece was beautiful! The caldera sunset is simply unmatched.",
                "I'm filming in Tokyo next month! Got any recommendations? ✈️",
                "Travel stories are best shared together!"
            )
        )

        val possibleReplies = replies[peerId] ?: listOf("Hey! Good to connect with you on Pulse Stream.")
        val replyText = possibleReplies.random()

        kotlinx.coroutines.delay(1000) // Simulate typing delay
        
        val replyMsgId = "msg_${UUID.randomUUID()}"
        val peerMsg = ChatMessageEntity(
            id = replyMsgId,
            conversationId = convoId,
            senderId = peerId,
            text = replyText,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(peerMsg)

        // Update last message with peer reply and increment unread
        val convo = chatDao.getConversationById(convoId)
        if (convo != null) {
            chatDao.insertConversation(convo.copy(
                lastMessage = replyText,
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = convo.unreadCount + 1
            ))
        }
    }

    suspend fun clearChatUnreads(convoId: String) {
        chatDao.clearUnreads(convoId)
    }

    // --- Content Moderation Actions ---
    suspend fun reportPost(postId: String, reason: String) {
        postDao.updateModerationStatus(postId, "FLAGGED", reason)
        triggerCustomNotification(
            title = "Post Flagged 🚩",
            message = "Post $postId has been flagged by a user for: $reason.",
            type = "TRENDING"
        )
    }

    suspend fun approvePost(postId: String) {
        postDao.updateModerationStatus(postId, "APPROVED", null)
    }

    suspend fun blockPost(postId: String) {
        postDao.deletePost(postId)
    }

    suspend fun moderateWithGemini(text: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }
        val prompt = """
            You are an automated content moderation AI for Pulse Stream, a creative social media platform.
            Analyze the following post caption for harmful content (hate speech, nudity, extreme violence, harassment, illegal acts).
            If it is harmful, respond with a JSON object containing {"harmful": true, "reason": "<short reason explanation>"}.
            If it is safe, respond with {"harmful": false, "reason": ""}.
            Do not include markdown blocks, write only the raw JSON.
            Caption: "$text"
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            Log.e("PulseStreamRepo", "Gemini moderation failed: ${e.message}")
            null
        }
    }

    // --- Write/Interaction Operations ---
    suspend fun createPost(
        caption: String,
        type: String, // "POST", "SHORT", "VIDEO"
        mediaUrl: String,
        aspectRatio: String,
        category: String
    ) {
        val postId = "post_${UUID.randomUUID()}"
        
        // Automated scanning for sensitive terms
        var initialStatus = "APPROVED"
        var flagReason: String? = null
        
        val lowercase = caption.lowercase()
        val triggerWords = listOf("hate", "violence", "kill", "nude", "naked", "blood", "abuse", "harass", "weapon")
        val containsTrigger = triggerWords.any { lowercase.contains(it) }
        
        if (containsTrigger) {
            initialStatus = "PENDING"
            flagReason = "Automated moderation flagged potential sensitive content (violence/nudity/hate speech)."
        } else {
            // Check via Gemini if available
            val geminiMod = moderateWithGemini(caption)
            if (geminiMod != null && geminiMod.contains("\"harmful\": true")) {
                initialStatus = "PENDING"
                flagReason = "Gemini automated check flagged potential policy violation."
            }
        }

        // Add default random analytics properties for creator portfolio metrics
        val views = (100..1500).random()
        val shares = (10..300).random()

        val newPost = PostEntity(
            id = postId,
            creatorId = "@you",
            creatorName = "You (Pulse Creator)",
            creatorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200", // Placeholder avatar
            type = type,
            caption = caption,
            mediaUrl = mediaUrl,
            aspectRatio = aspectRatio,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            isLiked = false,
            isTrending = false,
            category = category,
            moderationStatus = initialStatus,
            flagReason = flagReason,
            viewsCount = views,
            sharesCount = shares
        )
        postDao.insertPost(newPost)

        if (initialStatus == "PENDING") {
            triggerCustomNotification(
                title = "Post Under Review ⚠️",
                message = "Your post was held for review due to potential sensitive words detected.",
                type = "TRENDING"
            )
        }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean) {
        val delta = if (currentlyLiked) -1 else 1
        postDao.updateLikeStatus(postId, !currentlyLiked, delta)

        // Trigger Notification on liking if it's not by yourself
        if (!currentlyLiked) {
            val notification = NotificationEntity(
                id = "notif_${UUID.randomUUID()}",
                title = "New Like!",
                message = "Someone liked your post.",
                timestamp = System.currentTimeMillis(),
                type = "LIKE",
                postId = postId
            )
            notificationDao.insertNotification(notification)
        }
    }

    suspend fun toggleFollow(userId: String, currentlyFollowing: Boolean) {
        val delta = if (currentlyFollowing) -1 else 1
        userDao.updateFollowingStatus(userId, !currentlyFollowing, delta)

        // Trigger Notification on following
        if (!currentlyFollowing) {
            val notification = NotificationEntity(
                id = "notif_${UUID.randomUUID()}",
                title = "New Follower! 🎉",
                message = "$userId started following you.",
                timestamp = System.currentTimeMillis(),
                type = "FOLLOW"
            )
            notificationDao.insertNotification(notification)
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val comment = CommentEntity(
            id = "comment_${UUID.randomUUID()}",
            postId = postId,
            authorId = "@you",
            authorName = "You",
            authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            text = text,
            timestamp = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)
        postDao.incrementCommentCount(postId)

        // Trigger Notification
        val notification = NotificationEntity(
            id = "notif_${UUID.randomUUID()}",
            title = "New Comment 💬",
            message = "Someone commented: \"$text\"",
            timestamp = System.currentTimeMillis(),
            type = "COMMENT",
            postId = postId
        )
        notificationDao.insertNotification(notification)
    }

    suspend fun triggerCustomNotification(title: String, message: String, type: String) {
        val notification = NotificationEntity(
            id = "notif_${UUID.randomUUID()}",
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        notificationDao.insertNotification(notification)
    }

    suspend fun markNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    // --- Gemini Image Generation Call ---
    suspend fun generatePostImage(prompt: String, aspectRatio: String, useProModel: Boolean): String? {
        val model = if (useProModel) "gemini-3-pro-image-preview" else "gemini-3.1-flash-image-preview"
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("PulseStreamRepo", "Gemini API key is not configured.")
            return null
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            // Extract the generated image data (base64)
            val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData
            inlineData?.data
        } catch (e: Exception) {
            Log.e("PulseStreamRepo", "Error calling Gemini API: ${e.message}", e)
            null
        }
    }

    // --- Pre-population ---
    suspend fun prepopulateIfEmpty() {
        // Check if database already has users
        val existingUsers = userDao.getAllUsers().firstOrNull()
        if (!existingUsers.isNullOrEmpty()) {
            return
        }

        // 1. Insert Sample Creators
        val creators = listOf(
            UserEntity(
                id = "@tech_guru",
                name = "Alex Rivers",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                bio = "Building the future, one line of code at a time. AI Enthusiast & Hardware Reviewer. 📱⚙️",
                followersCount = 14200,
                followingCount = 180,
                isFollowing = false
            ),
            UserEntity(
                id = "@sound_wave",
                name = "Elena Rose",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                bio = "Indie music producer & keyboard synth jammer. Good vibes and high frequencies. 🎹✨",
                followersCount = 9800,
                followingCount = 312,
                isFollowing = true // Followed by default to showcase the 'Following' feed
            ),
            UserEntity(
                id = "@style_vision",
                name = "Leo Carter",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                bio = "Milan-based fashion designer. Sculpting fabrics and exploring neon streetwear. 👔🎨",
                followersCount = 22400,
                followingCount = 89,
                isFollowing = false
            ),
            UserEntity(
                id = "@globe_trotter",
                name = "Sophia Martinez",
                avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
                bio = "Travel filmmaker capturing stories from the hidden corners of the earth. ✈️🎥",
                followersCount = 31500,
                followingCount = 450,
                isFollowing = true // Followed by default
            )
        )
        userDao.insertUsers(creators)

        // 2. Insert Sample Posts, Shorts, and Videos
        val posts = listOf(
            PostEntity(
                id = "post_1",
                creatorId = "@globe_trotter",
                creatorName = "Sophia Martinez",
                creatorAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
                type = "VIDEO",
                caption = "Sunset at Santorini, Greece 🌅 Pure, unadulterated magic. Watching the sun dip below the caldera is an experience that stays with you forever. #travel #vlog #aesthetic",
                mediaUrl = "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=1000",
                aspectRatio = "16:9",
                timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                likesCount = 3450,
                commentsCount = 240,
                isLiked = false,
                isTrending = true,
                category = "Travel"
            ),
            PostEntity(
                id = "post_2",
                creatorId = "@tech_guru",
                creatorName = "Alex Rivers",
                creatorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                type = "POST",
                caption = "Just unboxed the newest folding tablet! The display crease is completely gone and the multitasking gesture support feels extremely snappy. Future is looking foldable! 📱💻 What do you think?",
                mediaUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=1000",
                aspectRatio = "1:1",
                timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                likesCount = 1890,
                commentsCount = 112,
                isLiked = false,
                isTrending = true,
                category = "Tech"
            ),
            PostEntity(
                id = "short_1",
                creatorId = "@sound_wave",
                creatorName = "Elena Rose",
                creatorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                type = "SHORT",
                caption = "Late night modular synth session 🎹 Sending synth vibes into the cosmic ether! #music #producer #shorts",
                mediaUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1000",
                aspectRatio = "9:16",
                timestamp = System.currentTimeMillis() - 10800000, // 3 hours ago
                likesCount = 890,
                commentsCount = 45,
                isLiked = true,
                isTrending = false,
                category = "Music"
            ),
            PostEntity(
                id = "short_2",
                creatorId = "@style_vision",
                creatorName = "Leo Carter",
                creatorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                type = "SHORT",
                caption = "Behind the scenes drafting the Summer Neon Suit collection. Hand stitches are the soul of high fashion! 👔✨ #fashion #design #shorts",
                mediaUrl = "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1000",
                aspectRatio = "9:16",
                timestamp = System.currentTimeMillis() - 14400000, // 4 hours ago
                likesCount = 1420,
                commentsCount = 89,
                isLiked = false,
                isTrending = false,
                category = "Fashion"
            ),
            PostEntity(
                id = "post_3",
                creatorId = "@style_vision",
                creatorName = "Leo Carter",
                creatorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                type = "POST",
                caption = "Styling classic washed denim with hyper-neon details this season. The visual tension is perfect. Wearable art or too bold? Let me know below. 👖🎨",
                mediaUrl = "https://images.unsplash.com/photo-1542272604-787c3835535d?w=1000",
                aspectRatio = "4:3",
                timestamp = System.currentTimeMillis() - 18000000, // 5 hours ago
                likesCount = 2210,
                commentsCount = 156,
                isLiked = false,
                isTrending = true,
                category = "Fashion"
            ),
            PostEntity(
                id = "post_4",
                creatorId = "@sound_wave",
                creatorName = "Elena Rose",
                creatorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                type = "VIDEO",
                caption = "Live acoustic set under the stars. Confessing my feelings through chords. Full video releasing tonight! 🎸🌟 #vlog #livemusic #singersongwriter",
                mediaUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=1000",
                aspectRatio = "16:9",
                timestamp = System.currentTimeMillis() - 21600000, // 6 hours ago
                likesCount = 5400,
                commentsCount = 422,
                isLiked = false,
                isTrending = true,
                category = "Music"
            )
        )
        postDao.insertPosts(posts)

        // 3. Insert Initial Comments
        val comments = listOf(
            CommentEntity(
                id = "c1",
                postId = "post_1",
                authorId = "@travel_junkie",
                authorName = "Markus",
                authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                text = "Greece is on my absolute bucket list! Gorgeous shot!",
                timestamp = System.currentTimeMillis() - 3200000
            ),
            CommentEntity(
                id = "c2",
                postId = "post_1",
                authorId = "@tech_guru",
                authorName = "Alex Rivers",
                authorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                text = "That cinematic drone footage looks stunning. What camera setup is this?",
                timestamp = System.currentTimeMillis() - 2800000
            ),
            CommentEntity(
                id = "c3",
                postId = "post_2",
                authorId = "@gadget_nerd",
                authorName = "Sarah",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
                text = "Is the battery life affected by the folding mechanism? Need an honest review!",
                timestamp = System.currentTimeMillis() - 6800000
            )
        )
        for (c in comments) {
            commentDao.insertComment(c)
        }

        // 4. Insert Initial Notifications
        val notifications = listOf(
            NotificationEntity(
                id = "n1",
                title = "Welcome to Pulse Stream! 🌟",
                message = "Explore amazing trending posts, scroll immersive shorts, follow outstanding creators, and generate stunning AI posts with custom aspect ratios using Gemini!",
                timestamp = System.currentTimeMillis(),
                type = "TRENDING",
                isRead = false
            )
        )
        for (n in notifications) {
            notificationDao.insertNotification(n)
        }

        // 5. Insert Initial Chats and Messages
        val convos = listOf(
            ChatConversationEntity(
                id = "conv_@tech_guru",
                peerId = "@tech_guru",
                peerName = "Alex Rivers",
                peerAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                lastMessage = "Hey! Let me know what you think of that folding tablet unboxing video! 📱",
                lastMessageTimestamp = System.currentTimeMillis() - 7200000,
                unreadCount = 1
            ),
            ChatConversationEntity(
                id = "conv_@sound_wave",
                peerId = "@sound_wave",
                peerName = "Elena Rose",
                peerAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                lastMessage = "That late-night synth session was so much fun. Did you catch it live?",
                lastMessageTimestamp = System.currentTimeMillis() - 10800000,
                unreadCount = 0
            )
        )
        for (c in convos) {
            chatDao.insertConversation(c)
        }

        val m1 = ChatMessageEntity(
            id = "msg_1",
            conversationId = "conv_@tech_guru",
            senderId = "@tech_guru",
            text = "Hey! Let me know what you think of that folding tablet unboxing video! 📱",
            timestamp = System.currentTimeMillis() - 7200000
        )
        chatDao.insertMessage(m1)

        val m2 = ChatMessageEntity(
            id = "msg_2",
            conversationId = "conv_@sound_wave",
            senderId = "@you",
            text = "I loved your modular synth session! Excellent chords.",
            timestamp = System.currentTimeMillis() - 11000000
        )
        val m3 = ChatMessageEntity(
            id = "msg_3",
            conversationId = "conv_@sound_wave",
            senderId = "@sound_wave",
            text = "That late-night synth session was so much fun. Did you catch it live?",
            timestamp = System.currentTimeMillis() - 10800000
        )
        chatDao.insertMessage(m2)
        chatDao.insertMessage(m3)
    }
}
