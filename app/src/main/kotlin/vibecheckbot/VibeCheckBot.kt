package vibecheckbot

import com.aallam.openai.client.OpenAI
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import vibecheckbot.util.MessageFormatter
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction

class VibeCheckBot(
    private val token: String,
    private val openAIToken: String,
    private val channelMessageLimit: Int = 100,
    private val serverMessageLimit: Int = 20
) {
    private val logger = LoggerFactory.getLogger(VibeCheckBot::class.java)
    private lateinit var kord: Kord
    private val openAI = OpenAI(openAIToken)
    private val vibeChecker = VibeChecker(openAI)
    private val messageFormatter = MessageFormatter()
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    suspend fun start() {
        logger.info("Starting VibeCheckBot...")
        
        kord = Kord(token)
        
        // Register slash commands
        kord.createGlobalChatInputCommand("vibecheck", "Check the vibe of this server or channel") {
            subCommand("channel", "Check the vibe of the current channel") {
                description = "Analyzes the last $channelMessageLimit messages in this channel"
            }
            subCommand("server", "Check the vibe of the entire server") {
                description = "Analyzes $serverMessageLimit messages from each channel in the server"
            }
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

                            logger.debug("Channel vibe check requested in channel: ${channel.name}")
                            val response = interaction.deferPublicResponse()
                            response.respond {
                                content = "🔮 Checking the vibe of this channel (last $channelMessageLimit messages)..."
                            }

                            val formattedMessages = mutableListOf<String>()
                            channel.getMessagesBefore(Snowflake(Instant.now().toEpochMilli()), channelMessageLimit)
                                .collect { message -> 
                                    messageFormatter.formatMessage(message)?.let { formattedMessages.add(it) }
                                }

                            if (formattedMessages.isEmpty()) {
                                logger.info("No messages found to analyze in channel: ${channel.name}")
                                response.respond {
                                    content = "No messages found to analyze in this channel!"
                                }
                                return@on
                            }

                            val result = vibeChecker.checkVibe(formattedMessages.joinToString("\n"))
                            logger.debug("Channel vibe check completed for channel: ${channel.name}")
                            response.respond {
                                content = "✨ Channel Vibe Check Results:\n$result"
                            }
                        }
                        "server" -> {
                            val guildInteraction = interaction as? GuildChatInputCommandInteraction
                            if (guildInteraction == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in a server!"
                                }
                                return@on
                            }

                            logger.debug("Server vibe check requested in guild: ${guildInteraction.guildId}")
                            val response = interaction.deferPublicResponse()
                            response.respond {
                                content = "🔮 Checking the vibe of the entire server ($serverMessageLimit messages per channel)... This might take a moment!"
                            }

                            var formattedMessages: String? = null
                            guildInteraction.guild.channels.collect { channel ->
                                if (channel !is TextChannel) return@collect
                                formattedMessages = messageFormatter.formatChannelMessages(channel, serverMessageLimit)
                            }
                        

                            if (formattedMessages == null) {
                                logger.info("No messages found to analyze in guild: ${guildInteraction.guildId}")
                                response.respond {
                                    content = "No messages found to analyze in the server!"
                                }
                                return@on
                            }

                            val result = vibeChecker.checkVibe(formattedMessages!!)
                            logger.debug("Server vibe check completed for guild: ${guildInteraction.guildId}")
                            response.respond {
                                content = "✨ Server Vibe Check Results:\n$result"
                            }
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
    
    val channelMessageLimit = System.getenv("CHANNEL_MESSAGE_LIMIT")?.toIntOrNull() ?: 100
    val serverMessageLimit = System.getenv("SERVER_MESSAGE_LIMIT")?.toIntOrNull() ?: 20
    
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