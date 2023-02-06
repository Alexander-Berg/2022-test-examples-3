package ru.yandex.direct.core.testing.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.outdoor.repository.PlacementsOutdoorDataRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementBlockRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementRepository;
import ru.yandex.direct.dbschema.ppcdict.Tables;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.Placements.PLACEMENTS;

@ParametersAreNonnullByDefault
public class TestPlacementRepository extends PlacementRepository {

    private DslContextProvider dslContextProvider;

    @Autowired
    public TestPlacementRepository(DslContextProvider dslContextProvider,
                                   PlacementBlockRepository blockRepository,
                                   PlacementsOutdoorDataRepository outdoorDataRepository) {
        super(dslContextProvider, blockRepository, outdoorDataRepository);
        this.dslContextProvider = dslContextProvider;
    }

    public Long getNextPageId() {
        return UtilRepository.getNextId(dslContextProvider.ppcdict(), PLACEMENTS, PLACEMENTS.PAGE_ID);
    }

    public void clearPlacements() {
        dslContextProvider.ppcdict()
                .deleteFrom(Tables.PLACEMENT_BLOCKS)
                .execute();
        dslContextProvider.ppcdict()
                .deleteFrom(Tables.PLACEMENTS)
                .execute();
    }
}
