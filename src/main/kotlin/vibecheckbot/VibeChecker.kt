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
                    the name of the channel. The output should be formatted for Discord, and all headers should be bolded.
                    
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
            logger.debug("Sending channel vibe check request to OpenAI using model: $openAIModelName")
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 500
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check channel vibe at this time."
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
                    you will generate a concise analysis of the vibe of the server. The output does not need to discuss every channel, but should select a few highlights and give a general overview of the server vibe as well.length

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
            logger.debug("Sending server vibe check request to OpenAI using model: $openAIModelName")
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(openAIModelName),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 1000
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check server vibe at this time."
            logger.debug("Server vibe check completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during server vibe check: ${e.message}", e)
            "Unable to check server vibe at this time."
        }
    }
} 