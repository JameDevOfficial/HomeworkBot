package homeworkbot;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class CommandRegistrar {

    private final GatewayDiscordClient client;

    public CommandRegistrar(GatewayDiscordClient client) {
        this.client = client;
    }

    public void registerCommands() {
        long applicationId = client.getRestClient().getApplicationId().block();

        //add_homework
        ApplicationCommandRequest addHomeworkCmd =
            ApplicationCommandRequest.builder()
                .name("add_homework")
                .description("Add a homework to the database")
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("title")
                        .description("The title of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("due")
                        .description("d.m(.y)")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .minLength(3)
                        .maxLength(10)
                        .required(true)
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("description")
                        .description("Short description of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("subject")
                        .description("The subject of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("Math")
                                .value("math")
                                .build()
                        )
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("English")
                                .value("english")
                                .build()
                        )
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("Biology")
                                .value("biology")
                                .build()
                        )
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("Chemistry")
                                .value("chemistry")
                                .build()
                        )
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("Physics")
                                .value("physics")
                                .build()
                        )
                        .addChoice(
                            discord4j.discordjson.json.ApplicationCommandOptionChoiceData.builder()
                                .name("History")
                                .value("history")
                                .build()
                        )
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("for")
                        .description(
                            "Ping @people or @roles with that homework or empty for everyone"
                        )
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build()
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                        .name("remind")
                        .description(
                            "Choose if the bot should remind everyone to do their homework (default: true)"
                        )
                        .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                        .required(false)
                        .build()
                )
                .build();

        //setup
        ApplicationCommandRequest setupCmd = ApplicationCommandRequest.builder()
            .name("setup_homework")
            .description("Setup the homework forum")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("custom")
                    .description("Use custom setup")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                    .required(false)
                    .build()
            )   
            .build();
        client
            .getRestClient()
            .getApplicationService()
            .createGuildApplicationCommand(
                applicationId,
                1419672961090322545L,
                addHomeworkCmd
            )
            .subscribe();

        client
            .getRestClient()
            .getApplicationService()
            .createGuildApplicationCommand(
                applicationId,
                1419672961090322545L,
                setupCmd
            )
            .subscribe();
    }
}
