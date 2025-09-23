package homeworkbot;

import homeworkbot.CommandRegistrar;
import homeworkbot.commands.*;

import java.util.Map;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandData;


public class Bot {

    public static void main(String[] args) {
        String token = System.getenv("TOKEN");
        GatewayDiscordClient client = DiscordClientBuilder.create(token).build().login().block();
        Bot.init(client);

        client.onDisconnect().block();

    }

    public static void init(GatewayDiscordClient client) {

        EventDispatcher eventDispatcher = client.getEventDispatcher();
        eventDispatcher.on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out
                            .println(String.format("Logged in as %s", self.getTag()));
                });
        CommandRegistrar commandRegistrar = new CommandRegistrar(client);
        commandRegistrar.registerCommands();
        AddHomework addHomework = new AddHomework();

        client.on(ChatInputInteractionEvent.class, event -> {
            if (event.getCommandName().equals("add_homework")) {
                return addHomework.handle(event);
            }
            return event.deferReply().withEphemeral(true);
        }).subscribe();
    }

    public static void deleteCommand(String name, GatewayDiscordClient client) {
        long applicationId = client.getRestClient().getApplicationId().block();
        long guildId = 1419672961090322545L;

        Map<String, ApplicationCommandData> discordCommands = client.getRestClient()
                .getApplicationService()
                .getGuildApplicationCommands(applicationId,
                        guildId)
                .collectMap(ApplicationCommandData::name)
                .block();

        long commandId = discordCommands.get(name).id().asLong();

        client.getRestClient().getApplicationService()
                .deleteGuildApplicationCommand(applicationId, guildId, commandId)
                .subscribe();

    }

}
