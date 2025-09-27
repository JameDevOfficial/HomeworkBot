package homeworkbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HomeworkOverview {

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();
        return Mono.justOrEmpty(event.getInteraction().getGuildId())
            .flatMapMany((Snowflake guildId) -> {
                return client
                    .getGuildById(guildId)
                    .flatMapMany(guild -> {
                        return guild
                            .getChannels()
                            .filter(channel -> channel instanceof ForumChannel)
                            .cast(ForumChannel.class)
                            .flatMap(forum -> forum.getActiveThreads());
                    });
            })
            .flatMap(thread -> {
                return thread
                    .getLastMessage()
                    .flatMapMany(message -> {
                        return message
                            .getReactors(Emoji.unicode("✅"))
                            .map(user -> user.getId())
                            .collectList()
                            .map(reactors -> new Object[] { thread, reactors });
                    })
                    .doOnError(error -> {
                        System.err.println(
                            "Error fetching reactors: " + error.getMessage()
                        );
                    });
            })
            .filter(data -> {
                @SuppressWarnings("unchecked")
                java.util.List<Snowflake> reactors = (java.util.List<
                        Snowflake
                    >) data[1];
                ThreadChannel thread = (ThreadChannel) data[0];

                boolean hasReacted = reactors.contains(
                    event.getInteraction().getUser().getId()
                );
                return !hasReacted;
            })
            .collectList()
            .flatMap(unreactedThreads -> {
                if (unreactedThreads.isEmpty()) {
                    return event.reply(
                        "No homework threads found or all are completed!"
                    );
                }

                EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                    .title("HOMEWORK OVERVIEW")
                    .description("Here are the homework threads:");

                return Flux.fromIterable(unreactedThreads)
                    .flatMap(data -> {
                        ThreadChannel thread =
                            (ThreadChannel) ((Object[]) data)[0];
                        return thread
                            .getMessageById(thread.getId())
                            .map(initialMessage -> {
                                String content = initialMessage.getContent();
                                String description = null;
                                String due = null;
                                String subject = null;

                                for (String line : content.split("\n")) {
                                    String lower = line.trim().toLowerCase();
                                    if (lower.startsWith("**description:**")) {
                                        description = line.substring(15).trim();
                                    } else if (lower.startsWith("**due:**")) {
                                        due = line.substring(8).trim();
                                    } else if (
                                        lower.startsWith("**subject:**")
                                    ) {
                                        subject = line.substring(12).trim();
                                    } else if (lower.startsWith("**for:**")) {}
                                }

                                // Use thread title as fallback for description
                                if (
                                    description == null || description.isEmpty()
                                ) {
                                    description = thread.getName();
                                }
                                if (due == null || due.isEmpty()) {
                                    due = "No Due Date";
                                }
                                if (subject == null || subject.isEmpty()) {
                                    subject = "Unknown Subject";
                                }

                                System.out.println("Parsed Thread:");
                                System.out.println(
                                    "Description: " + description
                                );
                                System.out.println("Due: " + due);
                                System.out.println("Subject: " + subject);

                                return new Object[] {
                                    thread,
                                    due,
                                    description,
                                    subject,
                                };
                            })
                            .onErrorResume(error -> {
                                System.err.println(
                                    "Error parsing thread message: " +
                                    error.getMessage()
                                );
                                return Mono.just(
                                    new Object[] {
                                        thread,
                                        null,
                                        thread.getName(),
                                        "Unknown Subject",
                                    }
                                );
                            });
                    })
                    .doOnNext(info -> {
                        // Ensure embed fields are added for each thread
                        String due = (String) info[1];
                        String description = (String) info[2];
                        String subject = (String) info[3];

                        // Replace null or invalid values with defaults
                        if (due == null || due.isEmpty()) {
                            due = "No Due Date";
                        }
                        if (subject == null || subject.isEmpty()) {
                            subject = "Unknown Subject";
                        }

                        String fieldValue =
                            "- " + description + " & " + subject;

                        System.out.println(
                            "Adding to embed: Due=" +
                            due +
                            ", Field=" +
                            fieldValue
                        );

                        // Log each field as it is added to the embed
                        if (due.matches("<t:\\d+:R>")) {
                            System.out.println(
                                "Adding Field: ## Relative Due Date, Value: " +
                                fieldValue
                            );
                            embedBuilder.addField(
                                "## Relative Due Date",
                                fieldValue,
                                false
                            );
                        } else {
                            System.out.println(
                                "Adding Field: ## " +
                                due +
                                ", Value: " +
                                fieldValue
                            );
                            embedBuilder.addField(
                                "## " + due,
                                fieldValue,
                                false
                            );
                        }
                    })
                    .then(
                        Mono.defer(() -> {
                            // Send the embed
                            return event
                                .reply()
                                .withEmbeds(embedBuilder.build());
                        })
                    );
            })
            .then(); // Ensure Mono<Void> is returned
    }
} // Close class
