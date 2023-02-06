package ru.yandex.market.core.testing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.core.cutoff.model.CutoffType;

/**
 * Тип проверки, которую проходит магазин в песочном индексе.
 */
@XmlEnum
public enum TestingType implements HasId<Integer> {

    /**
     * Премодерация магазина только по CPC (магазины Украины, Казахстана и тд).
     */
    @XmlEnumValue("0")
    CPC_PREMODERATION(0, ShopProgram.CPC),

    /**
     * Проверка CPC-лайт-тикетов. Запускается при ручном создании менеджерами по качеству отключения {@link
     * ru.yandex.market.core.cutoff.model.CutoffType#QMANAGER_OTHER QMANAGER_OTHER}
     */
    @XmlEnumValue("1")
    CPC_LITE_CHECK(1, ShopProgram.CPC),

    /**
     * Полная премодерация магазина CPC+CPA.
     * <p>
     * Создаётся:
     * <ol>
     * <li> для новых магазинов</li>
     * <li> для магазинов, которые отключены более 30 дней назад</li>
     * <li> для магазинов, у который был установлен {@link ru.yandex.market.core.param.model.ParamType#IS_ONLINE признак
     * онлайновости}</li>
     * </ol>
     *
     * @deprecated Вместо полной премодерации будут создаваться две записи. Одна для CPA, вторая для CPC.
     */
    @Deprecated
    @XmlEnumValue("2")
    FULL_PREMODERATION(2, ShopProgram.GENERAL),

    /**
     * CPA-премодерация магазина - контрольный заказ + ручные проверки.
     * Делается только один раз при первом подключении магазина к CPA, или если магазин был год отключен
     */
    @XmlEnumValue("3")
    CPA_PREMODERATION(3, ShopProgram.CPA),

    /**
     * CPA проверка при помощи автоматического котнрольного заказа в ABO.
     * В результате этой проверки АБО само открывает/закрывает отключения MBI.
     * Этот тип проверки надо обязательно отрефакторить на стороне MBI и ABO
     *
     * @deprecated CPA модерация теперь переиспользуется в ДСБС. Для ДСБС нет КЗ.
     */
    @XmlEnumValue("4")
    @Deprecated
    CPA_CHECK(4, ShopProgram.CPA),

    /**
     * Проверка интеграции магазина в инфраструктуру Маркета (проверка работы API для подключения к CPA и тд).
     * Магазин в этот вид тестинга может попасть только будучи не подключенным к Маркету, так как в случае, если магазин
     * подключен к Маркету, он может все проверки провести на проде.
     */
    @XmlEnumValue("5")
    SELF_CHECK(5, ShopProgram.SELF_CHECK),

    /**
     * Лайтовая общая проверка. Магазин на нее отправляется, когда создается общее отключение
     * {@link CutoffType#COMMON_OTHER}.
     *
     * @deprecated Общих отключений для CPA/CPC для ДСБС не предвидится в ближайшем будущем.
     */
    @Deprecated
    @XmlEnumValue("6")
    GENERAL_LITE_CHECK(6, ShopProgram.GENERAL),

    /**
     * Проверка DSBS-лайт-тикетов. Запускается при ручном создании менеджерами по качеству отключения {@link
     * ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs#QUALITY_OTHER}
     */
    @XmlEnumValue("7")
    DSBS_LITE_CHECK(7, ShopProgram.CPA),

    /**
     * Магазин хочется отладиться.
     * Актуально для ДСБС.
     * Отличается от самопроверки SELF_CHECK тем, что самопроверку магазин проходит один раз, проходя
     * сценарии в АБО, на самоотладку может уходить сколько угодно раз.
     */
    @XmlEnumValue("8")
    API_DEBUG(8, ShopProgram.API_DEBUG);

    public static final Set<TestingType> PREMODERATION_TYPES =
            Collections.unmodifiableSet(EnumSet.of(CPA_PREMODERATION, CPC_PREMODERATION));

    /**
     * Идентификатор типа для сторонних и внутренних сервисов.
     */
    private final int id;
    /**
     * Программа, к которой относится тип проверки.
     */
    private final ShopProgram shopProgram;

    TestingType(int id, ShopProgram shopProgram) {
        this.id = id;
        this.shopProgram = Objects.requireNonNull(shopProgram);
    }

    public static TestingType findTestingType(int i) {
        if ((i > -1) && (i < values().length)) {
            return values()[i];
        } else {
            throw new IllegalArgumentException("Unknown testing type");
        }
    }

    public static TestingType valueOf(int id) {
        return HasId.getById(TestingType.class, id);
    }

    @Override
    public Integer getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public ShopProgram getShopProgram() {
        return shopProgram;
    }

    public boolean subsumes(TestingType that) {
        return (this == CPC_PREMODERATION && that == CPC_LITE_CHECK)
                || (this == CPA_PREMODERATION && (that == CPA_CHECK || that == DSBS_LITE_CHECK));
    }

    // TODO test
    public boolean isLightCheck() {
        return this == CPC_LITE_CHECK || this == CPA_CHECK || this == GENERAL_LITE_CHECK || this == DSBS_LITE_CHECK;
    }

    public boolean isFullModeration() {
        return this == CPC_PREMODERATION || this == CPA_PREMODERATION || this == FULL_PREMODERATION;
    }

    public boolean isSelfCheck() {
        return this == SELF_CHECK;
    }

    public boolean isCpaCheck() {
        return this == CPA_PREMODERATION ||
                this == SELF_CHECK ||
                this == API_DEBUG ||
                this == DSBS_LITE_CHECK;
    }

    public TestingType merge(TestingType that) {
        return that.subsumes(this) ? that : this;
    }
}
