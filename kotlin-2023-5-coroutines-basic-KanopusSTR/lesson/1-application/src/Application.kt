import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*

fun CoroutineScope.runApplication(
    runUI: suspend () -> Unit,
    runApi: suspend () -> Unit,
) = launch(coroutineContext) {
    launch {
        runUI()
    }

    launch {
        var isSuccess = false
        while (!isSuccess) {
            try {
                runApi()
                isSuccess = true
            } catch (e: CancellationException) {
                isSuccess = true
            } catch (e: Exception) {
                delay(1.seconds)
            }
        }
    }
}
