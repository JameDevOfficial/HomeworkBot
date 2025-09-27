# HomeworkBot

HomeworkBot is a Discord bot designed to help manage and organize homework tasks in a Discord server. It provides features to create, track, and manage homework assignments efficiently. 

## Features

- **Homework Setup**: Automatically sets up a forum channel for homework management with predefined tags for subjects.
- **Add Homework**: Allows users to add homework assignments with details like title, description, due date, subject, and assigned users.
- **Homework Overview**: Displays an overview of all active homework assignments, grouped by due dates.
- **Homework Completion**: Automatically closes homework threads when all assigned users react with a ✅ emoji.

## Technologies Used

- **Discord4J**: Used for interacting with the Discord API.
- **AI Assistance**: Utilized AI for bug fixes and understanding the poorly documented parts of the Discord4J library.

## Commands

- `/homework setup`: Sets up the homework forum and tags.
- `/homework add`: Adds a new homework assignment.
- `/homework overview`: Displays an overview of all homework assignments.

## How to Run

1. Clone the repository.
2. Set the `TOKEN` environment variable with your Discord bot token.
3. Run the bot using Gradle:
   ```sh
   ./gradlew run