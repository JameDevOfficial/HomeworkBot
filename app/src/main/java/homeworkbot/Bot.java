package homeworkbot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

public class Bot {

    public static void main(String[] args) {
        String token = System.getenv("TOKEN");
        DiscordClient client = DiscordClient.create(token);
        Mono<Void> login = client.withGateway(
                (GatewayDiscordClient gateway) -> gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                    final User self = event.getSelf();
                    System.out.printf("Logged in as %s", self.getTag());
                })));
        login.block();
    }

    public void onLogin(DiscordClient client) {
        // client.
    }
}
