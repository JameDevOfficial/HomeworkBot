package homeworkbot.commands;

import discord4j.core.object.entity.ForumTag;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.ForumThreadMessageCreateSpec;
import discord4j.core.spec.StartThreadInForumChannelSpec;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

//To implement: https://github.com/Discord4J/Discord4J/blob/master/core/src/test/java/discord4j/core/ExampleForum.java
public class AddHomework {
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();
        if (event.getCommandName().equals("add_homework")) {
            String title = event.getOption("title")
                    .flatMap(option -> option.getValue())
                    .map(value -> value.asString())
                    .orElse("No title provided");

            String description = event.getOption("description")
                    .flatMap(option -> option.getValue())
                    .map(value -> value.asString())
                    .orElse("No description provided");

            String due = event.getOption("due")
                    .flatMap(option -> option.getValue())
                    .map(value -> value.asString())
                    .orElse("No due date provided");

            return client.getChannelById(Snowflake.of(channelId))
                    .ofType(ForumChannel.class)
                    .flatMap(forum -> forum.startThread(StartThreadInForumChannelSpec.builder()
                            .name(title)
                            .message(ForumThreadMessageCreateSpec.builder()
                                    .content("**Description:** " + description + "\n**Due:** " + due)
                                    .build())
                            .build()))
                    .then(event.reply("Homework posted: " + title));
        }
        return event.deferReply();
    }

    String channelId = "1419673001531805836";

}
