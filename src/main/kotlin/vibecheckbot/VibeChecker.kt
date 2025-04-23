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
    private val openAIModelName: String = "gpt-4.1-nano"
) {
    private val logger = LoggerFactory.getLogger(VibeChecker::class.java)

    suspend fun checkChannelVibe(text: String): String = withContext(Dispatchers.IO) {
        logger.debug("Starting channel vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Analyze the given text from a single Discord channel and provide a clinical assessment of its vibe.
                    
                    Focus on:
                    - The overall tone and sentiment of the channel
                    - The general atmosphere and mood
                    - Any notable patterns in communication
                    - The level of engagement and activity
                    - Whether the channel seems welcoming and inclusive
                    
                    Provide a concise but insightful analysis that captures the essence of the channel's vibe.
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
                    You are a vibe checker. Analyze the given text from multiple Discord channels and provide a comprehensive assessment of the server's vibe.
                    
                    For each channel:
                    - Analyze its unique characteristics and purpose
                    - Assess the tone, sentiment, and atmosphere
                    - Note the level of activity and engagement
                    - Identify any notable patterns or themes
                    
                    Then provide:
                    - A summary of each channel's individual vibe
                    - An overall assessment of the server's vibe
                    - How different channels complement or contrast with each other
                    - Whether the server as a whole feels cohesive and welcoming
                    
                    Structure your response to clearly separate channel-specific analysis from the overall server assessment.
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