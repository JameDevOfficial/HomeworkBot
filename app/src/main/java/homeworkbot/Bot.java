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
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;
import reactor.core.publisher.Mono;

public class Bot {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String token = dotenv.get("TOKEN");
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
                HomeworkReaction reactionEvent = new HomeworkReaction();
                reactionEvent.handle(event);
            });
        CommandRegistrar commandRegistrar = new CommandRegistrar(client);
        commandRegistrar.registerCommands();
        HomeworkAdd homeworkAdd = new HomeworkAdd();
        HomeworkOverview homeworkOverview = new HomeworkOverview();
        HomeworkSetup homeworkSetup = new HomeworkSetup(() -> {
            new CommandRegistrar(client).registerCommands();
        });
        client
            .on(ChatInputInteractionEvent.class, event -> {
                long startTime = System.currentTimeMillis();

                return event
                    .deferReply()
                    .then(
                        Mono.defer(() -> {
                            String subcommand = event.getOptions().isEmpty()
                                ? ""
                                : event.getOptions().get(0).getName();
                            System.out.println(
                                event.getCommandName() +
                                " " +
                                subcommand +
                                " " +
                                event.getCommandType()
                            );
                            Mono<Void> handlerMono;
                            if (
                                event.getCommandName().equals("homework") &&
                                subcommand.equals("add")
                            ) {
                                handlerMono = homeworkAdd.handle(event);
                            } else if (
                                event.getCommandName().equals("homework") &&
                                subcommand.equals("setup")
                            ) {
                                handlerMono = homeworkSetup.handle(event);
                            } else if (
                                event.getCommandName().equals("homework") &&
                                subcommand.equals("overview")
                            ) {
                                handlerMono = homeworkOverview.handle(event);
                            }
                            else if (
                                event.getCommandName().equals("ping")
                            ) {
                                handlerMono = new Ping().handle(event);
                            } else {
                                handlerMono = Mono.empty();
                            }
                            return handlerMono
                                .then(
                                    Mono.defer(() -> {
                                        long endTime =
                                            System.currentTimeMillis();
                                        double seconds =
                                            (endTime - startTime) / 1000.0;
                                        return event
                                            .getReply()
                                            .flatMap(message -> {
                                                String currentContent =
                                                    message.getContent();
                                                String timingText =
                                                    "\n-# " + seconds + "s";
                                                if (
                                                    currentContent != null &&
                                                    currentContent
                                                        .toLowerCase()
                                                        .contains("homework bot is thinking...")
                                                ) {
                                                    return event.editReply(
                                                        timingText.trim()
                                                    );
                                                } else {
                                                    // Append timing to the existing message
                                                    System.out.print(currentContent + timingText);
                                                    return event.editReply(
                                                        currentContent +
                                                        timingText
                                                    );
                                                }
                                            });
                                    })
                                )
                                .then();
                        })
                    );
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
