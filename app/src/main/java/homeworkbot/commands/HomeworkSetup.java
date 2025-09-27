package homeworkbot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.reaction.DefaultReaction;
import discord4j.core.spec.ForumChannelCreateSpec;
import discord4j.core.spec.ForumChannelEditSpec;
import discord4j.core.spec.ForumTagCreateSpec;
import discord4j.discordjson.json.DefaultReactionData;
import reactor.core.publisher.Mono;

public class HomeworkSetup {

    private final Runnable postSetup;

    public HomeworkSetup(Runnable postSetup) {
        this.postSetup = postSetup;
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();
        String[] tags = {
            "AM",
            "D",
            "E",
            "SYT",
            "SEW",
            "NWTK",
            "ITSI",
            "NW",
            "MEDT",
            "ITP",
        };

        return event
            .deferReply()
            .then(getOrCreateHomeworkForum(event, client, "homework"))
            .flatMap(forumChannel -> createAllTags(forumChannel, tags))
            .then(
                Mono.fromRunnable(() -> {
                    if (postSetup != null) postSetup.run();
                })
            )
            .then(
                event.editReply("Setup completed: Forum and tags configured.")
            )
            .doOnError(error -> {
                System.err.println(
                    "Error in setup command: " + error.getMessage()
                );
                error.printStackTrace();
            })
            .onErrorResume(error ->
                event.editReply("Setup failed: " + error.getMessage())
            )
            .then();
    }

    private Mono<ForumChannel> getOrCreateHomeworkForum(
        ChatInputInteractionEvent event,
        GatewayDiscordClient client,
        String forumName
    ) {
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).flatMap(
            guildId ->
                client
                    .getGuildById(guildId)
                    .flatMap(guild ->
                        guild
                            .getChannels()
                            .filter(
                                channel ->
                                    channel instanceof ForumChannel &&
                                    channel
                                        .getName()
                                        .equalsIgnoreCase(forumName)
                            )
                            .cast(ForumChannel.class)
                            .next()
                            .switchIfEmpty(
                                guild
                                    .createForumChannel(
                                        ForumChannelCreateSpec.builder()
                                            .name(forumName)
                                            .defaultAutoArchiveDurationOrNull(
                                                10080
                                            ) // DURATION4 is 1440 minutes
                                            .defaultReactionEmojiOrNull(
                                                new DefaultReaction(
                                                    client,
                                                    DefaultReactionData.builder()
                                                        .emojiName("✅")
                                                        .build()
                                                )
                                            )
                                            .build()
                                    )
                                    .doOnSuccess(forum ->
                                        System.out.println(
                                            "Forum created: " + forumName
                                        )
                                    )
                            )
                    )
        );
    }

    private Mono<Void> createAllTags(ForumChannel forum, String[] tagNames) {
        var allTags = new java.util.ArrayList<ForumTagCreateSpec>();

        for (var tag : forum.getAvailableTags()) {
            allTags.add(
                ForumTagCreateSpec.builder().name(tag.getName()).build()
            );
        }

        boolean hasNewTags = false;

        for (String tagName : tagNames) {
            boolean tagExists = forum
                .getAvailableTags()
                .stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));

            if (!tagExists) {
                allTags.add(ForumTagCreateSpec.builder().name(tagName).build());
                System.out.println("Adding tag: " + tagName);
                hasNewTags = true;
            } else {
                System.out.println("Tag already exists: " + tagName);
            }
        }

        if (hasNewTags) {
            return forum
                .edit(
                    ForumChannelEditSpec.builder()
                        .availableTags(allTags)
                        .build()
                )
                .doOnSuccess(updatedForum ->
                    System.out.println("All tags updated successfully")
                )
                .doOnError(error ->
                    System.err.println(
                        "Error updating forum tags: " + error.getMessage()
                    )
                )
                .then();
        } else {
            System.out.println("No new tags to add");
            return Mono.empty();
        }
    }
}
