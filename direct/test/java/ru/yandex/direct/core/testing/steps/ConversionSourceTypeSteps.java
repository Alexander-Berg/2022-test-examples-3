package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceType;
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode;
import ru.yandex.direct.core.entity.conversionsourcetype.repository.ConversionSourceTypeRepository;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.conversionsourcetype.service.validadation.ConversionSourceTypeValidationService.MAX_DESCRIPTION_LENGTH;
import static ru.yandex.direct.core.entity.conversionsourcetype.service.validadation.ConversionSourceTypeValidationService.MAX_NAME_LENGTH;

@Component
public class ConversionSourceTypeSteps {

    public static final String DEFAULT_NAME = "default_name";
    public static final String DRAFT_NAME = "draft_name";
    public static final String INVALID_NAME = "a".repeat(MAX_NAME_LENGTH + 1);

    public static final String DEFAULT_NAME_EN = "default_name_en";
    public static final String INVALID_NAME_EN = "a".repeat(MAX_NAME_LENGTH + 1);
    public static final String INVALID_NAME_EN_WITH_CYRILLIC = "не валидное имя";

    public static final String DESCRIPTION = "description";
    public static final String INVALID_DESCRIPTION = "a".repeat(MAX_DESCRIPTION_LENGTH + 1);

    public static final String DESCRIPTION_EN = "description_en";
    public static final String INVALID_DESCRIPTION_EN = "a".repeat(MAX_DESCRIPTION_LENGTH + 1);
    public static final String INVALID_DESCRIPTION_EN_WITH_CYRILLIC = "не валидное описание";

    public static final String VALID_URL = "https://yandex.ru/";
    public static final String BROKEN_URL = "https://invalid_url";

    private static final AtomicLong POSITION_COUNTER = new AtomicLong(10000L);

    @Autowired
    private ConversionSourceTypeRepository repository;

    public ConversionSourceTypeSteps(ConversionSourceTypeRepository repository) {
        this.repository = repository;
    }

    public ConversionSourceType getDefaultConversionSourceType() {
        return new ConversionSourceType()
                .withName(DEFAULT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(false)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);
    }

