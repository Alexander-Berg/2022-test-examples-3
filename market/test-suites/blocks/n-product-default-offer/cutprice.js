import {makeSuite, makeCase} from 'ginny';
import {head} from 'ambar';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';
import CutPriceDescription from '@self/platform/components/CutPriceDescription/__pageObject';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import newAndCutpriceOffersModel from '@self/platform/spec/hermione/fixtures/cutprice/newAndCutpriceOffersModel';

/**
 * Тест на уценку в ДО КМ
 * @property {PageObject.FilterPickerInline} cutpriceFilter - Фильтр уценки в визитке
 * @property {PageObject.ProductDeliveryCpa2} defaultOfferDelivery – Доставка дефолтного оффера
 * @property {PageObject.OfferInfo} offerInfo - Попап информации об оффере
 * @property {PageObject.WishlistControl} wishlistControl - Попап информации об оффере
 */
export default makeSuite('Блок ДО', {
    story: {
        'Описание': {
            'При выбранном фильтре "Уцененные"': {
                'содержит лэйбл "Уценённый — как новый."': makeCase({
                    id: 'marketfront-3539',
                    issue: 'MARKETVERSTKA-34745',
                    test() {
                        return this.cutPriceDescription.getInfoText()
                            .should.eventually.to.include(
                                'Уценённый — как новый.',
                                'Дефолтный оффер содержит правильный текст'
                            );
                    },
                }),
            },
            'При выбранном и далее убранном фильтре "Уцененные"': {
                'не содержит лейбла "Уценённый — как новый."': makeCase({
                    id: 'marketfront-3087',
                    issue: 'MARKETVERSTKA-32614',
                    async test() {
                        // Изначально в стейте лежит оффер с уценкой в ДО
                        await this.browser.allure.runStep(
                            'Проверяем существует ли блок с уценкой в ДО',
                            () => this.browser.isExisting(`${DefaultOffer.root} ${CutPriceDescription.root}`)
                        );
                        // Заменяем стейт репорта чтобы при убирании фильтра вернулись офферы с уценкой и без
                        await this.browser.setState('report', newAndCutpriceOffersModel.state);
                        await this.cutpriceFilter.clickReset();

                        return this.browser.allure.runStep(
                            'Ждем пока будет ДО без описания (значит и без уценки)',
                            () => this.browser.waitUntil(
                                async () => this.browser.allure
                                    .runStep(
                                        'Проверяем существует ли блок с описанием',
                                        () => this.browser.isExisting(`${DefaultOffer.root} ${CutPriceDescription.root}`)
                                    )
                                    .then(isExists => !isExists),
                                15000,
                                'Не дождались появления ДО без уценки'
                            )
                        );
                    },
                }),
            },
        },
        'Изображение товара': {
            'При клике': {
                'открывается попап': makeCase({
                    id: 'marketfront-3537',
                    issue: 'MARKETVERSTKA-34743',
                    async test() {
                        await this.cutPricePictures.getPictureByIndex(1).click();
                        const isPopupVisible = await this.offerInfo.isVisible();

                        return this.expect(isPopupVisible).to.be.equal(
                            true,
                            'попап с информацией об оффере виден на странице'
                        );
                    },
                }),
            },
        },
        'Счётчик изображений': {
            'По умолчанию': {
                'содержит количество оставшихся изображений': makeCase({
                    id: 'marketfront-3535',
                    issue: 'MARKETVERSTKA-34741',
                    async test() {
                        const text = await this.cutPricePictures.more.getText();

                        return this.expect(text).to.be.equal(
                            this.params.moreButtonText,
                            'количество оставшихся изображений соответствует ожидаемому'
                        );
                    },
                }),
            },
            'При клике': {
                'открывается попап': makeCase({
                    id: 'marketfront-3536',
                    issue: 'MARKETVERSTKA-34742',
                    async test() {
                        await this.cutPricePictures.more.click();
                        const isPopupVisible = await this.offerInfo.isVisible();

                        return this.expect(isPopupVisible).to.be.equal(
                            true,
                            'попап с информацией об оффере виден на странице'
                        );
                    },
                }),
            },
        },
        'Метрика': {
            'При клике по кнопке "В магазин"': {
                'цель срабатывает': makeCase({
                    id: 'marketfront-3295',
                    issue: 'MARKETVERSTKA-33061',
                    async test() {
                        await this.clickoutButton.click();

                        const goals = await this.browser.yaGetMetricaGoal(
                            nodeConfig.yaMetrika.market.id,
                            'product-page_default-offer_to-shop_go-to-shop',
                            schema({
                                'default-offer': {
                                    isCutPrice: Boolean,
                                },
                                'reqId': String,
                            })
                        );

                        const goal = head(goals);

                        return this.expect(goal['default-offer'].isCutPrice)
                            .to.be.equal(
                                true,
                                'Goal метрики содержит флаг isCutPrice = true'
                            );
                    },
                }),
            },
        },
    },
});
