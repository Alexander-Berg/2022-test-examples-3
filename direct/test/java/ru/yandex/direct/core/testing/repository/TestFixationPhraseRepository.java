package ru.yandex.direct.core.testing.repository;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.keyword.model.FixationPhrase;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.write.JooqWriter;
import ru.yandex.direct.jooqmapper.write.JooqWriterBuilder;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppcdict.tables.StopwordFixation.STOPWORD_FIXATION;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromProperty;

public class TestFixationPhraseRepository {

    private final DslContextProvider dslContextProvider;
    private final JooqWriter<FixationPhrase> stopwordFixationWriter;

    @Autowired
    public TestFixationPhraseRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
        this.stopwordFixationWriter = createMapper();
    }

    public void add(Collection<FixationPhrase> stopwordFixations) {
        new InsertHelper<>(dslContextProvider.ppcdict(), STOPWORD_FIXATION)
                .addAll(stopwordFixationWriter, stopwordFixations)
                .executeIfRecordsAdded();
    }

    private JooqWriter<FixationPhrase> createMapper() {
        return JooqWriterBuilder.<FixationPhrase>builder()
                .writeField(STOPWORD_FIXATION.PHRASE, fromProperty(FixationPhrase.PHRASE))
                .build();
    }
}
