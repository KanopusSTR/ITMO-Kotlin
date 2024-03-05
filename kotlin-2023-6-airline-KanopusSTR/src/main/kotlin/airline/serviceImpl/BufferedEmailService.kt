package airline.serviceImpl

import airline.api.Mail
import airline.service.EmailService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel

class BufferedEmailService(private val emailService: EmailService) : EmailService {
    private val channel: Channel<Mail> = Channel()

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run() {
        while (!channel.isClosedForReceive) {
            val mail = channel.receive()
            emailService.send(mail.to, mail.text)
        }
    }

    override suspend fun send(to: String, text: String) {
        channel.send(Mail(to, text))
    }
}
