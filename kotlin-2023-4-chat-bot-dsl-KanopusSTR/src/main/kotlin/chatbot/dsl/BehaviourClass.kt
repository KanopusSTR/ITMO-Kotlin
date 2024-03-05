package chatbot.dsl

import chatbot.api.*

@ChatBotMarker
class BehaviourClass<C : ChatContext?>(
    val client: Client,
    var functions: MutableList<PredProcPair<C>>,
    val contextManager: ChatContextsManager,
) {
    fun onMessage(predicate: PredicateType, messageFunc: MessageProcessor<C>) {
        functions.add(PredProcPair(predicate, messageFunc))
    }

    fun onMessage(messageFunc: MessageProcessor<C>) {
        onMessage({ _ -> true }, messageFunc)
    }

    fun onMessage(text: String, messageFunc: MessageProcessor<C>) {
        onMessage({ message -> message.text == text }, messageFunc)
    }

    fun onCommand(text: String, messageFunc: MessageProcessor<C>) {
        onMessage({ message -> message.text == "/$text" }, messageFunc)
    }

    fun onMessagePrefix(prefix: String, messageFunc: MessageProcessor<C>) {
        onMessage({ message -> message.text.startsWith(prefix) }, messageFunc)
    }

    fun onMessageContains(text: String, messageFunc: MessageProcessor<C>) {
        onMessage({ message -> message.text.contains(text) }, messageFunc)
    }

    inline fun <reified D : ChatContext?> into(behaviourFunc: BehaviourType<D>) {
        myInto<D>(behaviourFunc) { context -> context is D }
    }

    inline infix fun <reified D : ChatContext?> D.into(behaviourFunc: BehaviourType<D>) {
        myInto<D>(behaviourFunc) { context -> context == this@into }
    }

    inline fun <reified D : ChatContext?> myInto(
        behaviourFunc: BehaviourType<D>,
        crossinline predicate: (ChatContext?) -> Boolean,
    ) {
        val localFunctions: MutableList<PredProcPair<D>> = mutableListOf()
        behaviourFunc(BehaviourClass(client, localFunctions, contextManager))
        for (i in localFunctions.indices) {
            val function = localFunctions[i]
            localFunctions[i] = PredProcPair(
                { message ->
                    (function.predicateType)(message) && predicate(contextManager.getContext(message.chatId))
                },
                function.messageProcessor,
            )
        }
        val convertedValue = convert<D, MutableList<PredProcPair<C>>>(localFunctions)
        if (convertedValue != null) {
            functions.addAll(convertedValue)
        }
    }

    inline fun <D : ChatContext?, reified E : MutableList<PredProcPair<C>>>
    convert(predProcPairs: MutableList<PredProcPair<D>>): E? {
        if (predProcPairs is E) {
            return predProcPairs
        }
        return null
    }
}
