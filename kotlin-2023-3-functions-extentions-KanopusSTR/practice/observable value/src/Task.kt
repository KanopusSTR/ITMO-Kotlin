interface Value<S> {
    fun observe(callable: (S) -> Unit): Describer
}

fun interface Describer {
    fun cancel()
}

class MutableValue<S>(initial: S) : Value<S> {

    private val functions = mutableSetOf<(S) -> Unit>()

    var value = initial
        set(value) {
            field = value
            functions.forEach {
                runCatching {
                    it(value)
                }.onFailure {
                    printError(it)
                }
            }
        }

    override fun observe(callable: (S) -> Unit): Describer {
        runCatching {
            callable(value)
        }.onFailure {
             printError(it)
        }
        functions.add(callable)
        return Describer { functions.remove(callable) }
    }

    private fun printError(e: Throwable) {
        println("an error occurred while passing the current value to the passed function: " + e.message)
    }
}
