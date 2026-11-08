package kz.fahrentech.unraidtelegrambot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void parsesCommandArgumentsAndRawArguments() {
        BotCommand command = parser.parse("/logs app 100", "my_bot").orElseThrow();

        assertThat(command.name()).isEqualTo("logs");
        assertThat(command.arguments()).containsExactly("app", "100");
        assertThat(command.rawArguments()).isEqualTo("app 100");
    }

    @Test
    void parsesCommandAddressedToThisBot() {
        BotCommand command = parser.parse("/status@My_Bot plex", "my_bot").orElseThrow();

        assertThat(command.name()).isEqualTo("status");
        assertThat(command.arguments()).containsExactly("plex");
    }

    @Test
    void ignoresCommandAddressedToAnotherBot() {
        assertThat(parser.parse("/status@other_bot plex", "my_bot")).isEmpty();
    }

    @Test
    void splitsFirstArgumentForExec() {
        CommandParser.SplitValue split = parser.splitFirstArgument("plex ls -la /tmp");

        assertThat(split.first()).isEqualTo("plex");
        assertThat(split.remainder()).isEqualTo("ls -la /tmp");
    }
}
