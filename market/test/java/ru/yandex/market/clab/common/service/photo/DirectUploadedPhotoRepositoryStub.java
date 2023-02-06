package ru.yandex.market.clab.common.service.photo;

import ru.yandex.market.clab.db.jooq.generated.tables.pojos.UploadedPhoto;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DirectUploadedPhotoRepositoryStub
    implements UploadedPhotoRepository {

    private long idGenerator = 0;
    protected Map<Long, UploadedPhoto> photosMap = new LinkedHashMap<>();

    @Override
    public UploadedPhoto save(UploadedPhoto photo) {
        long id = idGenerator++;
        photo.setId(id);
        photosMap.put(id, photo);
        return photo;
    }

    @Override
    public List<UploadedPhoto> getByGoodId(Long goodId) {
        return photosMap.values().stream()
            .filter(photo -> photo.getGoodId().equals(goodId))
            .collect(Collectors.toList());
    }

    @Override
    public UploadedPhoto getById(long id) {
        return photosMap.get(id);
    }

    @Override
    public List<UploadedPhoto> getByGoodIds(Collection<Long> goodIds) {
        return photosMap.values().stream()
            .filter(el -> goodIds.contains(el.getGoodId()))
            .collect(Collectors.toList());
    }
}
