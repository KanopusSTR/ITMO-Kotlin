package chatbot.dsl

import chatbot.api.Keyboard
import chatbot.api.Message
import chatbot.api.MessageId

@ChatBotMarker
class SendMessageClass(var message: Message) {
    var text = String()
    var replyTo: MessageId? = null
    var readyKeyboard: Keyboard? = null
    var keyboard: MutableList<MutableList<Keyboard.Button>> = mutableListOf()
    var oneTime: Boolean = false

    fun row(rowFunction: RowType) {
        val row = RowClassImpl()
        rowFunction(row)
        keyboard.add(row.getKeyboard())
        readyKeyboard = Keyboard.Markup(oneTime, keyboard)
    }

    fun removeKeyboard() {
        readyKeyboard = Keyboard.Remove
    }

    fun withKeyboard(function: () -> Unit) {
        function()
        readyKeyboard = Keyboard.Markup(oneTime, keyboard)
    }

    fun isEmpty(): Boolean {
        return (keyboard.isEmpty() || keyboard[0].isEmpty()) &&
            (readyKeyboard == null || readyKeyboard!!.isEmpty()) &&
            text == "" && replyTo == null
    }

    interface RowClass {
        fun getKeyboard(): MutableList<Keyboard.Button>
        fun button(text: String)
        operator fun String.unaryMinus()
    }

    private class RowClassImpl : RowClass {
        private var keyboard: MutableList<Keyboard.Button> = mutableListOf()

        override fun getKeyboard() = keyboard

        override fun button(text: String) {
            keyboard.add(Keyboard.Button(text))
        }

        override operator fun String.unaryMinus() {
            keyboard.add(Keyboard.Button(this))
        }
    }
}

fun Keyboard.isEmpty(): Boolean {
    if (this is Keyboard.Markup) {
        return keyboard.all { it.isEmpty() }
    }
    return false
}
