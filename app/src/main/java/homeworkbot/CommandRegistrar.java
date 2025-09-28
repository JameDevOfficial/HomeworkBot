package homeworkbot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.util.ArrayList;
import java.util.List;

public class CommandRegistrar {

    private final GatewayDiscordClient client;
    private static final long GUILD_ID = 1419672961090322545L;

    public CommandRegistrar(GatewayDiscordClient client) {
        this.client = client;
    }

    public void registerCommands() {
        long applicationId = client.getRestClient().getApplicationId().block();

        List<
            discord4j.discordjson.json.ApplicationCommandOptionChoiceData
        > tagChoices = new ArrayList<>();
        client
            .getGuildById(Snowflake.of(GUILD_ID))
            .flatMapMany(guild -> guild.getChannels())
            .filter(
                channel ->
                    channel.getName() != null &&
                    channel.getName().equalsIgnoreCase("homework")
            )
            .ofType(ForumChannel.class)
            .next()
            .flatMapMany(channel ->
                reactor.core.publisher.Flux.fromIterable(
                    channel.getAvailableTags()
                )
            )
            .map(tag ->
                discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                    .name(tag.getName())
                    .value(tag.getName())
                    .build()
            )
            .collectList()
            .blockOptional()
            .ifPresent(tagChoices::addAll);

        var subjectOptionBuilder = ApplicationCommandOptionData.builder()
            .name("subject")
            .description("The subject of the homework")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(false);

        for (discord4j.discordjson.json.ApplicationCommandOptionChoiceData choice : tagChoices) {
            subjectOptionBuilder.addChoice(choice);
        }

        ApplicationCommandRequest pingCmd = ApplicationCommandRequest.builder()
            .name("ping")
            .description("Homework related commands")
            .build();
        // Homework Overview command as a subcommand group
        ApplicationCommandRequest homeworkCmd =
            ApplicationCommandRequest.builder()
                .name("homework")
                .description("Homework related commands")
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .type(
                            ApplicationCommandOption.Type.SUB_COMMAND.getValue()
                        )
                        .name("overview")
                        .description("Show an overview of all homework")
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .type(
                            ApplicationCommandOption.Type.SUB_COMMAND.getValue()
                        )
                        .name("setup")
                        .description("Setup the homework forum")
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("custom")
                                .description("Use custom setup")
                                .type(
                                    ApplicationCommandOption.Type.BOOLEAN.getValue()
                                )
                                .required(false)
                                .build()
                        )
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .type(
                            ApplicationCommandOption.Type.SUB_COMMAND.getValue()
                        )
                        .name("add")
                        .description("Add a homework to the database")
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("title")
                                .description("The title of the homework")
                                .type(
                                    ApplicationCommandOption.Type.STRING.getValue()
                                )
                                .required(true)
                                .build()
                        )
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("due")
                                .description("d.m(.y)")
                                .type(
                                    ApplicationCommandOption.Type.STRING.getValue()
                                )
                                .minLength(3)
                                .maxLength(10)
                                .required(true)
                                .build()
                        )
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("description")
                                .description(
                                    "Short description of the homework"
                                )
                                .type(
                                    ApplicationCommandOption.Type.STRING.getValue()
                                )
                                .required(false)
                                .build()
                        )
                        .addOption(subjectOptionBuilder.build())
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("for")
                                .description(
                                    "Ping @people or @roles with that homework or empty for everyone"
                                )
                                .type(
                                    ApplicationCommandOption.Type.STRING.getValue()
                                )
                                .required(false)
                                .build()
                        )
                        .addOption(
                            ApplicationCommandOptionData.builder()
                                .name("remind")
                                .description(
                                    "Choose if the bot should remind everyone to do their homework (default: true)"
                                )
                                .type(
                                    ApplicationCommandOption.Type.BOOLEAN.getValue()
                                )
                                .required(false)
                                .build()
                        )
                        .build()
                )
                .build();
        client
            .getRestClient()
            .getApplicationService()
            .createGuildApplicationCommand(applicationId, GUILD_ID, homeworkCmd)
            .subscribe();
        client
            .getRestClient()
            .getApplicationService()
            .createGuildApplicationCommand(applicationId, GUILD_ID, pingCmd)
            .subscribe();
    }
}
