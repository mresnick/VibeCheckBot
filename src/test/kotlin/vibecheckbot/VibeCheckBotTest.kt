package vibecheckbot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

class VibeCheckBotTest {
    @Test
    fun testBotCreation() {
        val token = "test_token"
        val openAIToken = "test_openai_token"
        val bot = VibeCheckBot(
            discordToken = token,
            openAIToken = openAIToken,
            channelMessageLimit = 20,
            serverMessageLimit = 10,
            openAIModelName = "gpt-4.1-nano",
            userMessageLimit = 5,
            messageCheckChance = 0.1,
            minReactionInterval = 30L,
            maxReactionInterval = 300L,
            imageMessageLimit = 50,
            maxTokens = 1000
        )
        assertNotNull(bot, "Bot should be created successfully")
    }
} 