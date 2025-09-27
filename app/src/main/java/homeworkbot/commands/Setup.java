package homeworkbot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.entity.channel.ThreadChannel.AutoArchiveDuration;
import discord4j.core.object.reaction.DefaultReaction;
import discord4j.core.spec.ForumChannelCreateSpec;
import discord4j.core.spec.ForumChannelEditSpec;
import discord4j.core.spec.ForumTagCreateSpec;
import discord4j.discordjson.json.DefaultReactionData;
import reactor.core.publisher.Mono;

public class Setup {

    private final Runnable postSetup;

    public Setup(Runnable postSetup) {
        this.postSetup = postSetup;
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if (event.getCommandName().equals("setup_homework")) {
            GatewayDiscordClient client = event.getClient();
            String[] tags = { "AM", "D", "E", "SYT", "SEW" };

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
                    event.editReply(
                        "Setup completed: Forum and tags configured."
                    )
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
        return Mono.empty(); // don't ack if not our command
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
        // Build a complete list of all tags (existing + new ones)
        var allTags = new java.util.ArrayList<ForumTagCreateSpec>();

        // Add all existing tags
        for (var tag : forum.getAvailableTags()) {
            allTags.add(
                ForumTagCreateSpec.builder().name(tag.getName()).build()
            );
        }

        // Track if we need to add any new tags
        boolean hasNewTags = false;

        // Add new tags that don't already exist
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

        // Only edit the forum if we have new tags to add
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
