package ru.yandex.market.supercontroller.dao.proto;

import Market.OffersData.OffersData;
import org.junit.Test;

import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mbo.http.OffersStorage;

import static org.junit.Assert.assertEquals;

public class OfferParamsToGenOfferConverterTest {


    @Test
    public void convertToGenOffer() {
        String testDescription = "|Описание: <img src=\\\"http://pic.ebayhost.net/201504/47945_2.jpg\\\" /> <br/> " +
            "<img src=\\\"http://pic.ebayhost.net/201504/47945_4.jpg\\\" /> <br />" +
            " <img src=\\\"http://pic.ebayhost.net/201504/47945_6.jpg\\\" /> " +
            "<img src=\\\"http://pic.ebayhost.net/201504/47945_3.jpg\\\" /> " +
            "<img src=\\\"http://pic.ebayhost.net/201504/47945_8.jpg\\\" /> " +
            "Manfrotto для 785PL быстрый релиз пластины для modo 785B<br /><br /> " +
            "основные признаки:<br /><br /> Подходит Для 785 Серии<br />1/4\\\"-20 винт<br />" +
            "Быстрый выпуск пластин<br /><br /> длинное описание:<br /><br /> " +
            "Эта плита быстрого выпуска поставлена с винтом камеры 1/ я - Inch - 20 и конструирована для приспособления " +
            "продуктов 785B, 785SHB / DIGI 718b и 718SHB серии Manfrotto Modo.<br /><br /> Содержание<br />" +
            "<br>1 X Manfrotto 785pl быстрый релиз пластины для Modo 785B\"";

        String afterClear = "|Описание:               Manfrotto для 785PL быстрый релиз пластины для modo 785B   " +
                "основные признаки:   Подходит Для 785 Серии 1/4\\\"-20 винт Быстрый выпуск пластин   " +
                "длинное описание:   Эта плита быстрого выпуска поставлена с винтом камеры 1/ я - Inch - 20 и " +
                "конструирована для приспособления продуктов " +
                "785B, 785SHB / DIGI 718b и 718SHB серии Manfrotto Modo.   " +
                "Содержание  1 X Manfrotto 785pl быстрый релиз пластины для Modo 785B\"";
        OffersStorage.GenerationDataOffer generationDataOffer = OfferParamsToGenOfferConverter.convertToGenOffer(
            "1", OffersStorage.SessionColor.WHITE,
                OffersData.OfferOR2SC.newBuilder()
                .setDescription(testDescription)
                .build(),
            UltraController.EnrichedOffer.newBuilder().build(),
            null);
        assertEquals(generationDataOffer.getDescription(), afterClear);
    }

    @Test
    public void createXparamFromOfferType() {
        OffersData.OfferOR2SC idxOffer = OffersData.OfferOR2SC.newBuilder()
                .setType(Offer.OfferType.OT_BOOK.getNumber())
                .build();

        OffersStorage.GenerationDataOffer generationDataOffer = convertToGenOffer(idxOffer);

        assertEquals("|Type:book", generationDataOffer.getParams());
    }

    @Test
    public void addOfferTypeToXparam() {
        OffersData.OfferOR2SC idxOffer = OffersData.OfferOR2SC.newBuilder()
                .setType(Offer.OfferType.OT_VENDOR_MODEL.getNumber())
                .build();

        OffersStorage.GenerationDataOffer generationDataOffer = convertToGenOffer(idxOffer);

        assertEquals("|Type:vendor.model", generationDataOffer.getParams());
    }

    @Test
    public void overwriteOfferTypeInXparam() {
        OffersData.OfferOR2SC idxOffer = OffersData.OfferOR2SC.newBuilder()
                .setType(Offer.OfferType.OT_GENERAL.getNumber())
                .build();

        OffersStorage.GenerationDataOffer generationDataOffer = convertToGenOffer(idxOffer);

        assertEquals("|Type:general", generationDataOffer.getParams());
    }


    @Test
    public void notUpdateXparamIfSameOfferType() {
        OffersData.OfferOR2SC idxOffer = OffersData.OfferOR2SC.newBuilder()
                .setType(Offer.OfferType.OT_MEDICINE.getNumber())
                .build();

        OffersStorage.GenerationDataOffer generationDataOffer = convertToGenOffer(idxOffer);

        assertEquals("|Type:medicine", generationDataOffer.getParams());
    }

    private OffersStorage.GenerationDataOffer convertToGenOffer(OffersData.OfferOR2SC idxOffer) {
        return OfferParamsToGenOfferConverter.convertToGenOffer(
                "1",
                OffersStorage.SessionColor.WHITE,
                idxOffer,
                UltraController.EnrichedOffer.newBuilder().build(),
                null);
    }
}
