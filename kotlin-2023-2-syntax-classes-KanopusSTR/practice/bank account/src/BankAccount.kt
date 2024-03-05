class BankAccount(amount: Int) {

    var balance = 0
        private set(value) {
            val oldBalance = balance
            field = value
            logTransaction(oldBalance, balance)
        }

    init {
        if (amount <= 0) {
            throw IllegalArgumentException("You should put positive amount of money")
        }
        balance = amount
    }

    fun deposit(money: Int) {
        if (money <= 0) {
            throw IllegalArgumentException("You should deposit positive amount of money")
        }
        balance += money
    }

    fun withdraw(money: Int) {
        if (money !in 1..<balance) {
            throw IllegalArgumentException("You can't withdraw amount of money less than 1 or more than current balance")
        }
        balance -= money
    }
}
