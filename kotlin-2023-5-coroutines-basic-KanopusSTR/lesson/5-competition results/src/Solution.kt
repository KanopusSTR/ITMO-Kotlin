import kotlinx.coroutines.flow.*

fun Flow<Cutoff>.resultsFlow(): Flow<Results> = scan(Results(emptyMap())) { result, cutoff ->
    Results(result.results + (cutoff.number to cutoff.time))
}.drop(1)
