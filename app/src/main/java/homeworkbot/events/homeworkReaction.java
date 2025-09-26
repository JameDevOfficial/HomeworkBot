package homeworkbot.events;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class homeworkReaction {

    private final String channelId;

    public homeworkReaction(String channelId) {
        this.channelId = channelId;
    }

    public void handle(ReactionAddEvent event) {
        event.getClient()
            .getChannelById(event.getChannelId())
            .ofType(ThreadChannel.class)
            .subscribe(threadChannel -> {
                if (isInHomeworkChannel(threadChannel)) {
                    event.getMessage().subscribe(message -> {
                        message.getGuild().subscribe(guild -> {
                            getRelevantMembers(message, guild)
                                .collectList()
                                .subscribe(mentionedMembers -> {
                                    int mentionedCount = mentionedMembers.size();
                                    int reactionCount = message.getReactions().size();
                                    printResult(reactionCount, mentionedCount);
                                });
                        });
                    });
                }
            });
    }

    private boolean isInHomeworkChannel(ThreadChannel threadChannel) {
        return threadChannel.getParentId()
                .map(parentId -> parentId.asString().equals(channelId))
                .orElse(false);
    }

    private Flux<Member> getRelevantMembers(Message message, discord4j.core.object.entity.Guild guild) {
        if (mentionsEveryoneOrHere(message)) {
            // Avoid calling getMembers() to prevent GUILD_MEMBERS intent error
            System.out.println("Cannot fetch all members for @everyone/@here without GUILD_MEMBERS intent.");
            return Flux.empty();
        } else {
            // Only mentioned users, non-bots, with roles
            List<String> mentionedUserIds = message.getUserMentionIds()
                    .stream()
                    .map(Snowflake::asString)
                    .distinct()
                    .collect(Collectors.toList());
    
            List<Mono<Member>> memberMonos = mentionedUserIds.stream()
                    .map(userId -> guild.getMemberById(Snowflake.of(userId)))
                    .collect(Collectors.toList());
    
            return Flux.merge(memberMonos)
                    .filter(member -> !member.isBot() && !member.getRoleIds().isEmpty());
        }
    }

    private boolean mentionsEveryoneOrHere(Message message) {
        String content = message.getContent();
        return content.contains("@everyone") || content.contains("@here");
    }

    private void printResult(int reactionCount, int mentionedCount) {
        if (reactionCount == mentionedCount) {
            System.out.println("Reaction count matches relevant users: " + reactionCount + " " + mentionedCount);
        } else {
            System.out.println("Reaction count does not match relevant users: " + reactionCount + " " + mentionedCount);
        }
    }
}
