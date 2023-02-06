package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmParamRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;

public class MdmParamCacheTest extends MdmBaseIntegrationTestClass {
    // Некоторые парамы у нас толком не заданы, например карготипы Меркурия, которых вообще в природе пока
    // не существует. Они ломают конверсии и нигде не юзаются, поэтому исключим их.
    private static final Set<Long> EXCLUDED = Set.of(
        KnownMdmParams.MERCURY_CIS_DISTINCT,
        KnownMdmParams.MERCURY_CIS_OPTIONAL,
        KnownMdmParams.MERCURY_CIS_REQUIRED,
        KnownMdmParams.MERCURY_ACTIVATION_TS
    );

    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmParamRepository paramRepository;

    @Test
    @Ignore
    public void findAllParamsUsingBmdmHandle() {
        mdmParamCache.refresh();

        var paramsFromRepository = paramRepository.findAll().stream()
            .filter(i -> !EXCLUDED.contains(i.getId())).collect(Collectors.toList());
        var paramsParamCache = mdmParamCache.find(
            paramsFromRepository.stream().map(MdmParam::getId).collect(Collectors.toList())
        );
        Assertions.assertThat(paramsParamCache).containsExactlyInAnyOrderElementsOf(paramsFromRepository);
    }
}
