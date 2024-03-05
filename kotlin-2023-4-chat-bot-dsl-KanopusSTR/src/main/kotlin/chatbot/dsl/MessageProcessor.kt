package chatbot.dsl

import chatbot.api.*

@DslMarker
annotation class ChatBotMarker

@ChatBotMarker
class MessageProcessorContext<C : ChatContext?>(
    val message: Message,
    val client: Client,
    val context: C,
    val setContext: (c: ChatContext?) -> Unit,
) {
    fun sendMessage(chatId: ChatId, function: SendMessageType) {
        val smc = SendMessageClass(this.message)
        function(smc)
        if (!smc.isEmpty()) {
            client.sendMessage(chatId, smc.text, smc.readyKeyboard, replyMessageId = smc.replyTo)
        }
    }
}

typealias MessageProcessor<C> = MessageProcessorContext<C>.() -> Unit
typealias BehaviourType<C> = BehaviourClass<C>.() -> Unit
typealias SendMessageType = SendMessageClass.() -> Unit
typealias RowType = SendMessageClass.RowClass.() -> Unit
typealias PredicateType = ChatBot.(Message) -> Boolean
