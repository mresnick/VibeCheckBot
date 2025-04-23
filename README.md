# VibeCheckBot

A Discord bot that analyzes the vibe of channels and servers using OpenAI's GPT-3.5.

## Features

- `/vibecheck channel` - Analyzes the vibe of the current channel
- `/vibecheck server` - Analyzes the vibe of the entire server across all channels

The bot provides detailed analysis including:
- Overall tone and sentiment
- General atmosphere and mood
- Communication patterns
- Engagement levels
- Welcoming/inclusive nature
- For server analysis: how different channels complement each other

## Setup

1. Create a `.env` file in the project root with the following variables:
```env
DISCORD_TOKEN=your_discord_bot_token
OPENAI_API_KEY=your_openai_api_key
CHANNEL_MESSAGE_LIMIT=20  # Optional, defaults to 20
SERVER_MESSAGE_LIMIT=10   # Optional, defaults to 10
```

2. Build and run the bot:
```bash
./gradlew build
java -jar app/build/libs/app.jar
```

## Development

### Prerequisites

- JDK 17 or higher
- Gradle 8.0 or higher
- Docker (for running tests)

### Running Tests

1. Create a `.env.test` file with test credentials:
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

### Docker Support

The project includes Docker support for running tests in a containerized environment:

```bash
docker-compose -f docker-compose.test.yml up --build
```

## Configuration

### Environment Variables

- `DISCORD_TOKEN` (Required): Your Discord bot token
- `OPENAI_API_KEY` (Required): Your OpenAI API key
- `CHANNEL_MESSAGE_LIMIT` (Optional): Number of messages to analyze in a channel (default: 20)
- `SERVER_MESSAGE_LIMIT` (Optional): Number of messages to analyze per channel in server check (default: 10)


## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 