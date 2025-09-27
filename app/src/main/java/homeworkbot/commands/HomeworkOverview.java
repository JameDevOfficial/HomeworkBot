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
                @SuppressWarnings("unchecked") // not a good solution
                java.util.List<Snowflake> reactors = (java.util.List<
                        Snowflake
                    >) data[1];

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
                    .title("Homework Overview ");

                java.util.Map<String, StringBuilder> dateTaskMap =
                    new java.util.HashMap<>();

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
                                        description = line.substring(16).trim();
                                    } else if (lower.startsWith("**due:**")) {
                                        due = line.substring(8).trim();
                                    } else if (
                                        lower.startsWith("**subject:**")
                                    ) {
                                        subject = line.substring(12).trim();
                                    }
                                }

                                if (
                                    description == null || description.isEmpty()
                                ) {
                                    description = content;
                                }

                                if (
                                    description == null || description.isEmpty()
                                ) {
                                    description = "No Description";
                                }
                                if (due == null || due.isEmpty()) {
                                    due = "No Due Date";
                                }
                                if (subject == null || subject.isEmpty()) {
                                    subject = "Unknown Subject";
                                }

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
                        String due = (String) info[1];
                        String description = (String) info[2];
                        String subject = (String) info[3];

                        if (due == null || due.isEmpty()) {
                            due = "No Due Date";
                        }
                        if (subject == null || subject.isEmpty()) {
                            subject = "Unknown Subject";
                        }

                        ThreadChannel thread = (ThreadChannel) info[0];
                        String fieldValue =
                            "```" +
                            thread.getName() +
                            " (" +
                            description +
                            ")" +
                            "```";

                        String fieldKey = due;

                        if (due.matches("\\d+\\.\\d+\\.\\d+")) {
                            fieldKey = due;
                        } else if (due.matches("<t:\\d+:R>")) {
                            String timestampStr = due.replaceAll("[^\\d]", "");
                            try {
                                long seconds = Long.parseLong(timestampStr);
                                java.time.Instant instant =
                                    java.time.Instant.ofEpochSecond(seconds);
                                java.time.ZonedDateTime dateTime =
                                    instant.atZone(
                                        java.time.ZoneId.systemDefault()
                                    );
                                int day = dateTime.getDayOfMonth();
                                int month = dateTime.getMonthValue();
                                int year = dateTime.getYear();
                                fieldKey = day + "." + month + "." + year;
                            } catch (Exception e) {
                                fieldKey = "Invalid Date";
                            }
                        } else {
                            fieldKey = "No Due Date";
                        }

                        StringBuilder sb = dateTaskMap.getOrDefault(
                            fieldKey,
                            new StringBuilder()
                        );
                        if (sb.length() > 0) {
                            sb
                                .append("\n```")
                                .append(thread.getName())
                                .append(" (")
                                .append(description)
                                .append(")```");
                        } else {
                            sb.append(fieldValue);
                        }
                        dateTaskMap.put(fieldKey, sb);
                    })
                    .then(
                        Mono.defer(() -> {
                            java.util.List<String> sortedKeys =
                                new java.util.ArrayList<>(dateTaskMap.keySet());
                            sortedKeys.sort((a, b) -> {
                                if (a.equals("No Due Date")) return 1;
                                if (b.equals("No Due Date")) return -1;
                                try {
                                    java.text.SimpleDateFormat sdf =
                                        new java.text.SimpleDateFormat(
                                            "dd.MM.yyyy"
                                        );
                                    java.util.Date dateA = sdf.parse(
                                        a.contains(".") ? a : a + ".2025"
                                    );
                                    java.util.Date dateB = sdf.parse(
                                        b.contains(".") ? b : b + ".2025"
                                    );
                                    return dateA.compareTo(dateB);
                                } catch (Exception e) {
                                    return a.compareTo(b);
                                }
                            });

                            for (String key : sortedKeys) {
                                embedBuilder.addField(
                                    key.matches("\\d+\\.\\d+\\.\\d+")
                                        ? key
                                        : "Other",
                                    dateTaskMap
                                        .get(key)
                                        .toString()
                                        .replace("*", ""),
                                    false
                                );
                            }
                            return event
                                .reply()
                                .withEmbeds(embedBuilder.build());
                        })
                    );
            })
            .then();
    }
}
