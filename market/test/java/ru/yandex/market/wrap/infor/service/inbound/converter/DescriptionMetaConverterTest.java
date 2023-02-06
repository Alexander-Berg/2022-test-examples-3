package ru.yandex.market.wrap.infor.service.inbound.converter;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.DescriptionMeta;

import static com.google.common.base.Strings.repeat;
import static ru.yandex.market.wrap.infor.service.inbound.converter.DescriptionMetaConverter.COMMENT_PREFIX;
import static ru.yandex.market.wrap.infor.service.inbound.converter.DescriptionMetaConverter.DESCRIPTION_PREFIX;
import static ru.yandex.market.wrap.infor.service.inbound.converter.DescriptionMetaConverter.DESCR_MAX_LENGTH;
import static ru.yandex.market.wrap.infor.service.inbound.converter.DescriptionMetaConverter.NAME_PREFIX;
import static ru.yandex.market.wrap.infor.service.inbound.converter.DescriptionMetaConverter.NOTES_MAX_LENGTH;

@RunWith(BlockJUnit4ClassRunner.class)
class DescriptionMetaConverterTest extends SoftAssertionSupport {

    private final DescriptionMetaConverter converter = new DescriptionMetaConverter();

    /**
     * На вход подается Item, обладающий только названием с допустимой длиной.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr указано название.
     * <p>
     * В поле notes1 - продублировано оно же со специальным префиксом названия.
     * <p>
     * В поле notes2 - null.
     */
    @Test
    void conversionWithNameOnly() {
        Item item = new Item.ItemBuilder("Название", null, null).build();

        DescriptionMeta meta = converter.convert(item);

        softly.assertThat(meta.getDescr()).isEqualTo(item.getName());
        softly.assertThat(meta.getNotes1()).containsSequence(NAME_PREFIX, item.getName());
        softly.assertThat(meta.getNotes2()).isNull();
    }

    /**
     * На вход подается Item, обладающий только названием, чья длина превышает допустимое ограничение.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr записано название, сокращенное до допустимой длины.
     * <p>
     * В в поле notes1 - его полная версия со специальным префиксом названия.
     * <p>
     * В поле notes2 - null.
     */
    @Test
    void conversionWithTooLongNameOnly() {
        Item item = new Item.ItemBuilder(repeat("A", DESCR_MAX_LENGTH + 5), null, null).build();

        DescriptionMeta meta = converter.convert(item);

        softly.assertThat(meta.getDescr()).isEqualTo(item.getName().substring(0, DESCR_MAX_LENGTH));
        softly.assertThat(meta.getNotes1()).containsSequence(NAME_PREFIX, item.getName());
        softly.assertThat(meta.getNotes2()).isNull();
    }

    /**
     * На вход подается Item, обладающий названием допустимой длины и признаком мастер бокса (10 единиц в упаковке).
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr записано название, которому предшествует префикс мастербокса.
     * <p>
     * В поле notes1 - снова оно же, но со специальным префиксом названия.
     * <p>
     * В поле notes2 - null.
     */
    @Test
    void conversionWithMasterBox() {
        Item item = new Item.ItemBuilder("Название", null, null).setBoxCapacity(10).build();

        DescriptionMeta meta = converter.convert(item);

        String expectedName = DescriptionMetaConverter.MASTERBOX_PREFIX + " " + item.getName();
        softly.assertThat(meta.getDescr()).isEqualTo(expectedName);
        softly.assertThat(meta.getNotes1()).containsSequence(NAME_PREFIX, expectedName);
        softly.assertThat(meta.getNotes2()).isNull();
    }

    /**
     * На вход подается Item, обладающий названием допустимой длины и описанием.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * В поле descr записано название
     * <p>
     * В поле notes1 - снова оно же, но со специальным префиксом названия
     * + описание товара с префиксом описания.
     * <p>
     * В поле notes2 - null.
     */
    @Test
    void conversionWithNameAndDescription() {
        Item item = new Item.ItemBuilder("Название", null, null).setDescription("Описание").build();

        DescriptionMeta meta = converter.convert(item);

        softly.assertThat(meta.getDescr()).isEqualTo(item.getName());
        softly.assertThat(meta.getNotes1()).containsSubsequence(
            NAME_PREFIX, item.getName(),
            DESCRIPTION_PREFIX, item.getDescription()
        );
        softly.assertThat(meta.getNotes2()).isNull();
    }

