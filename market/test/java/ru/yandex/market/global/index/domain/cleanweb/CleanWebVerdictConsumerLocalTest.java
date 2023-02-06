package ru.yandex.market.global.index.domain.cleanweb;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPictures;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;
import ru.yandex.market.global.index.domain.cleanweb.model.CleanWebVerdictEntity;
import ru.yandex.market.global.index.mapper.EntityMapper;
import ru.yandex.market.global.index.mapper.JsonMapper;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CleanWebVerdictConsumerLocalTest extends BaseLocalTest {

    private final CleanWebVerdictConsumerService consumerService;

    @Test
    public void testTransformation() {

        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(123)
                        .setOfferId("12345").build())
                .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                        .setPartner(
                                DataCampOfferPictures.PartnerPictures.newBuilder()
                                        .setOriginal(DataCampOfferPictures.SourcePictures.newBuilder()
                                                .addSource(DataCampOfferPictures.SourcePicture.newBuilder()
                                                        .setUrl("https://yandex.ru/").build()).build()).build()
                        ).build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                        .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                                                .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                                        .setName("cleanweb-moderaion-verict")
                                                        .setValue("[]").build())
                                                .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                                        .setName("some-value")
                                                        .setValue("123").build()).build()).build()).build()).build()
                );


        DataCampOffer.Offer offer = offerBuilder.build();

        DataCampOffer.Offer newOffer = consumerService.rebuildOffer(offer,
                new CleanWebVerdict(EntityMapper.MAPPER.getCleanWebModerationKeyFromOffer(offer))
                        .setSuccess(false)
                        .setReasons(List.of("text_toloka_lv_authority_insult",
                                "text_toloka_no_sense")));

        System.out.println(newOffer);

    }

    @Test
    public void testFlow() throws IOException {
        List<CleanWebVerdictEntity> offerVerdicts = JsonMapper.DB_JSON_MAPPER.readValue(
                readResourceAsString("/offer/clean-web-example-1.json"),
                new TypeReference<List<CleanWebVerdictEntity>>() {});
        consumerService.accept(offerVerdicts);
    }

    private static String readResourceAsString(String path) {
        //noinspection ConstantConditions
        return new Scanner(
                CleanWebVerdictConsumerLocalTest.class.getResourceAsStream(path), UTF_8
        ).useDelimiter("\\A").next();
    }

}
