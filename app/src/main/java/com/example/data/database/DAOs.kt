package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isFollowing = :isFollowing, followersCount = followersCount + :followerDelta WHERE id = :userId")
    suspend fun updateFollowingStatus(userId: String, isFollowing: Boolean, followerDelta: Int)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE moderationStatus = 'APPROVED' ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE moderationStatus = 'APPROVED' AND isTrending = 1 ORDER BY likesCount DESC, timestamp DESC")
    fun getTrendingPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE moderationStatus = 'APPROVED' AND creatorId IN (SELECT id FROM users WHERE isFollowing = 1) ORDER BY timestamp DESC")
    fun getFollowingPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE moderationStatus = 'APPROVED' AND type = 'SHORT' ORDER BY timestamp DESC")
    fun getShorts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE moderationStatus != 'APPROVED' ORDER BY timestamp DESC")
    fun getModerationQueue(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE posts SET isLiked = :isLiked, likesCount = likesCount + :likeDelta WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, isLiked: Boolean, likeDelta: Int)

    @Query("UPDATE posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: String)

    @Query("UPDATE posts SET moderationStatus = :status, flagReason = :reason WHERE id = :postId")
    suspend fun updateModerationStatus(postId: String, status: String, reason: String?)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_conversations SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp, unreadCount = unreadCount + :unreadDelta WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, lastMessage: String, timestamp: Long, unreadDelta: Int)

    @Query("UPDATE chat_conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnreads(conversationId: String)

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ChatConversationEntity?
}
