[![Docker](https://github.com/mresnick/VibeCheckBot/actions/workflows/docker-publish.yml/badge.svg?branch=main)](https://github.com/mresnick/VibeCheckBot/actions/workflows/docker-publish.yml)

# VibeCheckBot

Discord server and channel vibe check, to make sure your vibes are vibin'.

## Features

- `/vibecheck channel` - Vibe check the current channel
- `/vibecheck server` - Vibe check the whole server
- `/vibecheck user @user [#channel]` - Vibe check a specific user (optionally in a specific channel)
- Automatic emoji reactions to messages based on their vibe

## Emoji Reactions

The bot automatically reacts to messages with emojis that match their vibe. To prevent spam, reactions are controlled by three parameters:

- `MESSAGE_CHECK_CHANCE`: Base probability (0.0 to 1.0) of checking a message
- `MIN_REACTION_INTERVAL`: Minimum seconds between reactions in a channel (default: 30)
- `MAX_REACTION_INTERVAL`: Maximum seconds between reactions in a channel (default: 300)

The probability of reacting increases gradually between the minimum and maximum intervals, ensuring reactions are evenly distributed.

## Setup

1. Create a `.env` file in the project root with the following variables:
```env
DISCORD_TOKEN=your_discord_bot_token
OPENAI_API_KEY=your_openai_api_key
CHANNEL_MESSAGE_LIMIT=20  # Optional, defaults to 20
SERVER_MESSAGE_LIMIT=10   # Optional, defaults to 10
USER_MESSAGE_LIMIT=50     # Optional, defaults to 50
OPENAI_MODEL_NAME=gpt-4.1-nano # Optional, defaults to gpt-4.1-nano
MESSAGE_CHECK_CHANCE=0.05 # Optional, defaults to 0.05
MIN_REACTION_INTERVAL=30  # Optional, defaults to 30 seconds
MAX_REACTION_INTERVAL=300 # Optional, defaults to 300 seconds
```

2. Build and run the bot with Docker:
```bash
docker-compose up
```
  or don't
```bash
./gradlew build
java -jar build/libs/vibecheckbot.jar
```

## Development

### Running Tests

1. Create a `.env` file with test credentials:
```env
DISCORD_TOKEN=your_test_discord_bot_token
OPENAI_API_KEY=your_test_openai_api_key
```

2. Run the tests:
```bash
./gradlew test
```

For integration tests:
```bash
./gradlew integrationTest
```

## Configuration

### Environment Variables

- `DISCORD_TOKEN` (Required): Your Discord bot token
- `OPENAI_API_KEY` (Required): Your OpenAI API key
- `CHANNEL_MESSAGE_LIMIT` (Optional): Number of messages to analyze in a channel (default: 20)
- `SERVER_MESSAGE_LIMIT` (Optional): Number of messages to analyze per channel in server check (default: 10)
- `USER_MESSAGE_LIMIT` (Optional): Number of messages to analyze per channel in user check (default: 50)
- `OPENAI_MODEL_NAME` (Optional): OpenAI model to use (default: gpt-4.1-nano)
- `MESSAGE_CHECK_CHANCE` (Optional): Base probability (0.0 to 1.0) of checking a message (default: 0.05)
- `MIN_REACTION_INTERVAL` (Optional): Minimum seconds between reactions in a channel (default: 30)
- `MAX_REACTION_INTERVAL` (Optional): Maximum seconds between reactions in a channel (default: 300)

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
