package vibecheckbot

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class VibeChecker(private val openAI: OpenAI) {
    private val logger = LoggerFactory.getLogger(VibeChecker::class.java)

    suspend fun checkVibe(text: String): String = withContext(Dispatchers.IO) {
        logger.debug("Starting vibe check for text of length: ${text.length}")
        
        val messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = """
                    You are a vibe checker. Analyze the given text and provide a clinical assessment of the overall vibe.
                    
                    If the text contains messages from multiple channels (indicated by "Channel: #channel-name" sections):
                    - Analyze each channel's vibe separately
                    - Provide a summary of the overall server vibe
                    - Consider the tone, sentiment, and general atmosphere of each channel
                    - Keep the analysis concise and engaging
                    
                    If the text is from a single channel:
                    - Provide a focused analysis of that channel's vibe
                    - Consider the tone, sentiment, and general atmosphere
                    - Keep it concise and engaging
                """.trimIndent()
            ),
            ChatMessage(
                role = ChatRole.User,
                content = text
            )
        )

        try {
            val completion = openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId("gpt-3.5-turbo"),
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 500
                )
            )

            val result = completion.choices.first().message.content ?: "Unable to check vibe at this time."
            logger.debug("Vibe check completed successfully")
            result
        } catch (e: Exception) {
            logger.error("Error during vibe check: ${e.message}", e)
            "Unable to check vibe at this time."
        }
    }
} 