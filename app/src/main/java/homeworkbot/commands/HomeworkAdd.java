package homeworkbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.entity.channel.ThreadChannel.AutoArchiveDuration;
import discord4j.core.spec.ForumThreadMessageCreateSpec;
import discord4j.core.spec.StartThreadInForumChannelSpec;
import java.util.Collections;
import reactor.core.publisher.Mono;

//with help of https://github.com/Discord4J/Discord4J/blob/master/core/src/test/java/discord4j/core/ExampleForum.java
public class HomeworkAdd {

    private String tempDue;

    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GatewayDiscordClient client = event.getClient();

        var subcommandOptions = event
            .getOptions()
            .stream()
            .filter(option -> option.getName().equals("add"))
            .findFirst()
            .map(ApplicationCommandInteractionOption::getOptions)
            .orElse(Collections.emptyList());

        String title = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("title"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No title provided");

        String description = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("description"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse("No description provided");

        String dueRaw = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("due"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse(null);

        String subject = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("subject"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .orElse(null);

        Boolean remind = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("remind"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asBoolean())
            .orElse(false);

        String forUsers = subcommandOptions
            .stream()
            .filter(option -> option.getName().equals("for"))
            .findFirst()
            .flatMap(option -> option.getValue())
            .map(value -> value.asString())
            .map(s ->
                java.util.Arrays.stream(s.split("[, ]+"))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(part -> {
                        String id = part.replaceAll("[^0-9]", "");
                        return id.isEmpty() ? part : "<@" + id + ">";
                    })
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(" "))
            )
            .orElse(null);

        if (dueRaw != null) {
            // Match d.m or d.m.y
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "^(\\d{1,2})\\.(\\d{1,2})(?:\\.(\\d{2,4}))?$"
            ).matcher(dueRaw.trim());
            if (matcher.matches()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year;
            java.time.LocalDate today = java.time.LocalDate.now();
            if (matcher.group(3) != null) {
                year = Integer.parseInt(matcher.group(3));
                if (year < 100) year += 2000;
            } else {
                year = today.getYear();
            }
            java.time.LocalDate date;
            try {
                date = java.time.LocalDate.of(year, month, day);
                if (date.isBefore(today)) {
                date = java.time.LocalDate.of(year + 1, month, day);
                }
                long epochSeconds = date
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toEpochSecond();
                tempDue = "<t:" + epochSeconds + ":R>";
            } catch (Exception e) {
                tempDue = dueRaw + " (invalid date)";
            }
            } else {
            tempDue = dueRaw + " (invalid format)";
            }
        } else {
            tempDue = "No due date provided";
        }

        return Mono.justOrEmpty(event.getInteraction().getGuildId())
            .flatMap(guildId -> client.getGuildById(guildId))
            .flatMap(guild ->
            guild
                .getChannels()
                .filter(
                ch ->
                    ch.getName() != null &&
                    ch.getName().equalsIgnoreCase("homework")
                )
                .ofType(ForumChannel.class)
                .next()
            )
            .flatMap(forum -> {
            if (forum == null) {
                return event.editReply("Homework forum not found. Run /setup_homework first.").then(Mono.empty());
            }
            Snowflake tagId = null;
            if (subject != null) {
                tagId = forum
                .getAvailableTags()
                .stream()
                .filter(tag -> tag.getName().equalsIgnoreCase(subject))
                .findFirst()
                .map(discord4j.core.object.entity.ForumTag::getId)
                .orElse(null);
            }

            StringBuilder contentBuilder = new StringBuilder();

            if (!"No description provided".equals(description)) {
                contentBuilder.append("**Description:** ").append(description).append("\n");
            }
            contentBuilder.append("**Due:** ").append(tempDue).append("\n");

            if (forUsers != null) {
                contentBuilder.append("**For:** ").append(forUsers).append("\n");
            }
            if (subject != null) {
                contentBuilder.append("**Subject:** ").append(subject).append("\n");
            }
            contentBuilder.append("Remind: ").append(Boolean.toString(remind));

            StartThreadInForumChannelSpec.Builder builder =
                StartThreadInForumChannelSpec.builder()
                .autoArchiveDuration(AutoArchiveDuration.DURATION4)
                .name(title)
                .message(
                    ForumThreadMessageCreateSpec.builder()
                    .content(contentBuilder.toString())
                    .build()
                );
            if (tagId != null) {
                builder.addAppliedTag(tagId);
            }
            return forum.startThread(builder.build())
                .then(event.editReply("Homework posted: " + title));
            })
            .then();
    }
}
