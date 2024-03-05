import kotlin.math.sqrt


fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    for (i in 2..sqrt(n.toDouble()).toInt()) {
        if (n % i == 0) {
            return false
        }
    }
    return true
}

fun piFunction(x: Double): Int {
    var size = 0
    for (i in 2..x.toInt()) {
        if (isPrime(i)) size++
    }
    return size
}