    /**
     * На вход подается Item, обладающий названием допустимой длины, описанием и комментарием.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr записано название.
     * <p>
     * В поле notes1 - снова оно же, но со специальным префиксом названия
     * + описание товара с префиксом описания
     * + комментарий товара с префиксом комментария.
     * <p>
     * В поле notes2 - null.
     */
    @Test
    void conversionWithNameAndDescriptionAndComment() {
        Item item = new Item.ItemBuilder("Название", null, null)
            .setDescription("Описание")
            .setComment("Комментарий")
            .build();

        DescriptionMeta meta = converter.convert(item);

        softly.assertThat(meta.getDescr()).isEqualTo(item.getName());
        softly.assertThat(meta.getNotes1()).containsSubsequence(
            NAME_PREFIX, item.getName(),
            DESCRIPTION_PREFIX, item.getDescription(),
            COMMENT_PREFIX, item.getComment()
        );

        softly.assertThat(meta.getNotes2()).isNull();
    }

    /**
     * На вход подается Item, обладающий названием с длиной, которая превышает вместимость полей notes1+notes2.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr записано название, сокращенное до допустимой длины.
     * <p>
     * В поле notes1 - Префикс названия + само название, обрезанное так, дабы его первая часть вмещалась в ограничение.
     * <p>
     * В поле notes2 - оставшаяся часть, которая влезла.
     * <p>
     * Само название составлено ровно так, что бы полностью заполнить
     * notes1 - префиксом и символами A
     * notes2 - символом B
     */
    @Test
    void conversionWithNameExceedingBothNotes() {
        int aSymbolsCount = NOTES_MAX_LENGTH - NAME_PREFIX.length();
        Item item = new Item.ItemBuilder(
            repeat("A", aSymbolsCount) + repeat("B", NOTES_MAX_LENGTH * 4),
            null,
            null)
            .build();

        DescriptionMeta meta = converter.convert(item);

        softly.assertThat(meta.getDescr()).isEqualTo(item.getName().substring(0, DESCR_MAX_LENGTH));
        String notes1Value = NAME_PREFIX + repeat("A", aSymbolsCount);

        softly.assertThat(meta.getNotes1()).isEqualTo(notes1Value);
        softly.assertThat(meta.getNotes2()).isEqualTo(repeat("B", NOTES_MAX_LENGTH));
    }

    /**
     * На вход подается Item, обладающий названием с длиной, которая полностью займет поле notes1 с учетом префикса.
     * <p>
     * В результате на выходе должен получиться объект, у которого:
     * <p>
     * В поле descr записано название, сокращенное до допустимой длины.
     * <p>
     * В поле notes1 - Префикс названия + само название, обрезанное так, дабы его первая часть вмещалась в ограничения.
     * <p>
     * В поле notes2 - Префикс описания + описание.
     */
    @Test
    void conversionWithNameTakingNotes1AndDescription() {
        int aSymbolsCount = NOTES_MAX_LENGTH - NAME_PREFIX.length();
        Item item = new Item.ItemBuilder(repeat("A", aSymbolsCount), null, null)
            .setDescription("Описание")
            .build();

        DescriptionMeta meta = converter.convert(item);
        softly.assertThat(meta.getDescr()).isEqualTo(item.getName().substring(0, DESCR_MAX_LENGTH));
        String notes1Value = NAME_PREFIX + repeat("A", aSymbolsCount);

        softly.assertThat(meta.getNotes1()).isEqualTo(notes1Value);
        softly.assertThat(meta.getNotes2()).isEqualTo(DESCRIPTION_PREFIX + item.getDescription());
    }
}
