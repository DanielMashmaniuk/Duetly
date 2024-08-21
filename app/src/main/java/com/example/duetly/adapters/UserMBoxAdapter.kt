package com.example.duetly.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.fragments.Playlist
import com.example.duetly.R
import com.example.duetly.activities.getDrawableFromResourcePath
import com.example.duetly.models.User
import com.example.duetly.models.UserMessage

class UserMBoxAdapter(
    private var messagesList: MutableList<UserMessage>,
    private val onAcceptListener:(Pair<UserMessage,Int>) -> Unit,
    private val onRefuseListener:(Pair<UserMessage,Int>) -> Unit

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_FRIEND_REQ = 1
        private const val TYPE_SIMPLE_MESSAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messagesList[position]
        println("${messagesList.size}")
        return if (message.type == "Friend request") TYPE_FRIEND_REQ else TYPE_SIMPLE_MESSAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FRIEND_REQ) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friends_request_layout, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_simple_message, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messagesList[position]
        if (holder is SentMessageViewHolder) {
            val send: LinearLayout = holder.itemView.findViewById(R.id.accept)
            val cancelB: LinearLayout = holder.itemView.findViewById(R.id.refuse)
            send.setOnClickListener {
                onAcceptListener(Pair(message, position))
            }
            cancelB.setOnClickListener {
                onRefuseListener(Pair(message, position))
            }
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messagesList.size

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.messageText)
        private val send: LinearLayout = itemView.findViewById(R.id.accept)
        private val cancelB: LinearLayout = itemView.findViewById(R.id.refuse)
        private val showInfoIcon: ImageView = itemView.findViewById(R.id.showInfoIcon)
        private val messageInfo: View = itemView.findViewById(R.id.messageInfo)
        private var isExpanded: Boolean = false

        init {
            // Спочатку приховуємо messageInfo
            messageInfo.visibility = View.GONE

            showInfoIcon.setOnClickListener {
                toggleMessageInfoVisibility()
            }

        }

        fun bind(message: UserMessage) {
            textMessage.text = message.text

        }

        private fun toggleMessageInfoVisibility() {
            if (isExpanded) {
                // Згорнути messageInfo
                collapseView(messageInfo)
                setFlipUpAnimation(showInfoIcon)
            } else {
                // Розгорнути messageInfo
                expandView(messageInfo)
                setFlipDownAnimation(showInfoIcon)
            }
            isExpanded = !isExpanded
        }
        private fun setFlipDownAnimation(imageView: ImageView, duration: Long = 300) {
            val flipAnimator = ObjectAnimator.ofFloat(imageView, "rotationX", 0f, 180f)
            flipAnimator.duration = duration
            flipAnimator.start()
        }
        private fun setFlipUpAnimation(imageView: ImageView, duration: Long = 300) {
            val flipAnimator = ObjectAnimator.ofFloat(imageView, "rotationX", 180f, 0f)
            flipAnimator.duration = duration
            flipAnimator.start()
        }

        private fun expandView(view: View) {
            // Встановлюємо початкову прозорість на 0 (невидимий)
            view.alpha = 0f
            view.layoutParams.height = 0
            view.visibility = View.VISIBLE

            // Визначаємо кінцеву висоту в dp (наприклад, 65dp)
            val targetHeight = (65 * view.context.resources.displayMetrics.density).toInt()

            // Встановлюємо початкову висоту 0
            val initialHeight = view.height

            // Анімація розгортання висоти
            val heightAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
                duration = 300
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    view.layoutParams.height = animation.animatedValue as Int
                    view.requestLayout()
                }
            }

            // Анімація прозорості
            val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = LinearInterpolator()
            }

            // Запуск анімації висоти та прозорості одночасно
            AnimatorSet().apply {
                playTogether(heightAnimator, alphaAnimator)
                start()
            }
        }





        private fun collapseView(view: View) {
            val initialHeight = view.measuredHeight

            val valueAnimator = ValueAnimator.ofInt(initialHeight, 0)
            valueAnimator.addUpdateListener { animation ->
                view.layoutParams.height = animation.animatedValue as Int
                view.requestLayout()
            }
            valueAnimator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            valueAnimator.duration = 300
            valueAnimator.interpolator = AccelerateDecelerateInterpolator()
            valueAnimator.start()
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessageReceived: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: UserMessage) {
            textMessageReceived.text = message.text
        }
    }
    fun addMessage(message: UserMessage) {
        // Перевіряємо, чи вже існує таке повідомлення в списку
        if (messagesList.none { it.id == message.id }) {
            messagesList.add(message)
            notifyItemInserted(messagesList.size - 1)
        } else {
            // Якщо повідомлення вже існує, можна ігнорувати його або оновити існуюче
        }
    }

}

