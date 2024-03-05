import java.util.NoSuchElementException

sealed interface IntResult {
    data class Ok(val value: Int) : IntResult

    data class Error(val reason: String?) : IntResult

    fun getOrDefault(value: Int): Int {
        return when (this) {
            is Ok -> {
                this.value
            }

            is Error -> {
                value
            }
        }
    }

    fun getOrNull(): Int? {
        return when (this) {
            is Ok -> {
                this.value
            }

            is Error -> {
                null
            }
        }
    }

    fun getStrict(): Int {
        when (this) {
            is Ok -> {
                return this.value
            }

            is Error -> {
                throw NoResultProvided(this.reason)
            }
        }
    }
}

fun safeRun(function: () -> Int): Any {
    return try {
        IntResult.Ok(function())
    } catch (e: Exception) {
        IntResult.Error(e.message)
    }
}

class NoResultProvided(message: String?) : NoSuchElementException(message)
