package vibecheckbot

import dev.kord.core.Kord
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.Guild
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours

class EmojiCache(private val kord: Kord) {
    private val logger = LoggerFactory.getLogger(EmojiCache::class.java)
    private val emojiCache = ConcurrentHashMap<String, GuildEmoji>()
    private var refreshJob: Job? = null
    
    suspend fun start() {
        logger.info("Starting emoji cache...")
        refreshEmojis()
        scheduleRefresh()
        logger.info("Emoji cache started with ${emojiCache.size} emojis")
    }
    
    fun stop() {
        logger.info("Stopping emoji cache...")
        refreshJob?.cancel()
        emojiCache.clear()
        logger.info("Emoji cache stopped")
    }
    
    private fun scheduleRefresh() {
        refreshJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(4.hours)
                try {
                    logger.debug("Refreshing emoji cache...")
                    refreshEmojis()
                    logger.info("Emoji cache refreshed with ${emojiCache.size} emojis")
                } catch (e: Exception) {
                    logger.error("Error refreshing emoji cache: ${e.message}", e)
                }
            }
        }
    }
    
    private suspend fun refreshEmojis() {
        val newEmojiCache = ConcurrentHashMap<String, GuildEmoji>()
        
        try {
            val guilds = kord.guilds.toList()
            logger.debug("Fetching emojis from ${guilds.size} guilds")
            
            guilds.forEach { guild: Guild ->
                try {
                    val guildEmojis = guild.emojis.toList()
                    logger.debug("Found ${guildEmojis.size} emojis in guild: ${guild.name}")
                    
                    guildEmojis.forEach { emoji: GuildEmoji ->
                        if (emoji.name != null) {
                            newEmojiCache[emoji.name!!] = emoji
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error fetching emojis from guild ${guild.name}: ${e.message}", e)
                }
            }
            
            // Update the cache atomically
            emojiCache.clear()
            emojiCache.putAll(newEmojiCache)
            
        } catch (e: Exception) {
            logger.error("Error during emoji cache refresh: ${e.message}", e)
        }
    }
    
    fun getAvailableEmojiNames(): List<String> {
        return emojiCache.keys.toList()
    }
    
    fun getEmoji(name: String): GuildEmoji? {
        return emojiCache[name]
    }
    
    fun getCacheSize(): Int {
        return emojiCache.size
    }
} 