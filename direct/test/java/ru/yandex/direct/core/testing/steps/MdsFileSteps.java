package ru.yandex.direct.core.testing.steps;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.mdsfile.model.MdsFileCustomName;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileMetadata;
import ru.yandex.direct.core.entity.mdsfile.repository.MdsFileRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.dbschema.ppc.tables.MdsCustomNames.MDS_CUSTOM_NAMES;
import static ru.yandex.direct.dbschema.ppc.tables.MdsMetadata.MDS_METADATA;

public class MdsFileSteps {

    @Autowired
    private DslContextProvider dslContextProvider;

    private final JooqMapperWithSupplier<MdsFileMetadata> mdsFileMetadataMapper;
    private final JooqMapperWithSupplier<MdsFileCustomName> mdsFileNameMapper;

    public MdsFileSteps() {
        this.mdsFileMetadataMapper = MdsFileRepository.createMdsFileMetadataMapper();
        this.mdsFileNameMapper = MdsFileRepository.createMdsFileNameMapper();
    }

    public MdsFileMetadata getMetadata(int shard, long id) {
        return dslContextProvider.ppc(shard)
                .select(mdsFileMetadataMapper.getFieldsToRead())
                .from(MDS_METADATA)
                .where(MDS_METADATA.ID.eq(id))
                .fetchOne(mdsFileMetadataMapper::fromDb);
    }

    public List<MdsFileCustomName> getCustomName(int shard, long mdsId) {
        return dslContextProvider.ppc(shard)
                .select(mdsFileNameMapper.getFieldsToRead())
                .from(MDS_CUSTOM_NAMES)
                .where(MDS_CUSTOM_NAMES.MDS_ID.eq(mdsId))
                .fetch(mdsFileNameMapper::fromDb);
    }
}
