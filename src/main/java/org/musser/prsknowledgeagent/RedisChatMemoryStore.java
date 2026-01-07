package org.musser.prsknowledgeagent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.Utils.isNullOrBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * Custom Redis implementation of ChatMemoryStore using Lettuce client
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String DEFAULT_KEY_PREFIX = "langchain4j:chat:memory:";
    
    private final RedisClient redisClient;
    private final String keyPrefix;

    public RedisChatMemoryStore(RedisClient redisClient) {
        this(redisClient, DEFAULT_KEY_PREFIX);
    }

    public RedisChatMemoryStore(RedisClient redisClient, String keyPrefix) {
        this.redisClient = ensureNotNull(redisClient, "redisClient");
        this.keyPrefix = isNullOrBlank(keyPrefix) ? DEFAULT_KEY_PREFIX : keyPrefix;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        ensureNotNull(memoryId, "memoryId");
        
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String key = keyPrefix + memoryId;
            
            List<String> serializedMessages = commands.lrange(key, 0, -1);
            List<ChatMessage> messages = new ArrayList<>();
            
            for (String serializedMessage : serializedMessages) {
                messages.add(ChatMessageDeserializer.messageFromJson(serializedMessage));
            }
            
            return messages;
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        ensureNotNull(memoryId, "memoryId");
        ensureNotNull(messages, "messages");
        
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String key = keyPrefix + memoryId;
            
            // Clear existing messages
            commands.del(key);
            
            // Add new messages
            if (!messages.isEmpty()) {
                String[] serializedMessages = messages.stream()
                        .map(ChatMessageSerializer::messageToJson)
                        .toArray(String[]::new);
                commands.rpush(key, serializedMessages);
            }
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        ensureNotNull(memoryId, "memoryId");
        
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String key = keyPrefix + memoryId;
            commands.del(key);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RedisClient redisClient;
        private String keyPrefix = DEFAULT_KEY_PREFIX;

        public Builder redisClient(RedisClient redisClient) {
            this.redisClient = redisClient;
            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public RedisChatMemoryStore build() {
            return new RedisChatMemoryStore(redisClient, keyPrefix);
        }
    }
}
