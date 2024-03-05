fun sendMessageToClient(
        client: Client?,
        message: String?,
        mailer: Mailer
) {
    if (client != null && client.personalInfo != null) {
        client.personalInfo.let { mailer.sendMessage(it!!.email, message ?: "Hello!") }
    }
}
