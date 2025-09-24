package homeworkbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.spec.ForumThreadMessageCreateSpec;
import discord4j.core.spec.StartThreadInForumChannelSpec;
import discord4j.core.spec.ForumChannelEditSpec;
import reactor.core.publisher.Mono;

//To implement: https://github.com/Discord4J/Discord4J/blob/master/core/src/test/java/discord4j/core/ExampleForum.java
public class AddHomework {

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();
        if (event.getCommandName().equals("add_homework")) {
            event.deferReply();
            String title = event
            .getOption("title")
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No title provided");

            String description = event
            .getOption("description")
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No description provided");

            String due = event
            .getOption("due")
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No due date provided");

            String subject = event
            .getOption("subject")
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No subject provided");

            Boolean remind = event
            .getOption("remind")
            .flatMap(option -> option.getValue())
            .map(value -> value.asBoolean())
            .orElse(false);

            String forUsers = event
            .getOption("for")
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .map(s ->
                java.util.Arrays.stream(s.split("[, ]+"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> {
                    String id = part.replaceAll("[^0-9]", "");
                    return id.isEmpty() ? part : "<@" + id + ">";
                })
                .distinct()
                .collect(java.util.stream.Collectors.joining(" "))
            )
            .orElse("No users set");

            return client
            .getChannelById(Snowflake.of(channelId))
            .ofType(ForumChannel.class)
            .flatMap(forum -> {
                // Try to find tag by subject name
                Snowflake tagId = forum.getAvailableTags().stream()
                .filter(tag -> tag.getName().equalsIgnoreCase(subject))
                .findFirst()
                .map(discord4j.core.object.entity.ForumTag::getId)
                .orElse(null);

                Mono<Snowflake> tagMono;
                if (subject != null && !subject.equals("No subject provided") && tagId == null) {
                // Create tag if not found
                tagMono = forum.edit(
                    ForumChannelEditSpec.builder()
                        .addAvailableTag(discord4j.core.spec.ForumTagCreateSpec.builder().name(subject).build())
                        .build()
                )
                .map(updatedForum -> updatedForum.getAvailableTags().stream()
                    .filter(tag -> tag.getName().equalsIgnoreCase(subject))
                    .findFirst()
                    .map(discord4j.core.object.entity.ForumTag::getId)
                    .orElse(null)
                    );
                } else {
                tagMono = Mono.just(tagId);
                }

                return tagMono.flatMap(finalTagId -> {
                StartThreadInForumChannelSpec.Builder builder =
                    StartThreadInForumChannelSpec.builder()
                    .name(title)
                    .message(
                        ForumThreadMessageCreateSpec.builder()
                        .content(
                            "**Description:** " +
                            description +
                            "\n**Due:** " +
                            due +
                            "\n**For:** " +
                            forUsers +
                            "\n**Subject:** " +
                            subject +
                            "\nRemind: " +
                            Boolean.toString(remind)
                        )
                        .build()
                    );
                if (finalTagId != null) {
                    builder.addAppliedTag(finalTagId);
                }
                return forum.startThread(builder.build());
                });
            })
            .then(event.reply("Homework posted: " + title));
        }
        return event.deferReply();
    }

    String channelId = "1419673001531805836";
}
