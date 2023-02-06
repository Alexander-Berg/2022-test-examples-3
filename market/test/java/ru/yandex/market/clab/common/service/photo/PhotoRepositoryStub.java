package ru.yandex.market.clab.common.service.photo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public abstract class PhotoRepositoryStub<T> {
    private long idGenerator = 0;
    protected Map<Long, T> photosMap = new LinkedHashMap<>();

    public List<T> getProcessedPhotos(Collection<Long> goodIds) {
        return photosMap.values().stream()
            .filter(p -> goodIds.contains(getGoodId(p)))
            .map(this::clone)
            .collect(Collectors.toList());
    }

    public void createProcessedPhotos(Collection<T> photos) {
        photos.forEach(p -> {
            T pclone = clone(p);
            setId(pclone, idGenerator++);
            photosMap.put(getId(pclone), pclone);
        });
    }

    public void saveProcessedPhotos(Collection<T> photos) {
        photos.forEach(p -> {
            T pclone = clone(p);
            photosMap.put(getId(pclone), pclone);
        });
    }

    protected List<T> filter(Predicate<T> predicate) {
        return photosMap.values().stream()
            .filter(predicate)
            .map(this::clone)
            .collect(Collectors.toList());
    }

    public void removeProcessedPhotos(Long goodId) {
        photosMap.values().removeIf(p -> getGoodId(p).equals(goodId));
    }

    protected abstract void setId(T photo, Long id);
    protected abstract Long getId(T photo);
    protected abstract Long getGoodId(T photo);
    protected abstract T clone(T photo);
}
