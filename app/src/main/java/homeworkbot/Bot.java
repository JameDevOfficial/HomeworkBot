package homeworkbot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

public class Bot {

    public static void main(String[] args) {
        String token = System.getenv("TOKEN");
        System.out.println(token);
        DiscordClient client = DiscordClient.create(token);
        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> Mono.empty());
        login.block();
    }
}
