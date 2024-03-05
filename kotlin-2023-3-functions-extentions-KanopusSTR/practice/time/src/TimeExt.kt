val Int.milliseconds: Time
    get() = toLong().milliseconds

val Long.milliseconds: Time
    get() = Time(toLong() / 1000, (this % 1000).toInt())

val Int.seconds: Time
    get() = toLong().seconds

val Long.seconds: Time
    get() = Time(toLong(), 0)

val Int.minutes: Time
    get() = toLong().minutes

val Long.minutes: Time
    get() = Time(toLong() * 60, 0)

val Int.hours: Time
    get() = toLong().hours

val Long.hours: Time
    get() = (this * 60).minutes

operator fun Time.plus(other: Time): Time {
    val seconds = seconds + other.seconds + (milliseconds + other.milliseconds) / 1000
    val milliseconds = (milliseconds + other.milliseconds) % 1000
    return Time(seconds, milliseconds)
}


operator fun Time.minus(other: Time): Time {
    var curMS = milliseconds - other.milliseconds
    var curS = seconds - other.seconds
    if (milliseconds < other.milliseconds) {
        curMS += 1000
        curS -= 1
    }
    return Time(curS, curMS)
}

operator fun Time.times(other: Int): Time {
    val seconds = seconds * other + (milliseconds.toLong() * other) / 1000
    val milliseconds = ((milliseconds.toLong() * other) % 1000).toInt()
    return Time(seconds, milliseconds)
}