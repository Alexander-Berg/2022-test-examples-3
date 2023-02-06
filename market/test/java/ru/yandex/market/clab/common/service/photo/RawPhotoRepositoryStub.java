package ru.yandex.market.clab.common.service.photo;

import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;

import java.util.Collection;
import java.util.List;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class RawPhotoRepositoryStub extends PhotoRepositoryStub<RawPhoto> implements RawPhotoRepository {
    @Override
    protected void setId(RawPhoto photo, Long id) {
        photo.setId(id);
    }

    @Override
    protected Long getId(RawPhoto photo) {
        return photo.getId();
    }

    @Override
    public List<RawPhoto> getNotUploadedPhotos() {
        return filter(this::notUploaded);
    }

    public List<RawPhoto> getNotUploadedPhotos(Collection<Long> goodIds) {
        return filter(
            p -> goodIds.contains(getGoodId(p)) && notUploaded(p)
        );
    }

    private boolean notUploaded(RawPhoto photo) {
        return photo.getUploadedTs() == null;
    }

    @Override
    protected Long getGoodId(RawPhoto photo) {
        return photo.getGoodId();
    }

    @Override
    protected RawPhoto clone(RawPhoto photo) {
        return new RawPhoto(photo);
    }
}
