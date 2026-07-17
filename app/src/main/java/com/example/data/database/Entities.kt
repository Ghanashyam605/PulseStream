package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // e.g. "@tech_guru"
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val followersCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean = false
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatar: String,
    val type: String, // "POST", "SHORT", "VIDEO"
    val caption: String,
    val mediaUrl: String, // Can be web URL or Base64 image
    val aspectRatio: String = "1:1", // e.g. "1:1", "16:9", "9:16"
    val timestamp: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean = false,
    val isTrending: Boolean = false,
    val category: String = "General",
    val moderationStatus: String = "APPROVED", // "APPROVED", "PENDING", "FLAGGED", "BLOCKED"
    val flagReason: String? = null,
    val viewsCount: Int = 0,
    val sharesCount: Int = 0
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: Long
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String, // "LIKE", "COMMENT", "FOLLOW", "TRENDING"
    val postId: String? = null,
    val isRead: Boolean = false
)

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String,
    val peerId: String,
    val peerName: String,
    val peerAvatar: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val mediaUrl: String? = null,
    val timestamp: Long
)
