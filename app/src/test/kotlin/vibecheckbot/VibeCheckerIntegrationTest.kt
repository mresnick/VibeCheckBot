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
    fun `checkVibe returns valid response with real OpenAI API`() = runTest {
        logger.info("Starting integration test for VibeChecker")
        
        // Given
        val openAIToken = System.getenv("OPENAI_API_KEY")
        logger.debug("Using OpenAI API token from environment variable")
        val realOpenAI = OpenAI(openAIToken)
        val integrationVibeChecker = VibeChecker(realOpenAI)
        val testText = "Hello everyone! Let's have a great day!"
        logger.debug("Test text: $testText")

        // When
        logger.info("Calling checkVibe with test text")
        val result = integrationVibeChecker.checkVibe(testText)
        logger.debug("Received result: $result")

        // Then
        logger.info("Validating test results")
        assertNotNull(result)
        assert(result.isNotBlank())
        assert(result != "Unable to check vibe at this time.")
        logger.info("Integration test completed successfully")
    }
} 