package homeworkbot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;


//To implement: https://github.com/Discord4J/Discord4J/blob/master/core/src/test/java/discord4j/core/ExampleForum.java
public class AddHomework {
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if (event.getCommandName().equals("add_homework")) {
            String title = event.getOption("title")
                .flatMap(option -> option.getValue())
                .map(value -> value.asString())
                .orElse("No title provided");
            return event.reply("Handling homework soon! " + title);
        }
        return event.deferReply();
    }
}
