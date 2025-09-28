package homeworkbot.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.reaction.DefaultReaction;
import discord4j.core.spec.ForumChannelCreateSpec;
import discord4j.core.spec.ForumChannelEditSpec;
import discord4j.core.spec.ForumTagCreateSpec;
import discord4j.discordjson.json.DefaultReactionData;
import java.util.Collections;
import reactor.core.publisher.Mono;

public class HomeworkSetup {

    private final Runnable postSetup;

    public HomeworkSetup(Runnable postSetup) {
        this.postSetup = postSetup;
    }

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();
        var subcommandOptions = event.getOptions().stream().filter(option -> option.getName().equals("setup")).findFirst().map(ApplicationCommandInteractionOption::getOptions).orElse(Collections.emptyList());

        String channelName = subcommandOptions.stream().filter(option -> option.getName().equals("name")).findFirst().flatMap(option -> option.getValue()).map(value -> value.asString()).orElse("homework");
        String[] tags = { "AM", "D", "E", "SYT", "SEW", "NWTK", "ITSI", "NW", "MEDT", "ITP" };
        System.out.println(channelName);
        return getOrCreateHomeworkForum(event, client, channelName)
            .flatMap(result -> {
                if (result.isExisted()) {
                    return event.editReply("Setup skipped: Homework forum already exists.");
                } else {
                    return createAllTags(result.getForumChannel(), tags)
                        .then(
                            Mono.fromRunnable(() -> {
                                if (postSetup != null) postSetup.run();
                            })
                        )
                        .then(event.editReply("Setup completed: Forum and tags configured."));
                }
            })
            .doOnError(error -> {
                System.err.println("Error in setup command: " + error.getMessage());
                error.printStackTrace();
            })
            .onErrorResume(error -> {
                // Check if the error is related to interaction already being acknowledged
                if (error.getMessage() != null && error.getMessage().contains("Interaction has already been acknowledged")) {
                    System.err.println("Interaction already acknowledged, skipping reply attempt");
                    return Mono.empty();
                }

                // Try to edit the reply for other errors
                return event
                    .editReply("Setup failed: " + error.getMessage())
                    .onErrorResume(editError -> {
                        System.err.println("Failed to edit reply: " + editError.getMessage());
                        return Mono.empty();
                    });
            })
            .then();
    }

    // Add this inner class to track if the forum existed or was created
    private static class ForumResult {

        private final ForumChannel forumChannel;
        private final boolean existed;

        public ForumResult(ForumChannel forumChannel, boolean existed) {
            this.forumChannel = forumChannel;
            this.existed = existed;
        }

        public ForumChannel getForumChannel() {
            return forumChannel;
        }

        public boolean isExisted() {
            return existed;
        }
    }

    private Mono<ForumResult> getOrCreateHomeworkForum(ChatInputInteractionEvent event, GatewayDiscordClient client, String forumName) {
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).flatMap(guildId ->
            client
                .getGuildById(guildId)
                .flatMap(guild ->
                    guild
                        .getChannels()
                        .filter(channel -> channel instanceof ForumChannel && channel.getName().equalsIgnoreCase(forumName))
                        .cast(ForumChannel.class)
                        .next()
                        .map(existingForum -> {
                            System.out.println("Forum already exists: " + forumName);
                            return new ForumResult(existingForum, true);
                        })
                        .switchIfEmpty(
                            guild
                                .createForumChannel(
                                    ForumChannelCreateSpec.builder()
                                        .name(forumName)
                                        .defaultAutoArchiveDurationOrNull(10080) // DURATION4 is 1440 minutes
                                        .defaultReactionEmojiOrNull(new DefaultReaction(client, DefaultReactionData.builder().emojiName("✅").build()))
                                        .build()
                                )
                                .map(newForum -> {
                                    System.out.println("Forum created: " + forumName);
                                    return new ForumResult(newForum, false);
                                })
                        )
                )
        );
    }

    private Mono<Void> createAllTags(ForumChannel forum, String[] tagNames) {
        var allTags = new java.util.ArrayList<ForumTagCreateSpec>();

        for (var tag : forum.getAvailableTags()) {
            allTags.add(ForumTagCreateSpec.builder().name(tag.getName()).build());
        }

        boolean hasNewTags = false;

        for (String tagName : tagNames) {
            boolean tagExists = forum.getAvailableTags().stream().anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));

            if (!tagExists) {
                allTags.add(ForumTagCreateSpec.builder().name(tagName).build());
                System.out.println("Adding tag: " + tagName);
                hasNewTags = true;
            } else {
                System.out.println("Tag already exists: " + tagName);
            }
        }

        if (hasNewTags) {
            return forum.edit(ForumChannelEditSpec.builder().availableTags(allTags).build()).doOnSuccess(updatedForum -> System.out.println("All tags updated successfully")).doOnError(error -> System.err.println("Error updating forum tags: " + error.getMessage())).then();
        } else {
            System.out.println("No new tags to add");
            return Mono.empty();
        }
    }
}
