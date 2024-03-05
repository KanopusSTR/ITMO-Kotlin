import java.util.Scanner

fun greet(name: String): String {
    return "Hello, $name!"
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(greet(readlnOrNull() ?: "Anonymous"))
    } else {
        for (arg in args) println(greet(arg))
    }
}
