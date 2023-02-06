package ru.yandex.downloader.url;

import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.lang.Validate;

/**
 * @author akirakozov
 */
public class MulcaTargetId implements TargetReference {
    private final MulcaId mulcaId;

    public MulcaTargetId(MulcaId mulcaId) {
        Validate.isTrue(mulcaId.isOnlyStid());
        this.mulcaId = mulcaId;
    }

    public String getValue() {
        return mulcaId.toSerializedString();
    }
}
