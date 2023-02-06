package ru.yandex.market.clab.common.service.photo;

import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;

import java.util.Collection;
import java.util.List;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class EditedPhotoRepositoryStub extends PhotoRepositoryStub<EditedPhoto> implements EditedPhotoRepository {
    @Override
    protected void setId(EditedPhoto photo, Long id) {
        photo.setId(id);
    }

    @Override
    protected Long getId(EditedPhoto photo) {
        return photo.getId();
    }

    @Override
    protected Long getGoodId(EditedPhoto photo) {
        return photo.getGoodId();
    }

    @Override
    protected EditedPhoto clone(EditedPhoto photo) {
        return new EditedPhoto(photo);
    }

    @Override
    public List<EditedPhoto> getNotUploadedToMboPhotos() {
        return filter(this::notUploadedToMbo);
    }

    @Override
    public List<EditedPhoto> getNotUploadedToS3Photos() {
        return filter(this::notUploadedToS3);
    }

    @Override
    public List<EditedPhoto> getNotUploaded(Collection<Long> goodIds) {
        return filter(p -> goodIds.contains(getGoodId(p)) && (notUploadedToS3(p) || notUploadedToMbo(p)));
    }

    private boolean notUploadedToMbo(EditedPhoto p) {
        return p.getUploadedTs() == null;
    }

    private boolean notUploadedToS3(EditedPhoto p) {
        return p.getS3UploadedTs() == null;
    }

}
