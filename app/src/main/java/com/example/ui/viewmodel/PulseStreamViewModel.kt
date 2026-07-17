package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.PulseStreamRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PulseStreamViewModel(private val repository: PulseStreamRepository) : ViewModel() {

    // --- State Observables ---
    val allPosts: StateFlow<List<PostEntity>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingPosts: StateFlow<List<PostEntity>> = repository.trendingPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followingPosts: StateFlow<List<PostEntity>> = repository.followingPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsPosts: StateFlow<List<PostEntity>> = repository.shortsPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<ChatConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moderationQueue: StateFlow<List<PostEntity>> = repository.moderationQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationId
        .flatMapLatest { convoId ->
            if (convoId != null) repository.getMessagesForConversation(convoId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Feed & Layout Control ---
    private val _feedType = MutableStateFlow("TRENDING") // "TRENDING" vs "FOLLOWING"
    val feedType: StateFlow<String> = _feedType.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true) // Default to dark mode for Pulse Stream
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // --- Comment Thread Selection ---
    private val _activePostIdForComments = MutableStateFlow<String?>(null)
    val activePostIdForComments: StateFlow<String?> = _activePostIdForComments.asStateFlow()

    val activeComments: StateFlow<List<CommentEntity>> = _activePostIdForComments
        .flatMapLatest { postId ->
            if (postId != null) repository.getCommentsForPost(postId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Image Generator States ---
    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _generatedImageBase64 = MutableStateFlow<String?>(null)
    val generatedImageBase64: StateFlow<String?> = _generatedImageBase64.asStateFlow()

    private val _generatorError = MutableStateFlow<String?>(null)
    val generatorError: StateFlow<String?> = _generatorError.asStateFlow()

    init {
        // Automatically seed the database on initialization
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // --- User Actions ---
    fun setFeedType(type: String) {
        _feedType.value = type
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setActivePostForComments(postId: String?) {
        _activePostIdForComments.value = postId
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(postId, currentlyLiked)
        }
    }

    fun toggleFollow(userId: String, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            repository.toggleFollow(userId, currentlyFollowing)
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(postId, text)
        }
    }

    fun createPost(caption: String, type: String, mediaUrl: String, aspectRatio: String, category: String) {
        viewModelScope.launch {
            repository.createPost(caption, type, mediaUrl, aspectRatio, category)
            // Trigger a push notification for this post upload
            repository.triggerCustomNotification(
                title = "New Post Streamed! 🚀",
                message = "Your new ${type.lowercase()} was posted successfully.",
                type = "TRENDING"
            )
        }
    }

    fun clearGeneratedImage() {
        _generatedImageBase64.value = null
        _generatorError.value = null
    }

    fun generateAIImage(prompt: String, aspectRatio: String, usePro: Boolean) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingImage.value = true
            _generatorError.value = null
            _generatedImageBase64.value = null

            val result = repository.generatePostImage(prompt, aspectRatio, usePro)
            if (result != null) {
                _generatedImageBase64.value = result
            } else {
                _generatorError.value = "Failed to generate image. Please check your Gemini API key in the secrets panel."
            }
            _isGeneratingImage.value = false
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsAsRead()
        }
    }

    fun simulateNotification(title: String, message: String, type: String) {
        viewModelScope.launch {
            repository.triggerCustomNotification(title, message, type)
        }
    }

    // --- Direct Messaging Actions ---
    fun selectConversation(convoId: String?) {
        _activeConversationId.value = convoId
        if (convoId != null) {
            viewModelScope.launch {
                repository.clearChatUnreads(convoId)
            }
        }
    }

    fun sendMessage(peerId: String, text: String, mediaUrl: String? = null) {
        if (text.isBlank() && mediaUrl == null) return
        viewModelScope.launch {
            repository.sendMessage(peerId, text, mediaUrl)
        }
    }

    // --- Content Moderation Actions ---
    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            repository.reportPost(postId, reason)
        }
    }

    fun approvePost(postId: String) {
        viewModelScope.launch {
            repository.approvePost(postId)
        }
    }

    fun blockPost(postId: String) {
        viewModelScope.launch {
            repository.blockPost(postId)
        }
    }
}

class PulseStreamViewModelFactory(private val repository: PulseStreamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PulseStreamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PulseStreamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
