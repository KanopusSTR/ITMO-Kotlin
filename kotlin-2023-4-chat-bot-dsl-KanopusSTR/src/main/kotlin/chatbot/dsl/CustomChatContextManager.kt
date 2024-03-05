package chatbot.dsl

import chatbot.api.ChatContext
import chatbot.api.ChatContextsManager
import chatbot.api.ChatId

class CustomChatContextManager : ChatContextsManager {
    private var contexts: MutableMap<ChatId, ChatContext?> = HashMap()

    override fun getContext(chatId: ChatId): ChatContext? {
        return contexts[chatId]
    }

    override fun setContext(chatId: ChatId, newState: ChatContext?) {
        contexts[chatId] = newState
    }
}
