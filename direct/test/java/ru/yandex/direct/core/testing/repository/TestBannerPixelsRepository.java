package ru.yandex.direct.core.testing.repository;

import java.util.Collection;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.pixels.ClientPixelProvider;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerPixelsRepository;
import ru.yandex.direct.dbschema.ppc.tables.records.ClientPixelProvidersRecord;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_PIXEL_PROVIDERS;

public class TestBannerPixelsRepository {

    private final JooqMapperWithSupplier<ClientPixelProvider> clientPixelProvidersMapper;

    @Autowired
    public TestBannerPixelsRepository(OldBannerPixelsRepository bannerPixelsRepository) {
        clientPixelProvidersMapper = bannerPixelsRepository.clientPixelProvidersMapper;
    }

    public void addClientPixelProviderPermission(DSLContext context, Long clientId,
                                                 ClientPixelProvider clientPixelProvider) {
        InsertHelper<ClientPixelProvidersRecord> insertHelper =
                new InsertHelper<>(context, CLIENT_PIXEL_PROVIDERS)
                        .add(clientPixelProvidersMapper, clientPixelProvider)
                        .set(CLIENT_PIXEL_PROVIDERS.CLIENT_ID, clientId)
                        .newRecord();
        insertHelper.executeIfRecordsAdded();
    }

    public void deletePixelPermissions(DSLContext context, Collection<Long> clientIds) {
        context.deleteFrom(CLIENT_PIXEL_PROVIDERS)
                .where(CLIENT_PIXEL_PROVIDERS.CLIENT_ID.in(clientIds))
                .execute();
    }
}
