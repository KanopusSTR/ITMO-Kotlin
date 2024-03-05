import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ParallelEvaluator {
    suspend fun run(task: Task, n: Int, context: CoroutineContext) = coroutineScope {
        for (i in 0..<n) {
            launch(context) {
                try {
                    task.run(i)
                } catch (e: Exception) {
                    throw TaskEvaluationException(e)
                }
            }
        }
    }
}
