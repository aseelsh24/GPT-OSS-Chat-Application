package com.gptoss.chat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gptoss.chat.adapters.MessageAdapter
import com.gptoss.chat.api.ApiException
import com.gptoss.chat.api.ApiManager
import com.gptoss.chat.api.NetworkException
import com.gptoss.chat.databinding.ActivityMainBinding
import com.gptoss.chat.models.Message
import com.gptoss.chat.utils.MessageUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var apiManager: ApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiManager = ApiManager.getInstance(this)
        setupRecyclerView()
        setupClickListeners()
        addWelcomeMessage()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        binding.recyclerViewMessages.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    scrollToPosition(messageAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (MessageUtils.isValidMessage(messageText)) {
                sendMessage(MessageUtils.sanitizeMessage(messageText))
                binding.editTextMessage.text.clear()
            }
        }

        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                binding.buttonSend.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = MessageUtils.createBotMessage(
            "Hello! I'm GPT-OSS, your AI assistant. How can I help you today?"
        )
        addMessageToChat(welcomeMessage)
    }

    private fun sendMessage(messageText: String) {
        val userMessage = MessageUtils.createUserMessage(messageText)
        addMessageToChat(userMessage)

        setLoadingState(true)

        lifecycleScope.launch {
            apiManager.sendChatMessage(messageText)
                .onSuccess { response ->
                    val botMessage = MessageUtils.createBotMessage(response)
                    addMessageToChat(botMessage)
                }
                .onFailure { exception ->
                    handleApiError(exception)
                }
            
            setLoadingState(false)
        }
    }

    private fun handleApiError(exception: Throwable) {
        val errorMessage = when (exception) {
            is NetworkException -> {
                Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
                "Sorry, I'm having trouble connecting to the server. Please check your internet connection and try again."
            }
            is ApiException -> {
                Toast.makeText(this, getString(R.string.error_api), Toast.LENGTH_SHORT).show()
                "Sorry, I encountered an API error (Code: ${exception.code}). Please try again in a moment."
            }
            else -> {
                Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
                "Sorry, something unexpected happened. Please try again."
            }
        }
        
        val botErrorMessage = MessageUtils.createBotMessage(errorMessage)
        addMessageToChat(botErrorMessage)
    }

    private fun addMessageToChat(message: Message) {
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.buttonSend.isEnabled = !isLoading
        binding.buttonSend.text = if (isLoading) getString(R.string.sending) else getString(R.string.send)
        binding.editTextMessage.isEnabled = !isLoading
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.recyclerViewMessages.post {
                binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
}
