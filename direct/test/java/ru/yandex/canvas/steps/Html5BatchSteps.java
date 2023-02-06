package ru.yandex.canvas.steps;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.SessionParams;

import static java.util.Collections.singletonList;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_YNDX_FRONTPAGE;

@Lazy(false)
@Service
@ParametersAreNullableByDefault
public class Html5BatchSteps {

    @Autowired
    private BatchesRepository repository;

    @Autowired
    private SequenceService sequenceService;

    public Batch createBatch(long clientId, @Nonnull Batch batch) {
        return repository.createBatch(batch.setClientId(clientId));
    }

    public Batch getBatch(long clientId, String batchId, SessionParams.SessionTag productType) {
        SessionParams.SessionTag converted = productType == CPM_BANNER ? null : productType;
        return repository.getBatchById(clientId, batchId, converted);
    }

    public Optional<Batch> getArchiveBatch(long clientId, String batchId) {
        return StreamEx
                .of(repository.getBatchesByQuery(clientId, Integer.MAX_VALUE, 0, Sort.Direction.ASC, true, ""))
                .findFirst(b -> b.getId().equals(batchId));
    }

    public Batch defaultBatch(long clientId) {
        return new Batch().setName("Default batch")
                .setProductType(CPM_BANNER)
                .setDate(LocalDateTime.now())
                .setArchive(false)
                .setClientId(clientId)
                .setCreatives(singletonList(defaultCreative()));
    }

    public Batch defaultYndxFrontpageBatch(long clientId) {
        Batch source = defaultBatch(clientId).setProductType(CPM_YNDX_FRONTPAGE);
        source.getCreatives().get(0).setWidth(728).setHeight(90);
        return source;
    }

    public Creative defaultCreative() {
        Long creativeId = sequenceService.getNextCreativeIdsList(1).get(0);
        return new Creative()
                .setId(creativeId)
                .setWidth(240)
                .setHeight(400);
    }
}
