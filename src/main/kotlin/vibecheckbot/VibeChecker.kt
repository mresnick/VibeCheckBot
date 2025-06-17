package vibecheckbot

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class VibeChecker(
    private val openAI: OpenAI,
    private val openAIModelName: String
) {
    private val logger = LoggerFactory.getLogger(VibeChecker::class.java)

    suspend fun checkChannelVibe(text: String): String = withContext(Dispatchers.IO) {
        logger.debug("Starting channel vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Your only purpose is to check vibes, and you do that job well. Given a channel and some of its message history,
                    you will generate a concise analysis of the vibe of the channel. The output should begin with a header, "Vibe Check: #channelName", where channelName is
                    the name of the channel provided in the input text. The output should be formatted for Discord, and all headers should be bolded.
                    
                    Take anything and everything into account, including but not limited to: 
                    - The overall tone and sentiment of the channel
                    - The general atmosphere and mood
                    - Any notable patterns in communication
                    - The level of engagement and activity
                """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = text
            )
        )

        try {
            logger.debug("Sending channel vibe check request to OpenAI using model: $openAIModelName, request body: ${messages.map { "${it.role}: ${it.content?.take(200) ?: "null"}..." }}")
            
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 500
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check channel vibe at this time."
            logger.info("Channel vibe response: $result")
            logger.debug("Channel vibe check completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during channel vibe check: ${e.message}", e)
            "Unable to check channel vibe at this time."
        }
    }

    suspend fun checkServerVibe(text: String): String = withContext(Dispatchers.IO) {
        logger.debug("Starting server vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Your only purpose is to check vibes, and you do that job well. Given a list of channels and their message history, 
                    you will generate a concise analysis of the vibe of the server. The output does not need to discuss every channel, but should select a few highlights and give a general overview of the server vibe as well.

                    For every channel mentioned, it should begin with "Channel: #channelName", where channelName is the name of the channel. 
                    
                    The overall output should conclude with a "Server Vibe" section, using that as a header. The output should be formatted for Discord, and all headers should be bolded.

                    Take anything and everything into account, including but not limited to: 
                    - The overall tone and sentiment of the channel
                    - The general atmosphere and mood
                    - Any notable patterns in communication
                    - The level of engagement and activity
                """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = text
            )
        )

        try {
            logger.debug("Sending server vibe check request to OpenAI using model: $openAIModelName, request body: ${messages.map { "${it.role}: ${it.content?.take(200) ?: "null"}..." }}")
            
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 1000
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check server vibe at this time."
            logger.info("Server vibe response: $result")
            logger.debug("Server vibe check completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during server vibe check: ${e.message}", e)
            "Unable to check server vibe at this time."
        }
    }

    suspend fun checkUserVibe(text: String): String = withContext(Dispatchers.IO) {
        logger.debug("Starting user vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Your only purpose is to check vibes, and you do that job well. Analyze the given text from a Discord user's messages and generate a check of their vibe.
                    
                    Focus on:
                    - The user's communication style and tone
                    - Their personality traits and characteristics
                    - Their level of engagement and activity
                    - Their interaction patterns with others
                    - Whether they seem friendly and approachable
                    
                    The response should provide an overall assessment rather than specific examples. The response should have a header indicating the user and channel being analyzed. 
                    
                    The response should be formatted for Discord, and all headers should be bolded. 
                """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = text
            )
        )

        try {
            logger.debug("Sending user vibe check request to OpenAI using model: $openAIModelName, request body: ${messages.map { "${it.role}: ${it.content?.take(200) ?: "null"}..." }}")
            
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 500
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check user vibe at this time."
            logger.info("User vibe response: $result")
            logger.debug("User vibe check completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during user vibe check: ${e.message}", e)
            "Unable to check user vibe at this time."
        }
    }

    suspend fun checkMessageVibeEmoji(text: String, availableCustomEmojis: List<String> = emptyList()): Pair<String, String>? = withContext(Dispatchers.IO) {
        logger.debug("Starting message vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Analyze the vibe of the following message on a scale of 1 to 10, where 1 is the worst and 10 is the best.

                    Vibe should take into account the message's content, length, any salient points it is making, humor, etc.

                    If the vibe is a 9 or 10, respond with an emoji of your choosing. You can use either a Unicode emoji or a custom server emoji.
                    For Unicode emojis, respond with "unicode:emoji" (e.g., "unicode:🌟")
                    For custom server emojis, respond with "custom:emoji_name" (e.g., "custom:pepega")

                    Available custom emojis: ${availableCustomEmojis.joinToString(", ")}

                    Respond only with the emoji format, should you select one.
                    If using a custom emoji, make sure to use one from the available list.
                """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = text
            )
        )

        try {
            logger.debug("Sending message vibe check request to OpenAI using model: $openAIModelName, request body: ${messages.map { "${it.role}: ${it.content?.take(200) ?: "null"}..." }}")
            
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 20
                )
            )

            val result = completion.choices.first().message.content?.trim()
            logger.info("Message vibe response: $result")
            
            if (result.isNullOrEmpty()) {
                logger.debug("Message vibe check completed with no emoji (vibe not high enough)")
                return@withContext null
            }

            // Parse the response format
            val parts = result.split(":", limit = 2)
            if (parts.size != 2) {
                logger.error("Invalid emoji format received: $result")
                return@withContext null
            }

            val (type, emoji) = parts
            if (type !in listOf("unicode", "custom")) {
                logger.error("Invalid emoji type received: $type")
                return@withContext null
            }

            // Validate custom emoji if one was selected
            if (type == "custom" && emoji !in availableCustomEmojis) {
                logger.error("Selected custom emoji not in available list: $emoji")
                return@withContext null
            }

            logger.debug("Message vibe check completed successfully with emoji: $result")
            Pair(type, emoji)
        } catch (e: Exception) {
            logger.error("Error during message vibe check: ${e.message}", e)
            null
        }
    }

    suspend fun getAboutInfo(): String = withContext(Dispatchers.IO) {
        logger.debug("Getting about info")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a discord bot that can be used to check the vibe of a server, channel, or user. Your name is VibeCheckBot.

                    Respond as though a user has just run an "about" command looking to understand your capabilities and function.
                """.trimIndent()
            )
        )

        try {
            logger.debug("Sending about info request to OpenAI using model: $openAIModelName, request body: ${messages.map { "${it.role}: ${it.content?.take(200) ?: "null"}..." }}")
            
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 500
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to get about info at this time."
            logger.info("About info response: $result")
            logger.debug("About info request completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during about info request: ${e.message}", e)
            "Unable to get about info at this time."
        }
    }
} 