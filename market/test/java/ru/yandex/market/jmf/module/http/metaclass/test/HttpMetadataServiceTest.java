package ru.yandex.market.jmf.module.http.metaclass.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.db.hibernate.impl.HibernateInitializer;
import ru.yandex.market.jmf.logic.def.test.MetadataServiceTestUtil;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.impl.CreateOrEditMetaclassDto;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.module.http.metaclass.HttpMetaclassExtension;
import ru.yandex.market.jmf.module.http.metaclass.dto.HttpErrorHandlerDto;
import ru.yandex.market.jmf.module.http.metaclass.dto.HttpMetaclassExtensionDto;
import ru.yandex.market.jmf.module.http.support.config.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(InternalModuleHttpMetaclassTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HttpMetadataServiceTest {

    private static final Fqn FQN_NEW_ENTITY = Fqn.of("newHttpEntity");

    @Inject
    private MetadataService metadataService;

    @Test
    public void addHttpMetaclassWithoutParent() {
        assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = createEmptyHttpMetaclassDto(null);
        createOrEditMetaclass(dto);

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        assertTrue(newMetaclass.hasExtension(HttpMetaclassExtension.class));
    }

    @Test
    public void addHttpMetaclassWithParent() {
        assertNull(metadataService.getMetaclass(FQN_NEW_ENTITY));

        CreateOrEditMetaclassDto dto = createEmptyHttpMetaclassDto(Fqn.of("entity"));
        createOrEditMetaclass(dto);

        Metaclass newMetaclass = metadataService.getMetaclass(FQN_NEW_ENTITY);

        MetadataServiceTestUtil.assertEquals(dto, newMetaclass);
        assertTrue(newMetaclass.hasExtension(HttpMetaclassExtension.class));
    }

    private void createOrEditMetaclass(CreateOrEditMetaclassDto dto) {
        metadataService.createOrEditMetaclass(dto, metadataService.getConfVersion(),
                Map.of(HibernateInitializer.HibernateReloadAttributes.SKIP, true));
    }

    private CreateOrEditMetaclassDto createEmptyHttpMetaclassDto(Fqn parent) {
        return createHttpMetaclassDto(parent, null, null, null, null, null);
    }

    private CreateOrEditMetaclassDto createHttpMetaclassDto(Fqn parent,
                                                            HttpRequest getMethod,
                                                            HttpRequest listMethod,
                                                            String pathToItems,
                                                            HttpErrorHandlerDto getErrorHandler,
                                                            HttpErrorHandlerDto listErrorHandler) {
        HttpMetaclassExtensionDto httpExtension = new HttpMetaclassExtensionDto(
                HttpMetaclassExtension.CODE,
                getMethod,
                listMethod,
                pathToItems,
                getErrorHandler,
                listErrorHandler
        );
        CreateOrEditMetaclassDto dto = MetadataServiceTestUtil.createMetaclass(FQN_NEW_ENTITY, parent, 0);
        dto.setExtensions(List.of(httpExtension));
        return dto;
    }
}
