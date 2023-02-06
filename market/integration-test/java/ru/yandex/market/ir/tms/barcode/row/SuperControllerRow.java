package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

/**
 * Let's declare here only the columns we actually use.
 */
@Bendable
public class SuperControllerRow {
    @BenderPart(strictName = true, name = "category_id")
    private final long categoryId;

    @BenderPart(strictName = true, name = "url")
    private final String url;

    @BenderPart(strictName = true, name = "model_id")
    private final long modelId;

    @BenderPart(strictName = true, name = "modification_id")
    private final long modificationId;

    @BenderPart(strictName = true, name = "market_sku_id")
    private final long marketSkuId;

    @BenderPart(strictName = true, name = "barcode")
    private final String barcode;

    @BenderPart(strictName = true, name = "matched_id")
    private final long matchedId;

    @BenderPart(strictName = true, name = "matched_type")
    private final long matchedType;

    @BenderPart(strictName = true, name = "matched_target")
    private final long matchedTarget;

    @BenderPart(strictName = true, name = "classification_type")
    private final String classificationType;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public SuperControllerRow(
        long categoryId,
        String url,
        long modelId,
        long modificationId,
        long marketSkuId,
        String barcode,
        long matchedId,
        long matchedType,
        long matchedTarget,
        String classificationType
    ) {
        this.categoryId = categoryId;
        this.url = url;
        this.modelId = modelId;
        this.modificationId = modificationId;
        this.marketSkuId = marketSkuId;
        this.barcode = barcode;
        this.matchedId = matchedId;
        this.matchedType = matchedType;
        this.matchedTarget = matchedTarget;
        this.classificationType = classificationType;
    }
}
