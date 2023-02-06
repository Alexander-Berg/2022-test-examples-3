package ru.yandex.market.jmf.module.ou.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class OuTestUtils {

    private final BcpService bcpService;
    private final EntityStorageService entityStorageService;

    public OuTestUtils(BcpService bcpService, EntityStorageService entityStorageService) {
        this.bcpService = bcpService;
        this.entityStorageService = entityStorageService;
    }

    public Ou createOu() {
        return createOu(Randoms.string());
    }

    public Ou createOu(Fqn fqn) {
        return bcpService.create(fqn, Maps.of(
                Ou.TITLE, Randoms.string()
        ));
    }

    public Ou createOu(Fqn fqn, Entity parent) {
        return bcpService.create(fqn, Maps.of(
                Ou.TITLE, Randoms.string(),
                Ou.PARENT, parent
        ));
    }

    public Ou createOu(Map<String, Object> attrs) {
        return createOu(Randoms.string(), attrs);
    }

    public Ou createOu(Entity parent) {
        return createOu(parent, Maps.of());
    }

    public Ou createOu(Entity parent, Map<String, Object> attrs) {
        Map<String, Object> properties = Maps.of(
                Ou.TITLE, Randoms.string(),
                Ou.PARENT, parent
        );

        properties.putAll(attrs);
        return bcpService.create(Ou.FQN_DEFAULT, properties);
    }

    public Ou createOu(String string) {
        return createOu(string, Maps.of());
    }

    public Ou createOu(String string, Map<String, Object> attrs) {
        Map<String, Object> properties = Maps.of(
                Ou.TITLE, string
        );
        properties.putAll(attrs);
        return bcpService.create(Ou.FQN_DEFAULT, properties);
    }

    public Ou getAnyCreatedOu() {
        return entityStorageService.<Ou>list(Query.of(Ou.FQN).withLimit(1))
                .get(0);
    }
}
