package ru.yandex.personal.mail.search.metrics.scraper.utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import ru.yandex.personal.mail.search.metrics.scraper.model.mail.search.MailSearchScrapedData;

public class DeserializableMailSearchScrapedData extends MailSearchScrapedData {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeserializableMailSearchScrapedData(
            @JsonProperty("foundAllAverageSize") int foundAllAverageSize,
            @JsonProperty("snippets") List<DeserializableMailSearchMessageSnippet> snippets)
    {
        super(foundAllAverageSize, Lists.newArrayList(snippets));
    }
}
