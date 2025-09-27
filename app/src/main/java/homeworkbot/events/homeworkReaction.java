package homeworkbot.events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.ThreadChannelEditSpec;

import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HomeworkReaction {

    public HomeworkReaction() {}

    public void handle(ReactionAddEvent event) {
        event
            .getClient()
            .getChannelById(event.getChannelId())
            .ofType(ThreadChannel.class)
            .flatMap(threadChannel ->
                threadChannel
                    .getParent()
                    .map(
                        parent ->
                            parent.getName() != null &&
                            parent.getName().equalsIgnoreCase("homework")
                    )
                    .defaultIfEmpty(false)
                    .flatMap(isHomework -> {
                        if (!isHomework) return Mono.empty();
                        return event
                            .getMessage()
                            .flatMap(message ->
                                message
                                    .getGuild()
                                    .map(guild -> {
                                        List<Snowflake> relevantMemberIds = getRelevantMembers(message, guild);
                                        long matchingReactions = relevantMemberIds.contains(event.getUserId()) ? 1 : 0;
                                        handleReaction((int) matchingReactions, relevantMemberIds.size(),threadChannel, event);
                                        printResult((int) matchingReactions, relevantMemberIds.size());
                                        return Mono.empty();
                                    })
                                    .flatMap(mono -> mono)
                            );
                    })
            )
            .subscribe();
    }

    private List<Snowflake> getRelevantMembers(
        Message message,
        discord4j.core.object.entity.Guild guild
    ) {
        if (mentionsEveryoneOrHere(message)) {
            // Avoid calling getMembers() to prevent GUILD_MEMBERS intent error
            System.out.println(
                "Cannot fetch all members for @everyone/@here without GUILD_MEMBERS intent."
            );
            return java.util.Collections.emptyList();
        } else {
            // Only mentioned users, non-bots, with roles
            List<String> mentionedUserIds = message
                .getUserMentionIds()
                .stream()
                .map(Snowflake::asString)
                .distinct()
                .collect(Collectors.toList());

            System.out.println("Mentioned user IDs: " + mentionedUserIds);

            List<Mono<Member>> memberMonos = mentionedUserIds
                .stream()
                .map(userId -> guild.getMemberById(Snowflake.of(userId)))
                .collect(Collectors.toList());

            return Flux.merge(memberMonos)
                .filter(member -> !member.isBot())
                .map(Member::getId)
                .collectList()
                .block();
        }
    }

    private boolean mentionsEveryoneOrHere(Message message) {
        String content = message.getContent();
        return content.contains("@everyone") || content.contains("@here");
    }

    private void handleReaction(int reactionCount, int mentionedCount, ThreadChannel channel, ReactionAddEvent event) {
        if (reactionCount >= mentionedCount) {
            channel.edit(ThreadChannelEditSpec.create().withArchived(true)).subscribe();
            System.out.println("Closed task");
        } 
        else {
            //maybe add sth in the future
        }
    }

    private void printResult(int reactionCount, int mentionedCount) {
        if (reactionCount == mentionedCount) {
            System.out.println(
                "Reaction count matches relevant users: " +
                reactionCount +
                " " +
                mentionedCount
            );
        } else {
            System.out.println(
                "Reaction count does not match relevant users: " +
                reactionCount +
                " " +
                mentionedCount
            );
        }
    }
}
