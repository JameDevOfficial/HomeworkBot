package homeworkbot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

//just an example for me
public class Ping {
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if (event.getCommandName().equals("ping")) {
            return event.reply("Pong!");
        }
        return event.deferReply();
    }
}
