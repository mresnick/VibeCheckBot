package vibecheckbot

import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.slf4j.LoggerFactory

@Tag("integration")
class VibeCheckerIntegrationTest {
    private val logger = LoggerFactory.getLogger(VibeCheckerIntegrationTest::class.java)

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    fun `checkChannelVibe returns valid response with real OpenAI API`() = runTest {
        logger.info("Starting integration test for channel vibe check")
        
        // Given
        val openAIToken = System.getenv("OPENAI_API_KEY")
        logger.debug("Using OpenAI API token from environment variable")
        val realOpenAI = OpenAI(openAIToken)
        val integrationVibeChecker = VibeChecker(realOpenAI, "gpt-4.1-nano", 1000)
        val testText = "Hello everyone! Let's have a great day!"
        logger.debug("Test text: $testText")

        // When
        logger.info("Calling checkChannelVibe with test text")
        val result = integrationVibeChecker.checkChannelVibe(testText)
        logger.debug("Received result: $result")

        // Then
        logger.info("Validating test results")
        assertNotNull(result)
        assert(result.isNotBlank())
        assert(result != "Unable to check channel vibe at this time.")
        logger.info("Channel vibe check integration test completed successfully")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    fun `checkServerVibe returns valid response with real OpenAI API`() = runTest {
        logger.info("Starting integration test for server vibe check")
        
        // Given
        val openAIToken = System.getenv("OPENAI_API_KEY")
        logger.debug("Using OpenAI API token from environment variable")
        val realOpenAI = OpenAI(openAIToken)
        val integrationVibeChecker = VibeChecker(realOpenAI, "gpt-4.1-nano", 1000)
        val testText = """
            #general: Hello everyone! Great discussion today!
            #random: Just sharing some memes and having fun
            #help: Thanks for the help with my question!
        """.trimIndent()
        logger.debug("Test text: $testText")

        // When
        logger.info("Calling checkServerVibe with test text")
        val result = integrationVibeChecker.checkServerVibe(testText)
        logger.debug("Received result: $result")

        // Then
        logger.info("Validating test results")
        assertNotNull(result)
        assert(result.isNotBlank())
        assert(result != "Unable to check server vibe at this time.")
        logger.info("Server vibe check integration test completed successfully")
    }
} 