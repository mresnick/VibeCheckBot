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
        vibeChecker = VibeChecker(mockOpenAI)
    }

    @Test
    fun `checkVibe returns expected response when OpenAI call succeeds`() = runTest {
        // Given
        val expectedResponse = "The vibe is positive and energetic! 🎉"
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
            model = ModelId("gpt-3.5-turbo")
        )

        // When
        val result = vibeChecker.checkVibe(testText)

        // Then
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `checkVibe returns error message when OpenAI call fails`() = runTest {
        // Given
        val testText = "Hello everyone!"
        
        coEvery {
            mockOpenAI.chatCompletion(any())
        } throws RuntimeException("API Error")

        // When
        val result = vibeChecker.checkVibe(testText)

        // Then
        assertEquals("Unable to check vibe at this time.", result)
    }
} 