package ru.yandex.personal.mail.search.metrics.scraper.utils;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.personal.mail.search.metrics.scraper.model.mail.search.MailSearchMessageSnippet;

public class DeserializableMailSearchMessageSnippet extends MailSearchMessageSnippet {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeserializableMailSearchMessageSnippet(
            @JsonProperty("subject") String subject,
            @JsonProperty("snippet") String snippet,
            @JsonProperty("sender") String sender,
            @JsonProperty("date") Instant date)
    {
        super(subject, snippet, sender, date);
    }
}
