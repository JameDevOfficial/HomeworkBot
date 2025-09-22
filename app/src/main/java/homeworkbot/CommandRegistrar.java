package homeworkbot;

import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.core.object.command.ApplicationCommandOption;

public class CommandRegistrar {
    private final GatewayDiscordClient client;

    public CommandRegistrar(GatewayDiscordClient client) {
        this.client = client;
    }

    public void registerCommands() {
        long applicationId = client.getRestClient().getApplicationId().block();

        //ping
        ApplicationCommandRequest addHomeworkCmd = ApplicationCommandRequest.builder()
                .name("add_homework")
                .description("Add a homework to the database")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("title")
                        .description("The title of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("due")
                        .description("d.m(.y)")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("description")
                        .description("Short description of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("subject")
                        .description("The subject of the homework")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("for")
                        .description("Ping @people or @roles with that homework or empty for everyone")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("remind")
                        .description("Choose if the bot should remind everyone to do their homework (default: true)")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .build();

        client.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, 1419672961090322545L, 
                        addHomeworkCmd)
                .subscribe();
    }
}