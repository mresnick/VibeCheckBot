package vibecheckbot

import com.aallam.openai.client.OpenAI
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import vibecheckbot.util.MessageFormatter
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction

class VibeCheckBot(
    private val discordToken: String,
    private val openAIToken: String,
    private val channelMessageLimit: Int = 20,
    private val serverMessageLimit: Int = 10
) {
    private val logger = LoggerFactory.getLogger(VibeCheckBot::class.java)
    private lateinit var kord: Kord
    private val openAI = OpenAI(openAIToken)
    private val vibeChecker = VibeChecker(openAI)
    private val messageFormatter = MessageFormatter()
    private val maxDiscordMessageLength = 2000

    private suspend fun sendLongMessage(messageChannelBehavior: MessageChannelBehavior, content: String) {
        if (content.length <= maxDiscordMessageLength) {
            messageChannelBehavior.createMessage { this.content = content }
            return
        }

        // Split the content into chunks of maxDiscordMessageLength
        val chunks = content.chunked(maxDiscordMessageLength)
        chunks.forEach { chunk ->
            messageChannelBehavior.createMessage { this.content = chunk }
        }
    }

    private suspend fun getChannelMessages(channel: TextChannel, limit: Int): List<String> {
        return try { 
            channel.getMessagesBefore(channel.lastMessageId!!, limit)
            .toList()
            .filter { message -> message.author != null }
            .mapNotNull { message -> messageFormatter.formatMessage(message) }
        } catch (e: Exception) {
            logger.error("Error getting messages from channel: ${channel.name}", e)
            emptyList()
        }
    }

    private suspend fun checkChannelVibe(channel: TextChannel, response: DeferredEphemeralMessageInteractionResponseBehavior) {
        logger.debug("Channel vibe check requested in channel: ${channel.name}")

        val formattedMessages = getChannelMessages(channel, channelMessageLimit)

        if (formattedMessages.isEmpty()) {
            logger.info("No messages found to analyze in channel: ${channel.name}")
            response.respond {
                content = "No messages found to analyze in this channel!"
            }
            return
        }

        val result = vibeChecker.checkChannelVibe(formattedMessages.joinToString("\n"))
        logger.debug("Channel vibe check completed for channel: ${channel.name}")
        channel.createMessage {
            content = "Channel Vibe Check Results:\n$result"
        }
        response.delete()
    }

    private suspend fun checkServerVibe(guildInteraction: GuildChatInputCommandInteraction, response: DeferredEphemeralMessageInteractionResponseBehavior) {
        logger.debug("Server vibe check requested in guild: ${guildInteraction.guildId}")

        val formattedMessage: String = guildInteraction.guild.channels.toList()
            .filter{ it is TextChannel }
            .map { channel -> 
                val channelName = (channel as TextChannel).name
                logger.debug("Getting messages from $channelName")
                val messages = getChannelMessages(channel, serverMessageLimit)
                logger.debug("Messages: $messages")
                if (messages.isNotEmpty()) {
                    "Server Vibe Check\n\nChannel: #$channelName\n${messages.joinToString("\n")}"
                } else null
            }
            .filterNotNull()
            .joinToString("\n\n")
        
        logger.debug("Formatted message: $formattedMessage")

        if (formattedMessage.isEmpty()) {
            logger.info("No messages found to analyze in guild: ${guildInteraction.guildId}")
            response.respond {
                content = "No messages found to analyze in the server!"
            }
            return
        }

        val result = vibeChecker.checkServerVibe(formattedMessage)
        logger.debug("Server vibe check completed for guild: ${guildInteraction.guildId}")
        sendLongMessage(guildInteraction.channel, "Server Vibe Check Results:\n$result")
        response.delete()
    }

    suspend fun start() {
        logger.info("Starting VibeCheckBot...")
        
        kord = Kord(discordToken)
        
        // Register slash commands
        kord.createGlobalChatInputCommand("vibecheck", "Check the vibe of this server or channel") {
            subCommand("channel", "Check the vibe of the current channel")
            subCommand("server", "Check the vibe of the entire server")
        }
        logger.debug("Slash commands registered successfully")

        // Handle slash commands
        kord.on<ChatInputCommandInteractionCreateEvent> {
            val command = interaction.command as SubCommand
            when (command.rootName) {
                "vibecheck" -> {
                    when (command.name) {
                        "channel" -> {
                            val channel = interaction.channel.asChannel() as? TextChannel
                            if (channel == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in text channels!"
                                }
                                return@on
                            }

                            val response = interaction.deferEphemeralResponse()
                            response.respond {
                                content = "Checking the vibe of this channel..."
                            }
                            checkChannelVibe(channel, response)
                        }
                        "server" -> {
                            val guildInteraction = interaction as? GuildChatInputCommandInteraction
                            if (guildInteraction == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in a server!"
                                }
                                return@on
                            }

                            val response = interaction.deferEphemeralResponse()
                            response.respond {
                                content = "Checking the vibe of the entire server... This might take a moment!"
                            }
                            checkServerVibe(guildInteraction, response)
                        }
                        else -> {
                            interaction.deferEphemeralResponse().respond {
                                content = "Unknown subcommand!"
                            }
                        }
                    }
                }
                else -> {
                    interaction.deferEphemeralResponse().respond {
                        content = "Unknown command!"
                    }
                }
            }
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
        logger.info("VibeCheckBot started successfully")
    }

    suspend fun stop() {
        logger.info("Stopping VibeCheckBot...")
        if (::kord.isInitialized) {
            kord.logout()
        }
        logger.info("VibeCheckBot stopped successfully")
    }
}

fun main() = runBlocking {
    val logger = LoggerFactory.getLogger("VibeCheckBotMain")
    
    val discordToken = System.getenv("DISCORD_TOKEN") ?: throw IllegalStateException("DISCORD_TOKEN environment variable not set")
    val openAIToken = System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set")
    
    val channelMessageLimit = System.getenv("CHANNEL_MESSAGE_LIMIT")?.toIntOrNull() ?: 20
    val serverMessageLimit = System.getenv("SERVER_MESSAGE_LIMIT")?.toIntOrNull() ?: 10
    
    logger.info("Initializing VibeCheckBot with channel message limit: $channelMessageLimit, server message limit: $serverMessageLimit")
    
    val bot = VibeCheckBot(
        discordToken,
        openAIToken,
        channelMessageLimit,
        serverMessageLimit
    )
    
    try {
        bot.start()
    } catch (e: Exception) {
        logger.error("Error starting bot: ${e.message}", e)
        bot.stop()
    }
} 