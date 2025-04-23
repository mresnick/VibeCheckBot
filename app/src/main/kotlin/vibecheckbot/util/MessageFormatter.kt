package vibecheckbot.util

import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.effectiveName
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageFormatter {
    private val logger = LoggerFactory.getLogger(MessageFormatter::class.java)

    fun formatMessage(message: Message): String? {
        val timestamp = message.timestamp
        val content = message.content
        val author = message.author?.username
        return if (content.isNotBlank()) {
            "$timestamp | $author: $content"
        } else {
            null
        }
    }

    suspend fun formatChannelMessages(channel: TextChannel, limit: Int): String {
        logger.debug("Formatting messages for channel: ${channel.name} with limit: $limit")
        
        val rawMessages = channel.messages.toList()
        logger.debug(rawMessages.joinToString())

        val messages = channel.messages.toList().take(limit)
            .mapNotNull { formatMessage(it) }
        
        return if (messages.isNotEmpty()) {
            logger.debug("Formatted ${messages.size} messages for channel: ${channel.name}")
            """
            Channel: #${channel.name}
            Messages:
            ${messages.joinToString("\n") { "- $it" }}
            
            """.trimIndent()
        } else {
            logger.debug("No messages found for channel: ${channel.name}")
            ""
        }
    }
} 