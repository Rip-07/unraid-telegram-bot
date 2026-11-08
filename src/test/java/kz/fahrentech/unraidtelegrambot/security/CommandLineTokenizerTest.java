package kz.fahrentech.unraidtelegrambot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import org.junit.jupiter.api.Test;

class CommandLineTokenizerTest {

    private final CommandLineTokenizer tokenizer = new CommandLineTokenizer();

    @Test
    void tokenizesQuotedArgumentsWithoutShellExecution() {
        assertThat(tokenizer.tokenize("echo \"hello world\" '/tmp/a b'"))
                .containsExactly("echo", "hello world", "/tmp/a b");
    }

    @Test
    void reportsUnclosedQuote() {
        assertThatThrownBy(() -> tokenizer.tokenize("echo \"broken"))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("незакрытую кавычку");
    }
}
