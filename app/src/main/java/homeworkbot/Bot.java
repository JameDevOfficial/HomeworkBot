package homeworkbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandData;
import homeworkbot.commands.*;
import homeworkbot.events.*;
import java.util.Map;
import reactor.core.publisher.Mono;

public class Bot {

    static String channelId;

    // Startup event to automatically get the "homework" channel ID
    static void fetchHomeworkChannelId(GatewayDiscordClient client) {
        client
            .getGuilds()
            .flatMap(guild -> guild.getChannels())
            .filter(channel -> "homework".equalsIgnoreCase(channel.getName()))
            .next()
            .map(channel -> channel.getId().asString())
            .doOnNext(id -> {
                channelId = id;
                System.out.println("Homework channel ID set to: " + channelId);
            })
            .block();
    }

    public static void main(String[] args) {
        String token = System.getenv("TOKEN");
        GatewayDiscordClient client = DiscordClientBuilder.create(token)
            .build()
            .login()
            .block();
        Bot.init(client);

        client.onDisconnect().block();
    }

    public static void init(GatewayDiscordClient client) {
        EventDispatcher eventDispatcher = client.getEventDispatcher();
        eventDispatcher
            .on(ReadyEvent.class)
            .subscribe(event -> {
                User self = event.getSelf();
                System.out.println(
                    String.format("Logged in as %s", self.getTag())
                );
            });

        eventDispatcher
            .on(ReactionAddEvent.class)
            .subscribe(event -> {
                homeworkReaction reactionEvent = new homeworkReaction(
                    channelId
                );
                reactionEvent.handle(event);
            });
        CommandRegistrar commandRegistrar = new CommandRegistrar(client);
        commandRegistrar.registerCommands();
        fetchHomeworkChannelId(client);
        AddHomework addHomework = new AddHomework(channelId);
        Setup setup = new Setup(channelId);
        client
            .on(ChatInputInteractionEvent.class, event -> {
                if (event.getCommandName().equals("add_homework")) {
                    return addHomework.handle(event);
                } else if (event.getCommandName().equals("setup_homework")) {
                    return setup.handle(event);
                }
                return Mono.empty();
            })
            .subscribe();
    }

    public static void deleteCommand(String name, GatewayDiscordClient client) {
        long applicationId = client.getRestClient().getApplicationId().block();
        long guildId = 1419672961090322545L;

        Map<String, ApplicationCommandData> discordCommands = client
            .getRestClient()
            .getApplicationService()
            .getGuildApplicationCommands(applicationId, guildId)
            .collectMap(ApplicationCommandData::name)
            .block();

        long commandId = discordCommands.get(name).id().asLong();

        client
            .getRestClient()
            .getApplicationService()
            .deleteGuildApplicationCommand(applicationId, guildId, commandId)
            .subscribe();
    }
}
