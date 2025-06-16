package vibecheckbot

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatChoice
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VibeCheckerTest {
    private lateinit var mockOpenAI: OpenAI
    private lateinit var vibeChecker: VibeChecker

    @BeforeEach
    fun setup() {
        mockOpenAI = mockk()
        vibeChecker = VibeChecker(mockOpenAI, "gpt-4.1-nano")
    }

    @Test
    fun `checkChannelVibe returns expected response when OpenAI call succeeds`() = runTest {
        // Given
        val expectedResponse = "The channel vibe is positive and energetic! 🎉"
        val testText = "Hello everyone! Let's have a great day!"
        
        coEvery {
            mockOpenAI.chatCompletion(any())
        } returns ChatCompletion(
            id = "test-id",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(
                        role = ChatRole.Assistant,
                        content = expectedResponse
                    )
                )
            ),
            created = 0,
            model = ModelId("gpt-4.1-nano")
        )

        // When
        val result = vibeChecker.checkChannelVibe(testText)

        // Then
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `checkChannelVibe returns error message when OpenAI call fails`() = runTest {
        // Given
        val testText = "Hello everyone!"
        
        coEvery {
            mockOpenAI.chatCompletion(any())
        } throws RuntimeException("API Error")

        // When
        val result = vibeChecker.checkChannelVibe(testText)

        // Then
        assertEquals("Unable to check channel vibe at this time.", result)
    }

    @Test
    fun `checkServerVibe returns expected response when OpenAI call succeeds`() = runTest {
        // Given
        val expectedResponse = "The server vibe is diverse and engaging! 🌟"
        val testText = "Channel 1: Hello everyone! Channel 2: Great discussion!"
        
        coEvery {
            mockOpenAI.chatCompletion(any())
        } returns ChatCompletion(
            id = "test-id",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(
                        role = ChatRole.Assistant,
                        content = expectedResponse
                    )
                )
            ),
            created = 0,
            model = ModelId("gpt-4.1-nano")
        )

        // When
        val result = vibeChecker.checkServerVibe(testText)

        // Then
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `checkServerVibe returns error message when OpenAI call fails`() = runTest {
        // Given
        val testText = "Channel 1: Hello everyone!"
        
        coEvery {
            mockOpenAI.chatCompletion(any())
        } throws RuntimeException("API Error")

        // When
        val result = vibeChecker.checkServerVibe(testText)

        // Then
        assertEquals("Unable to check server vibe at this time.", result)
    }
} 