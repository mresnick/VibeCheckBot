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
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.interaction.channel
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
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
import dev.kord.core.entity.interaction.UserOptionValue
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.User
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.DiscordEmoji
import dev.kord.core.entity.Message
import dev.kord.core.entity.StandardEmoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.entity.GuildEmoji
import kotlin.random.Random
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize

class VibeCheckBot(
    private val discordToken: String,
    private val openAIToken: String,
    private val channelMessageLimit: Int,
    private val serverMessageLimit: Int,
    private val userMessageLimit: Int,
    private val openAIModelName: String,
    private val messageCheckChance: Double,
    private val minReactionInterval: Long,
    private val maxReactionInterval: Long,
    private val imageMessageLimit: Int,
    private val maxTokens: Int
) {
    private val logger = LoggerFactory.getLogger(VibeCheckBot::class.java)
    private lateinit var kord: Kord
    private lateinit var emojiCache: EmojiCache
    private val openAI = OpenAI(openAIToken)
    private val vibeChecker = VibeChecker(openAI, openAIModelName, maxTokens)
    private val messageFormatter = MessageFormatter()
    private val maxDiscordMessageLength = 2000
    
    // Track last reaction time per channel
    private val lastReactionTimes = mutableMapOf<Snowflake, Instant>()

    suspend fun start() {
        logger.info("Starting VibeCheckBot...")
        
        kord = Kord(discordToken)
        
        // Initialize emoji cache
        emojiCache = EmojiCache(kord)
        
        // Register slash commands
        kord.createGlobalChatInputCommand("vibecheck", "Check the vibe of this server or channel") {
            subCommand("channel", "Check the vibe of a channel") {
                channel("target", "The channel to check (defaults to current channel)") {
                    channelTypes = listOf(ChannelType.GuildText)
                }
            }
            subCommand("server", "Check the vibe of the entire server")
            subCommand("user", "Check the vibe of a specific user") {
                user("target", "The user to check")
                channel("channel", "The channel to check (optional)") {
                    channelTypes = listOf(ChannelType.GuildText)
                }
            }
            subCommand("about", "Who am I?")
        }
        kord.createGlobalChatInputCommand("vibeimage", "Generate an image representing a vibe") {
            subCommand("channel", "Generate an image representing the channel's vibe") {
                channel("target", "The channel to generate an image for (defaults to current channel)") {
                    channelTypes = listOf(ChannelType.GuildText)
                }
            }
            subCommand("user", "Generate an image representing a user's vibe") {
                user("target", "The user to generate an image for")
            }
            subCommand("server", "Generate an image representing the server's vibe")
        }
        logger.debug("Slash commands registered successfully")

        // Handle message events
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on // Ignore bot messages
            
            val channelId = message.channelId
            val currentTime = Instant.now()
            val lastReactionTime = lastReactionTimes[channelId]
            
            // Calculate time since last reaction
            val secondsSinceLastReaction = lastReactionTime?.let {
                ChronoUnit.SECONDS.between(it, currentTime)
            } ?: maxReactionInterval
            
            // Calculate probability based on time since last reaction
            val timeBasedProbability = when {
                secondsSinceLastReaction < minReactionInterval -> 0.0
                secondsSinceLastReaction > maxReactionInterval -> messageCheckChance
                else -> {
                    val progress = (secondsSinceLastReaction - minReactionInterval).toDouble() / 
                                 (maxReactionInterval - minReactionInterval)
                    messageCheckChance * progress
                }
            }
            
            // Only check messages based on calculated probability
            if (Random.nextDouble() < timeBasedProbability) {
                try {
                    // Get available custom emojis from the global cache
                    val availableCustomEmojis = emojiCache.getAvailableEmojiNames()
                    
                    // Add reactions based on message content
                    val reaction = vibeChecker.checkMessageVibeEmoji(message.content, availableCustomEmojis)
                    if (reaction != null) {
                        val (type, emoji) = reaction
                        when (type) {
                            "unicode" -> {
                                message.addReaction(ReactionEmoji.Unicode(emoji))
                                lastReactionTimes[channelId] = currentTime
                            }
                            "custom" -> {
                                val customEmoji = emojiCache.getEmoji(emoji)
                                if (customEmoji != null) {
                                    message.addReaction(ReactionEmoji.Custom(customEmoji.id, customEmoji.name!!, customEmoji.isAnimated))
                                    lastReactionTimes[channelId] = currentTime
                                } else {
                                    logger.debug("Custom emoji not found in cache: $emoji")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error handling emoji reaction: ${e.message}", e)
                }
            }
        }

        // Handle slash commands
        kord.on<ChatInputCommandInteractionCreateEvent> {
            val command = interaction.command as SubCommand
            when (command.rootName) {
                "vibecheck" -> {
                    when (command.name) {
                        "channel" -> {
                            val targetChannel = command.options["target"]?.value?.let { kord.getChannel(it as Snowflake) } as? TextChannel
                            val channel = targetChannel ?: interaction.channel.asChannel() as? TextChannel
                            
                            if (channel == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in text channels!"
                                }
                                return@on
                            }

                            val response = interaction.deferPublicResponse()
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

                            val response = interaction.deferPublicResponse()
                            checkServerVibe(guildInteraction, response)
                        }
                        "user" -> {
                            val guildInteraction = interaction as? GuildChatInputCommandInteraction
                            if (guildInteraction == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in a server!"
                                }
                                return@on
                            }

                            val targetUser = (command.options["target"]?.value as Snowflake).let { kord.getUser(it) }
                            if (targetUser == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "Please specify a user to check!"
                                }
                                return@on
                            }

                            val targetChannel = command.options["channel"]?.value?.let { kord.getChannel(it as Snowflake) } as? TextChannel
                            val response = interaction.deferPublicResponse()
                            checkUserVibe(guildInteraction, targetUser, response, targetChannel)
                        }
                        "about" -> {
                            val response = interaction.deferPublicResponse()
                            val result = vibeChecker.getAboutInfo()
                            response.respond {
                                content = result
                            }
                        }
                    }
                }
                "vibeimage" -> {
                    when (command.name) {
                        "channel" -> {
                            val targetChannel = command.options["target"]?.value?.let { kord.getChannel(it as Snowflake) } as? TextChannel
                            val channel = targetChannel ?: interaction.channel.asChannel() as? TextChannel
                            if (channel == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in text channels!"
                                }
                                return@on
                            }
                            val response = interaction.deferPublicResponse()
                            generateChannelVibeImage(channel, response)
                        }
                        "user" -> {
                            val guildInteraction = interaction as? GuildChatInputCommandInteraction
                            if (guildInteraction == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in a server!"
                                }
                                return@on
                            }
                            val targetUser = (command.options["target"]?.value as Snowflake).let { kord.getUser(it) }
                            if (targetUser == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "Please specify a user to generate an image for!"
                                }
                                return@on
                            }
                            val response = interaction.deferPublicResponse()
                            generateUserVibeImage(guildInteraction, targetUser, response)
                        }
                        "server" -> {
                            val guildInteraction = interaction as? GuildChatInputCommandInteraction
                            if (guildInteraction == null) {
                                interaction.deferEphemeralResponse().respond {
                                    content = "This command can only be used in a server!"
                                }
                                return@on
                            }
                            val response = interaction.deferPublicResponse()
                            generateServerVibeImage(guildInteraction, response)
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

        emojiCache.start()

        logger.info("VibeCheckBot started successfully")

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    suspend fun stop() {
        logger.info("Stopping VibeCheckBot...")
        
        // Stop emoji cache
        emojiCache.stop()
        
        if (::kord.isInitialized) {
            kord.logout()
        }
        logger.info("VibeCheckBot stopped successfully")
    }

    private suspend fun sendLongMessage(messageChannelBehavior: MessageChannelBehavior, content: String) {
        if (content.length <= maxDiscordMessageLength) {
            messageChannelBehavior.createMessage {
                this.content = content
            }
            return
        }

        // Split the content into chunks of maxDiscordMessageLength
        val chunks = content.chunked(maxDiscordMessageLength)
        chunks.forEach { chunk ->
            messageChannelBehavior.createMessage {
                this.content = chunk
            }
        }
    }

    private suspend fun getChannelMessages(channel: TextChannel, limit: Int): List<Message> {
        return try { 
            channel.getMessagesBefore(channel.lastMessageId!!, limit)
                .toList()
                .filter { message -> message.author != null }
        } catch (e: Exception) {
            logger.error("Error getting messages from channel: ${channel.name}", e)
            emptyList()
        }
    }

    private suspend fun checkChannelVibe(channel: TextChannel, response: DeferredPublicMessageInteractionResponseBehavior) {
        logger.debug("Channel vibe check requested in channel: ${channel.name}")

        val messages = getChannelMessages(channel, channelMessageLimit)
        val formattedMessages = if (messages.isNotEmpty()) {
            listOf("Channel: #${channel.name}") + messages.mapNotNull { messageFormatter.formatMessage(it) }
        } else {
            emptyList()
        }

        if (formattedMessages.isEmpty()) {
            logger.info("No messages found to analyze in channel: ${channel.name}")
            response.respond {
                content = "No messages found to analyze in this channel!"
            }
            return
        }

        val result = vibeChecker.checkChannelVibe(formattedMessages.joinToString("\n"))
        logger.debug("Channel vibe check completed for channel: ${channel.name}")
        response.respond {
            content = result
        }
    }

    private suspend fun checkServerVibe(guildInteraction: GuildChatInputCommandInteraction, response: DeferredPublicMessageInteractionResponseBehavior) {
        logger.debug("Server vibe check requested in guild: ${guildInteraction.guildId}")

        val formattedMessage: String = guildInteraction.guild.channels.toList()
            .filter{ it is TextChannel }
            .map { channel -> 
                val channelName = (channel as TextChannel).name
                logger.debug("Getting messages from $channelName")
                val messages = getChannelMessages(channel, serverMessageLimit)
                val formattedMessages = if (messages.isNotEmpty()) {
                    listOf("Channel: #$channelName") + messages.mapNotNull { messageFormatter.formatMessage(it) }
                } else {
                    emptyList()
                }
                if (formattedMessages.isNotEmpty()) {
                    formattedMessages.joinToString("\n")
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
        response.respond {
            content = result
        }
    }

    private suspend fun checkUserVibe(
        guildInteraction: GuildChatInputCommandInteraction,
        targetUser: dev.kord.core.entity.User,
        response: DeferredPublicMessageInteractionResponseBehavior,
        targetChannel: TextChannel? = null
    ) {
        logger.debug("User vibe check requested for user: ${targetUser.id}${targetChannel?.let { " in channel: ${it.name}" } ?: ""}")

        val formattedMessage: String = guildInteraction.guild.channels.toList()
            .filter { it is TextChannel }
            .filter { targetChannel == null || it == targetChannel }
            .map { channel -> 
                val channelName = (channel as TextChannel).name
                logger.debug("Getting messages from $channelName")
                val messages = getChannelMessages(channel, userMessageLimit)
                    .filter { message -> message.author?.id == targetUser.id }
                val formattedMessages = if (messages.isNotEmpty()) {
                    listOf("Channel: #$channelName") + messages.mapNotNull { messageFormatter.formatMessage(it) }
                } else {
                    emptyList()
                }
                if (formattedMessages.isNotEmpty()) {
                    formattedMessages.joinToString("\n")
                } else null
            }
            .filterNotNull()
            .joinToString("\n\n")
        
        logger.debug("Formatted message: $formattedMessage")

        if (formattedMessage.isEmpty()) {
            logger.info("No messages found to analyze for user: ${targetUser.id}${targetChannel?.let { " in channel: ${it.name}" } ?: ""}")
            response.respond {
                content = "No messages found to analyze for ${targetUser.mention}${targetChannel?.let { " in #${it.name}" } ?: ""}!"
            }
            return
        }

        val result = vibeChecker.checkUserVibe(formattedMessage)
        logger.debug("User vibe check completed for user: ${targetUser.id}${targetChannel?.let { " in channel: ${it.name}" } ?: ""}")
        response.respond {
            content = result
        }
    }

    private suspend fun generateChannelVibeImage(channel: TextChannel, response: DeferredPublicMessageInteractionResponseBehavior) {
        logger.debug("Channel vibe image generation requested in channel: ${channel.name}")

        val messages = getChannelMessages(channel, imageMessageLimit)
        val formattedMessages = if (messages.isNotEmpty()) {
            messages.mapNotNull { messageFormatter.formatMessageWithoutMetadata(it) }
        } else {
            emptyList()
        }

        if (formattedMessages.isEmpty()) {
            logger.info("No messages found to analyze in channel: ${channel.name}")
            response.respond {
                content = "No messages found to generate an image for this channel!"
            }
            return
        }

        val imageUrl = vibeChecker.generateChannelVibeImage(channel.name, formattedMessages)
        if (imageUrl != null) {
            response.respond {
                content = "Here is an image representing the vibe of #${channel.name}:\n$imageUrl"
            }
        } else {
            response.respond {
                content = "Failed to generate an image for this channel's vibe."
            }
        }
    }

    private suspend fun generateUserVibeImage(
        guildInteraction: GuildChatInputCommandInteraction,
        targetUser: dev.kord.core.entity.User,
        response: DeferredPublicMessageInteractionResponseBehavior
    ) {
        logger.debug("User vibe image generation requested for user: ${targetUser.id}")

        val formattedMessages: List<String> = guildInteraction.guild.channels.toList()
            .filter { it is TextChannel }
            .flatMap { channel ->
                val messages = getChannelMessages(channel as TextChannel, imageMessageLimit)
                    .filter { message -> message.author?.id == targetUser.id }
                if (messages.isNotEmpty()) {
                    messages.mapNotNull { messageFormatter.formatMessageWithoutMetadata(it) }
                } else {
                    emptyList()
                }
            }

        if (formattedMessages.isEmpty()) {
            logger.info("No messages found to analyze for user: ${targetUser.id}")
            response.respond {
                content = "No messages found to generate an image for this user's vibe."
            }
            return
        }

        val imageUrl = vibeChecker.generateUserVibeImage(targetUser.username, formattedMessages)
        if (imageUrl != null) {
            response.respond {
                content = "Here is an image representing the vibe of ${targetUser.mention}:\n$imageUrl"
            }
        } else {
            response.respond {
                content = "Failed to generate an image for this user's vibe."
            }
        }
    }

    private suspend fun generateServerVibeImage(
        guildInteraction: GuildChatInputCommandInteraction,
        response: DeferredPublicMessageInteractionResponseBehavior
    ) {
        logger.debug("Server vibe image generation requested in guild: ${guildInteraction.guildId}")

        val formattedMessages: List<String> = guildInteraction.guild.channels.toList()
            .filter { it is TextChannel }
            .flatMap { channel ->
                val messages = getChannelMessages(channel as TextChannel, imageMessageLimit)
                if (messages.isNotEmpty()) {
                    messages.mapNotNull { messageFormatter.formatMessageWithoutMetadata(it) }
                } else {
                    emptyList()
                }
            }

        if (formattedMessages.isEmpty()) {
            logger.info("No messages found to analyze in guild: ${guildInteraction.guildId}")
            response.respond {
                content = "No messages found to generate an image for the server!"
            }
            return
        }

        val imageUrl = vibeChecker.generateServerVibeImage(formattedMessages)
        if (imageUrl != null) {
            response.respond {
                content = "Here is an image representing the vibe of this server:\n$imageUrl"
            }
        } else {
            response.respond {
                content = "Failed to generate an image for this server's vibe."
            }
        }
    }
}

fun main() = runBlocking {
    val logger = LoggerFactory.getLogger("VibeCheckBotMain")
    
    val discordToken = System.getenv("DISCORD_TOKEN") ?: throw IllegalStateException("DISCORD_TOKEN environment variable not set")
    val openAIToken = System.getenv("OPENAI_API_KEY") ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set")
    
    val channelMessageLimit = System.getenv("CHANNEL_MESSAGE_LIMIT")?.toIntOrNull() ?: 20
    val serverMessageLimit = System.getenv("SERVER_MESSAGE_LIMIT")?.toIntOrNull() ?: 10
    val userMessageLimit = System.getenv("USER_MESSAGE_LIMIT")?.toIntOrNull() ?: 50
    val openAIModelName = System.getenv("OPENAI_MODEL_NAME") ?: "gpt-4.1-nano"
    val messageCheckChance = System.getenv("MESSAGE_CHECK_CHANCE")?.toDoubleOrNull() ?: 0.05
    val minReactionInterval = System.getenv("MIN_REACTION_INTERVAL")?.toLongOrNull() ?: 30L
    val maxReactionInterval = System.getenv("MAX_REACTION_INTERVAL")?.toLongOrNull() ?: 300L
    val imageMessageLimit = System.getenv("IMAGE_MESSAGE_LIMIT")?.toIntOrNull() ?: 10
    val maxTokens = System.getenv("MAX_TOKENS")?.toIntOrNull() ?: 1000
    
    logger.info("""Initializing VibeCheckBot with the following configuration:\n" +
                "discordToken: "+ if (discordToken.isNotEmpty()) "[REDACTED]" else "(not set)" + ", " +
                "openAIToken: "+ if (openAIToken.isNotEmpty()) "[REDACTED]" else "(not set)" + ", " +
                "channelMessageLimit: $channelMessageLimit, " +
                "serverMessageLimit: $serverMessageLimit, " +
                "userMessageLimit: $userMessageLimit, " +
                "imageMessageLimit: $imageMessageLimit, " +
                "openAIModelName: $openAIModelName, " +
                "messageCheckChance: $messageCheckChance, " +
                "minReactionInterval: $minReactionInterval, " +
                "maxReactionInterval: $maxReactionInterval, " +
                "maxTokens: $maxTokens" +
                """)
    
    val bot = VibeCheckBot(
        discordToken = discordToken,
        openAIToken = openAIToken,
        channelMessageLimit = channelMessageLimit,
        serverMessageLimit = serverMessageLimit,
        userMessageLimit = userMessageLimit,
        openAIModelName = openAIModelName,
        messageCheckChance = messageCheckChance,
        minReactionInterval = minReactionInterval,
        maxReactionInterval = maxReactionInterval,
        imageMessageLimit = imageMessageLimit,
        maxTokens = maxTokens
    )
    
    try {
        bot.start()
    } catch (e: Exception) {
        logger.error("Error starting bot: ${e.message}", e)
        bot.stop()
    }
} 