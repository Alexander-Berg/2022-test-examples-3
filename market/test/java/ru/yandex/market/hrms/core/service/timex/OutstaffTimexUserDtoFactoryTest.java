package ru.yandex.market.hrms.core.service.timex;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Company;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Post;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.ExternalSystemLoginLoaderFactory;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffEntity;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffEntityRepo;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffTimexUserDtoFactory;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffTimexUserDtoFactoryNew;
import ru.yandex.market.hrms.core.service.outstaff.TimexContext;
import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.outstaff.dto.YaDiskResponseDto;
import ru.yandex.market.hrms.core.service.util.HrmsCollectionUtils;
import ru.yandex.market.hrms.model.domain.DomainType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OutstaffTimexUserDtoFactoryTest.before.csv")
class OutstaffTimexUserDtoFactoryTest extends AbstractCoreTest {

    @Autowired
    private ExternalSystemLoginLoaderFactory externalSystemLoginLoaderFactory;

    @Autowired
    private OutstaffTimexUserDtoFactoryNew outstaffTimexUserDtoFactoryNew;

    @Autowired
    private OutstaffTimexUserDtoFactory outstaffTimexUserDtoFactory;

    @Autowired
    private OutstaffEntityRepo outstaffEntityRepo;

    @MockBean
    private YaDiskClient yaDiskClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<DomainType, Long> domainIds = Map.of(
            DomainType.FFC, 4L,
            DomainType.SC, 42L,
            DomainType.RW, 52L
    );

    @ParameterizedTest()
    @CsvSource({
            "FFC,OutstaffTimexUserDtoFactoryTest.ffc.json",
            "SC,OutstaffTimexUserDtoFactoryTest.sc.json",
            "RW,OutstaffTimexUserDtoFactoryTest.rw.json"
    })
    public void happyPath(DomainType domainType, String expectedJsonFilepath) throws Exception {
        when(yaDiskClient.getFileDirectDownloadLink(anyString()))
                .thenReturn(new YaDiskResponseDto(403, null, null,
                        null, false, null, null, null));

        var domainId = domainIds.get(domainType);
        var outstaff = outstaffEntityRepo.findAllByDomainId(domainId);
        var singleEntity = StreamEx.of(outstaff)
                .filterBy(OutstaffEntity::getDomainId, domainId)
                .findFirst()
                .orElseThrow();

        var externalSystemLoginLoader = externalSystemLoginLoaderFactory.forOutstaff(outstaff);

        var timexContext = prepareTimexContext(outstaff);
        var result = outstaffTimexUserDtoFactory.create(singleEntity, timexContext, "somewhere",
                externalSystemLoginLoader);
        var resultJson = objectMapper.writeValueAsString(result);

        JSONAssert.assertEquals(loadFromFile(expectedJsonFilepath), resultJson, false);
        Mockito.reset(yaDiskClient);
    }

    @ParameterizedTest()
    @CsvSource({
            "FFC,OutstaffTimexUserDtoFactoryTest.ffc.json",
            "SC,OutstaffTimexUserDtoFactoryTest.sc.json",
            "RW,OutstaffTimexUserDtoFactoryTest.rw.json"
    })
    public void happyPathNew(DomainType domainType, String expectedJsonFilepath) throws Exception {
        when(yaDiskClient.getFileDirectDownloadLink(anyString()))
                .thenReturn(new YaDiskResponseDto(403, null, null,
                        null, false, null, null, null));

        var domainId = domainIds.get(domainType);
        var outstaff = outstaffEntityRepo.findAllByDomainId(domainId);
        var singleEntity = StreamEx.of(outstaff)
                .filterBy(OutstaffEntity::getDomainId, domainId)
                .findFirst()
                .orElseThrow();

        var externalSystemLoginLoader = externalSystemLoginLoaderFactory.forOutstaff(outstaff);

        var timexContext = prepareTimexContext(outstaff);
        var result = outstaffTimexUserDtoFactoryNew.create(singleEntity, timexContext, "somewhere",
                externalSystemLoginLoader);
        var resultJson = objectMapper.writeValueAsString(result);

        JSONAssert.assertEquals(loadFromFile(expectedJsonFilepath), resultJson, false);
        Mockito.reset(yaDiskClient);
    }

    private TimexContext prepareTimexContext(Collection<OutstaffEntity> entities) {

        var accessLevels = StreamEx.of(entities)
                .distinct(OutstaffEntity::getArea)
                .map(o -> {
                    var accessLevel = new AccessLevel();
                    accessLevel.setName(o.getArea());
                    return accessLevel;
                })
                .toList();

        var posts = StreamEx.of(entities)
                .distinct(OutstaffEntity::getArea)
                .map(o -> {
                    var post = new Post();
                    post.setName(o.getPosition());
                    return post;
                })
                .toList();

        return TimexContext.builder()
                .posts(posts)
                .accessLevels(accessLevels)
                .companies(HrmsCollectionUtils.mapToListBy(accessLevels, a -> {
                    var company = new Company();
                    company.setName(a.getName() + " ЛАРЕЦ");
                    return company;
                }))
                .build();
    }
}