    public ConversionSourceType getDefaultConversionSourceTypeWithEn() {
        return new ConversionSourceType()
                .withName(DEFAULT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(false)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withNameEn(DEFAULT_NAME_EN)
                .withDescriptionEn(DESCRIPTION_EN);
    }

    public ConversionSourceType getDraftConversionSourceType() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);
    }

    public ConversionSourceType getDraftConversionSourceTypeWithEn() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withNameEn(DEFAULT_NAME_EN)
                .withDescriptionEn(DESCRIPTION_EN);
    }

    public ConversionSourceType getNotEditableConversionSourceType() {
        return new ConversionSourceType()
                .withName(DEFAULT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(false)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.METRIKA)
                .withIsEditable(false);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidName() {
        return new ConversionSourceType()
                .withName(INVALID_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidNameEn() {
        return new ConversionSourceType()
                .withName(DEFAULT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withNameEn(INVALID_NAME_EN);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidNameEnCyrillic() {
        return new ConversionSourceType()
                .withName(DEFAULT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withNameEn(INVALID_NAME_EN_WITH_CYRILLIC);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidDescription() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(INVALID_DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withDescriptionEn(DESCRIPTION_EN);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidDescriptionEn() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withDescriptionEn(INVALID_DESCRIPTION_EN);
    }

    public ConversionSourceType getConversionSourceTypeWithInvalidDescriptionEnWithCyrillic() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true)
                .withDescriptionEn(INVALID_DESCRIPTION_EN_WITH_CYRILLIC);
    }

    public ConversionSourceType getConversionSourceTypeWithBrokenIconUrl() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(BROKEN_URL)
                .withActivationUrl(VALID_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);
    }

    public ConversionSourceType getConversionSourceTypeWithBrokenActivationUrl() {
        return new ConversionSourceType()
                .withName(DRAFT_NAME)
                .withDescription(DESCRIPTION)
                .withIconUrl(VALID_URL)
                .withActivationUrl(BROKEN_URL)
                .withIsDraft(true)
                .withPosition(POSITION_COUNTER.incrementAndGet())
                .withCode(ConversionSourceTypeCode.OTHER)
                .withIsEditable(true);
    }

    public List<ConversionSourceType> getConversionSourceTypeByIds(List<Long> ids) {
        return repository.getConversionSourceTypeByIds(ids);
    }

    public ConversionSourceType addConversionSourceTypeAndReturnAdded(ConversionSourceType conversionSourceType) {
        Long id = repository.add(singletonList(conversionSourceType)).get(0);
        return repository.getConversionSourceTypeByIds(singletonList(id)).get(0);
    }

    public ConversionSourceType addDefaultConversionSourceType() {
        ConversionSourceType conversionSourceType = getDefaultConversionSourceType();
        return addConversionSourceTypeAndReturnAdded(conversionSourceType);
    }

    public ConversionSourceType addDefaultConversionSourceTypeWithEn() {
        ConversionSourceType conversionSourceType = getDefaultConversionSourceTypeWithEn();
        return addConversionSourceTypeAndReturnAdded(conversionSourceType);
    }

    public ConversionSourceType addDraftConversionSourceType() {
        ConversionSourceType conversionSourceType = getDraftConversionSourceType();
        return addConversionSourceTypeAndReturnAdded(conversionSourceType);
    }

    public ConversionSourceType addNotEditableConversionSourceType() {
        ConversionSourceType conversionSourceType = getNotEditableConversionSourceType();
        return addConversionSourceTypeAndReturnAdded(conversionSourceType);
    }

    public ConversionSourceType updateConversionSourceTypeAndReturnUpdated(ConversionSourceType conversionSourceType) {
        ModelChanges<ConversionSourceType> modelChanges = new ModelChanges<>(conversionSourceType.getId(),
                ConversionSourceType.class);
        modelChanges.processNotNull(conversionSourceType.getName(), ConversionSourceType.NAME);
        modelChanges.processNotNull(conversionSourceType.getDescription(), ConversionSourceType.DESCRIPTION);
        modelChanges.process(conversionSourceType.getIconUrl(), ConversionSourceType.ICON_URL);
        modelChanges.process(conversionSourceType.getActivationUrl(), ConversionSourceType.ACTIVATION_URL);
        modelChanges.processNotNull(conversionSourceType.getIsDraft(), ConversionSourceType.IS_DRAFT);
        modelChanges.processNotNull(conversionSourceType.getPosition(), ConversionSourceType.POSITION);
        modelChanges.processNotNull(conversionSourceType.getCode(), ConversionSourceType.CODE);
        modelChanges.processNotNull(conversionSourceType.getIsEditable(), ConversionSourceType.IS_EDITABLE);
        modelChanges.processNotNull(conversionSourceType.getNameEn(), ConversionSourceType.NAME_EN);
        modelChanges.processNotNull(conversionSourceType.getDescriptionEn(), ConversionSourceType.DESCRIPTION_EN);

        AppliedChanges<ConversionSourceType> appliedChanges =
                modelChanges.applyTo(repository.getConversionSourceTypeByIds(singletonList(conversionSourceType.getId())).get(0));
        Long id = repository.update(singletonList(appliedChanges)).get(0);
        return repository.getConversionSourceTypeByIds(singletonList(id)).get(0);
    }

    public void removeConversionSourceType(ConversionSourceType conversionSourceType) {
        repository.remove(singletonList(conversionSourceType.getId()));
    }

    public List<ModelChanges<ConversionSourceType>> getModelChanges(List<ConversionSourceType> conversionSourceTypes) {
        return StreamEx.of(conversionSourceTypes).map(conversionSourceType ->
                new ModelChanges<>(conversionSourceType.getId(), ConversionSourceType.class)
                        .processNotNull(conversionSourceType.getName(), ConversionSourceType.NAME)
                        .processNotNull(conversionSourceType.getDescription(), ConversionSourceType.DESCRIPTION)
                        .process(conversionSourceType.getIconUrl(), ConversionSourceType.ICON_URL)
                        .process(conversionSourceType.getActivationUrl(), ConversionSourceType.ACTIVATION_URL)
                        .processNotNull(conversionSourceType.getIsDraft(), ConversionSourceType.IS_DRAFT)
                        .processNotNull(conversionSourceType.getPosition(), ConversionSourceType.POSITION)
                        .processNotNull(conversionSourceType.getCode(), ConversionSourceType.CODE)
                        .processNotNull(conversionSourceType.getIsEditable(), ConversionSourceType.IS_EDITABLE)
                        .processNotNull(conversionSourceType.getNameEn(), ConversionSourceType.NAME_EN)
                        .processNotNull(conversionSourceType.getDescriptionEn(), ConversionSourceType.DESCRIPTION_EN)
        ).toList();
    }
}
