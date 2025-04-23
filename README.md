# VibeCheckBot

A Discord bot that analyzes the vibe of your server or channels using OpenAI's GPT models. It can check the overall mood and atmosphere of your server or specific channels by analyzing recent messages.

## Features

- `/vibecheck channel` - Analyzes the last 100 messages in the current channel
- `/vibecheck server` - Analyzes 20 messages from each channel in the server
- Uses OpenAI's GPT models for intelligent vibe analysis
- Configurable message limits for both channel and server analysis

## Prerequisites

- Java 21 or higher
- Gradle 8.8 or higher
- A Discord Bot Token
- An OpenAI API Key

## Setup

1. **Create a Discord Bot**
   - Go to the [Discord Developer Portal](https://discord.com/developers/applications)
   - Create a new application
   - Navigate to the "Bot" tab and create a bot
   - Copy the bot token

2. **Configure Environment Variables**
   Create a `.env` file in the root directory with the following variables:
   ```
   DISCORD_TOKEN=your_discord_bot_token
   OPENAI_API_KEY=your_openai_api_key
   CHANNEL_MESSAGE_LIMIT=100  # Optional, defaults to 100
   SERVER_MESSAGE_LIMIT=20    # Optional, defaults to 20
   ```

3. **Build and Run**
   ```bash
   # Using Gradle
   ./gradlew build
   ./gradlew run

   # Using Docker
   docker build -t vibecheckbot .
   docker run --env-file .env vibecheckbot
   ```

## Running Tests

The project includes both unit tests and integration tests. Here's how to run them:

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run a specific test class
./gradlew test --tests vibecheckbot.VibeCheckerTest

# Run tests with detailed output
./gradlew test --info
```

### Integration Tests
Integration tests require an OpenAI API key to be set in the environment:
```bash
# Set the OpenAI API key
export OPENAI_API_KEY=your_api_key

# Run integration tests
./gradlew test --tests vibecheckbot.VibeCheckerIntegrationTest
```

### Test Coverage
To generate a test coverage report:
```bash
./gradlew jacocoTestReport
```
The report will be available at `app/build/reports/jacoco/test/html/index.html`

## Discord Permissions

The bot requires the following permissions:
- `Send Messages`
- `Read Message History`
- `Use Slash Commands`
- `Message Content Intent` (Required for reading message content)

To enable the Message Content Intent:
1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Select your application
3. Go to the "Bot" tab
4. Scroll down to "Privileged Gateway Intents"
5. Enable "Message Content Intent"

## Inviting the Bot to Your Server

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Select your application
3. Go to the "OAuth2" tab
4. In the "URL Generator" section:
   - Select the `bot` scope
   - Select the following permissions:
     - `Send Messages`
     - `Read Message History`
     - `Use Slash Commands`
5. Copy the generated URL and use it to invite the bot to your server

## Usage

Once the bot is running and added to your server, you can use the following commands:

- `/vibecheck channel` - Analyzes the vibe of the current channel
- `/vibecheck server` - Analyzes the vibe of the entire server

## Configuration

You can customize the bot's behavior by setting the following environment variables:

- `CHANNEL_MESSAGE_LIMIT` (default: 100) - Number of messages to analyze per channel
- `SERVER_MESSAGE_LIMIT` (default: 20) - Number of messages to analyze per channel when checking the entire server

## Docker Deployment

The bot can be deployed using Docker:

```bash
# Build the image
docker build -t vibecheckbot .

# Run the container
docker run --env-file .env vibecheckbot
```

Make sure your `.env` file contains all required environment variables.

## Contributing

Feel free to open issues or submit pull requests for any improvements or bug fixes.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 