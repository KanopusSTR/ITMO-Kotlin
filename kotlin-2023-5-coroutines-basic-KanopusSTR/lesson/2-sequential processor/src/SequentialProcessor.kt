import kotlinx.coroutines.*

class SequentialProcessor(private val handler: (String) -> String) : TaskProcessor {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val threadContext = newSingleThreadContext("MySingleThreadContext")

    override suspend fun process(argument: String): String {
        return runBlocking(threadContext) {
            handler(argument)
        }
    }
}
