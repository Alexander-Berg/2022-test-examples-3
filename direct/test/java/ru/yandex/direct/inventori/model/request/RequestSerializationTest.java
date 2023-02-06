package ru.yandex.direct.inventori.model.request;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class RequestSerializationTest {
    private static final BlockSize STANDARD_VIDEO_PROPORTION = new BlockSize(16, 9);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void serializeDeserializeRequestTest() throws Exception {
        CampaignParameters parameters = CampaignParameters.builder()
                .withRf(new CampaignParametersRf(3, 7))
                .withSchedule(CampaignParametersSchedule.builder()
                        .withStrategyType(StrategyType.MIN_CPM)
                        .withBudget(100)
                        .withStartDate("2018-01-01")
                        .withEndDate("2018-01-10")
                        .withCpm(0L)
                        .build())
                .build();
        List<AudienceGroup> audiences = asList(
                new AudienceGroup(AudienceGroup.GroupingType.ALL, ImmutableSet.of("2000000010", "2000000011")),
                new AudienceGroup(AudienceGroup.GroupingType.ANY,
                        ImmutableSet.of("2000000012", "2000000013", "2000000014")));
        List<PageBlock> pageBlocks = singletonList(
                new PageBlock(1L, asList(2L, 3L, 4L))
        );
        PlatformCorrections platformCorrections = PlatformCorrections.builder()
                .withDesktop(150)
                .withMobile(120)
                .withMobileOsType(MobileOsType.IOS)
                .build();
        List<ProfileCorrection> profileCorrections = asList(
                ProfileCorrection.builder()
                        .withGender(ProfileCorrection.Gender.MALE)
                        .withAge(ProfileCorrection.Age._18_24)
                        .withCorrection(345)
                        .build(),
                ProfileCorrection.builder()
                        .withGender(ProfileCorrection.Gender.FEMALE)
                        .withCorrection(678)
                        .build()
        );
        List<Target> targets = singletonList(
                new Target()
                        .withAdGroupId(444L)
                        .withGroupType(GroupType.BANNER)
                        // https://wiki.yandex-team.ru/InventORIInDirect/API/target/#objazatelnostnalichijapolejjvzavisimostiottipagruppy
                        //.withBlockSizes(singletonList(new BlockSize(100, 500)))
                        .withVideoCreatives(singletonList(new VideoCreative(60000, new BlockSize(200, 600),
                                singleton(STANDARD_VIDEO_PROPORTION))))
                        .withExcludedDomains(ImmutableSet.of("google.com"))
                        .withCryptaGroups(singletonList(new CryptaGroup(ImmutableSet.of("608:5", "608:1", "608:1"))))
                        .withAudienceGroups(audiences)
                        .withRegions(ImmutableSet.of(225, -1))
                        .withPageBlocks(pageBlocks)
                        .withPlatformCorrections(platformCorrections)
                        .withProfileCorrections(profileCorrections));

        CampaignPredictionRequest request = new CampaignPredictionRequest(42L, 1, InventoriCampaignType.MEDIA_RSYA,
                targets, parameters, null,
                null, null);

        InputStream in = ClassLoader.getSystemResourceAsStream("request.json");
        CampaignPredictionRequest expected = mapper.readerFor(CampaignPredictionRequest.class).readValue(in);

        assertEquals(expected, request);
    }
}
