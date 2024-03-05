package chatbot.dsl

import chatbot.api.*

class PredProcPair<C : ChatContext?>(val predicateType: PredicateType, val messageProcessor: MessageProcessor<C>)

@ChatBotMarker
class MyChatBot(private val client: Client) : ChatBot {
    override var logLevel: LogLevel = LogLevel.ERROR

    private var contextsManager: ChatContextsManager = CustomChatContextManager()
    private val functions: MutableList<PredProcPair<ChatContext?>> = mutableListOf()

    override fun processMessages(message: Message) {
        val context = contextsManager.getContext(message.chatId)
        val mpc = MessageProcessorContext(message, client, context) { chatContext ->
            contextsManager.setContext(message.chatId, chatContext)
        }

        val text = if (message.text.isNotEmpty() && message.text.first() == '/') {
            message.text.split(' ').first()
        } else {
            message.text
        }

        for (pair in functions) {
            if (pair.predicateType(this, Message(message.id, message.chatId, text, message.replyMessageId))) {
                pair.messageProcessor(mpc)
                break
            }
        }
    }

    fun use(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    fun use(contextsManager: ChatContextsManager) {
        this.contextsManager = contextsManager
    }

    operator fun LogLevel.unaryPlus() {
        logLevel = this
    }

    fun behaviour(function: BehaviourType<ChatContext?>) {
        function(BehaviourClass(client, functions, contextsManager))
    }
}

fun chatBot(client: Client, function: MyChatBot.() -> Unit): ChatBot {
    val myChatBot = MyChatBot(client)
    myChatBot.function()
    return myChatBot
}
