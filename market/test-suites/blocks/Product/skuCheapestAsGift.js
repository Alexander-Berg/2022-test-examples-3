import {makeSuite, makeCase} from 'ginny';

import {
    offerMock,
    productMock,
    skuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/cheapestAsGift';
import {
    createFilter,
    createFilterValue,
    mergeState,
    createProductForSku,
    createSku,
    createOfferForSku,
    createEntityFilter,
    createEntityFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    skuMock as redSkuMock,
    productMock as redProductMock,
    offerMock as redOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/multiOffer';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

const greenId = '14898020';
const greenFilter = {
    id: greenId,
    value: 'зеленый',
    found: 3,
    initialFound: 1,
    code: '#008000',
    marketSku: skuMock.id,
    slug: 'avtomobilnaia-shina-aplus-a501-195-70-r15-104-102r-zimniaia-zeleniy',
    checked: true,
};

const redId = '14896255';
const filterId = '14871214';
const filterMock = {
    id: filterId,
    type: 'enum',
    name: 'Цвет товара',
    xslname: 'color_vendor',
    subType: 'image_picker',
    kind: 2,
    position: 1,
    noffers: 1,
    valuesGroups: {
        type: 'all',
        valuesIds: [greenId, redId],
    },
};

export const prepareKadavrState = () => {
    const product = createProductForSku(
        productMock,
        skuMock.id,
        productMock.id
    );
    const sku = createSku(
        skuMock,
        skuMock.id
    );
    const offer = createOfferForSku(
        offerMock,
        skuMock.id,
        offerMock.wareId
    );

    const redFilter = {
        id: redId,
        initialFound: 1,
        found: 3,
        value: 'красный',
        code: '#FF0000',
        marketSku: redSkuMock.id,
        slug: 'avtomobilnaia-shina-aplus-a501-195-70-r15-104-102r-zimniaia-krasniy',
    };

    const state = mergeState([sku, product, offer,
        {
            data: {
                search: {
                    total: 1,
                    totalOffers: 1,
                },
            },
        },
        createEntityFilterValue(redFilter, skuMock.id, filterId, redFilter.id),
        createEntityFilterValue(greenFilter, skuMock.id, filterId, greenFilter.id),
        createEntityFilter(filterMock, 'sku', skuMock.id, filterId),

        createFilter(filterMock, filterId),
        createFilterValue(greenFilter, filterId, greenId),
        createFilterValue(redFilter, filterId, redId),
    ]);

    return {state, skuId: skuMock.id, slug: skuMock.slug};
};

const prepareAdditionalSkuState = () => {
    const rProductMock = createProductForSku(redProductMock, redSkuMock.id, redProductMock.id);
    const rOfferMock = createOfferForSku(redOfferMock, redSkuMock.id, redOfferMock.wareId);
    const rSkuMock = createSku(redSkuMock, redSkuMock.id);

    return mergeState([
        rProductMock, rOfferMock, rSkuMock,
    ]);
};

export const bannerSuite = makeSuite('Баннер акции', {
    feature: 'Баннер акции',
    environment: 'kadavr',
    issue: 'BLUEMARKET-10084',
    id: 'bluemarket-3259',
    story: {
        'Открыть КМ с акционным товаром': makeCase({
            async test() {
                await this.skuCheapestAsGift.isVisible()
                    .should.eventually.to.be.equal(true, 'Баннер должен быть виден');
            },
        }),
        'Нажать на баннер': makeCase({
            async test() {
                await this.browser.yaWaitForChangeUrl(() => this.skuCheapestAsGift.clickOnBanner());
            },
        }),
    },
});

export const skuChange = makeSuite('Переключение с акционного sku', {
    feature: 'Переключение с акционного sku',
    environment: 'kadavr',
    issue: 'BLUEMARKET-10084',
    id: 'bluemarket-3260',
    story: {
        'Выбрать другой цвет товара': makeCase({
            async test() {
                await this.browser.yaScenario(this, setReportState, {state: prepareAdditionalSkuState()});

                await this.radioGroup.selectValueByIndex(1);

                await this.inGalleryBadge.waitForHidden();

                await this.skuCheapestAsGift.isVisible()
                    .should.eventually.to.be.equal(false, 'Баннер не должен быть виден');
            },
        }),
    },
});
