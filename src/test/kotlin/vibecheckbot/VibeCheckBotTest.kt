package vibecheckbot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

class VibeCheckBotTest {
    @Test
    fun testBotCreation() {
        val token = "test_token"
        val openAIToken = "test_openai_token"
        val bot = VibeCheckBot(token, openAIToken)
        assertNotNull(bot, "Bot should be created successfully")
    }
} 