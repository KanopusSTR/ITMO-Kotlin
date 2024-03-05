class IntMatrix(rows: Int = 0, columns: Int = 0) {

    val rows: Int
    val columns: Int
    private val array: IntArray

    init {
        this.rows = rows
        this.columns = columns
        if (rows < 0 || columns < 0) {
            throw IllegalArgumentException("Amount of rows and columns can't be negative")
        }
        array = IntArray(rows * columns)
    }

    operator fun get(row: Int, column: Int): Int {
        checkSize(row, column)
        return array[getIndex(row, column)]
    }

    operator fun set(row: Int, column: Int, value: Int) {
        checkSize(row, column)
        array[getIndex(row, column)] = value
    }

    private fun getIndex(row: Int, column: Int): Int {
        return row * columns + column
    }

    private fun checkSize(row: Int, column: Int) {
        if (row >= rows || column >= columns || row < 0 || column < 0) {
            throw IllegalArgumentException("You should write row and column numbers that in matrix range")
        }
    }
}
