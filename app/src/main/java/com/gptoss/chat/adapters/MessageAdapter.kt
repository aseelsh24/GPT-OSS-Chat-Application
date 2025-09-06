package com.gptoss.chat.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gptoss.chat.R
import com.gptoss.chat.databinding.ItemMessageBinding
import com.gptoss.chat.models.Message
import com.gptoss.chat.utils.MessageType

class MessageAdapter(private val messages: List<Message>) : 
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(private val binding: ItemMessageBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textViewMessage.text = message.text
            binding.textViewTime.text = message.timestamp

            val context = binding.root.context
            val containerLayoutParams = binding.messageContainer.layoutParams as LinearLayout.LayoutParams

            when (message.type) {
                MessageType.USER -> {
                    binding.messageContainer.setBackgroundResource(R.drawable.message_background_user)
                    binding.textViewMessage.setTextColor(
                        ContextCompat.getColor(context, R.color.message_text_color)
                    )
                    binding.textViewTime.setTextColor(
                        ContextCompat.getColor(context, R.color.time_text_color)
                    )
                    
                    containerLayoutParams.gravity = Gravity.END
                    containerLayoutParams.setMargins(
                        context.resources.getDimensionPixelSize(R.dimen.message_margin_far),
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.message_margin_near),
                        0
                    )
                }
                
                MessageType.BOT -> {
                    binding.messageContainer.setBackgroundResource(R.drawable.message_background_bot)
                    binding.textViewMessage.setTextColor(
                        ContextCompat.getColor(context, R.color.bot_message_text)
                    )
                    binding.textViewTime.setTextColor(
                        ContextCompat.getColor(context, R.color.bot_message_text)
                    )
                    
                    containerLayoutParams.gravity = Gravity.START
                    containerLayoutParams.setMargins(
                        context.resources.getDimensionPixelSize(R.dimen.message_margin_near),
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.message_margin_far),
                        0
                    )
                }
            }
            
            binding.messageContainer.layoutParams = containerLayoutParams
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
    
    fun addMessage(message: Message) {
        if (messages is MutableList) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }
    
    fun getLastMessage(): Message? {
        return if (messages.isNotEmpty()) messages.last() else null
    }
}
