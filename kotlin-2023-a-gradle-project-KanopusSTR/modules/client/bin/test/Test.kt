import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Test : FunSpec({
    test("first test") {
        getE() shouldBe 2.718281828459045
    }
})
