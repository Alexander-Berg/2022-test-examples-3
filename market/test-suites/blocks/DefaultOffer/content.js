import {makeCase, makeSuite} from 'ginny';

// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import Delivery from '@self/platform/spec/page-objects/widgets/parts/OfferSummary/Delivery';
import DeliveryPolicyStatus from '@self/root/src/components/DeliveryPolicyStatus/__pageObject';
import BottomDrawerPopup from '@self/root/src/components/BottomDrawerPopup/__pageObject';

/**
 * Тесты на показ информации в блоке DefaultOffer
 */
export default makeSuite('Содержимое блока дефолтного офера', {
    params: {
        showWidget: 'Отображется ли на странице',
        showShopBtn: 'Наличие кнопки "В магазин"',
        showPrice: 'Отображает ли цену',
        expectedPriceValue: 'Ожидаемое значение стоимости',
        deliveryTexts: 'Тексты о доставке (массив)',
        showReturnPolicy: 'Показывать ли сообщение о политике доставки',
        policyText: 'Текст о политике доставки',
        policyHintText: 'Текст в хинте о политике доставки',
    },
    defaultParams: {
        showWidget: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                defaultOffer: () => this.createPageObject(DefaultOffer),
                delivery: () => this.createPageObject(Delivery, {
                    parent: this.defaultOffer,
                }),
                deliveryPolicy: () => this.createPageObject(DeliveryPolicyStatus, {
                    parent: this.defaultOffer,
                }),
                bottomDrawerPopup: () => this.createPageObject(BottomDrawerPopup),
            });
        },
        'ожидаемое': makeCase({
            async test() {
                const {
                    showWidget,
                    showShopBtn,
                    showPrice,
                    expectedPriceValue,
                    deliveryTexts,
                    showReturnPolicy,
                    policyText,
                    policyHintText,
                } = this.params;

                if (showWidget) {
                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что ДО присутствует на странице'
                        );
                } else {
                    return this.defaultOffer.isExisting()
                        .should.eventually.to.equal(
                            false,
                            'Проверяем, что ДО нет на странице'
                        );
                }

                if (showShopBtn != null) {
                    await this.defaultOffer.button.isExisting()
                        .should.eventually.to.equal(
                            showShopBtn,
                            `Кнопка "В магазин" ${showShopBtn ? '' : 'не '}должна присутствовать`
                        );
                }

                if (showPrice != null) {
                    await this.defaultOffer.price.isExisting()
                        .should.eventually.to.equal(
                            showPrice,
                            `Цена дефолтного товарного предложения ${showPrice ? '' : 'не '}должна присутствовать`
                        );

                    if (showPrice && expectedPriceValue) {
                        const price = await this.defaultOffer.getPriceValue();

                        await this.expect(Number(price)).to.be.equal(
                            expectedPriceValue,
                            'Значение цены дефолтного товарного предложения должно отображаться'
                        );
                    }
                }

                if (deliveryTexts) {
                    const deliveryTextPromise = this.delivery.getText();

                    await Promise.all(
                        deliveryTexts.map(text =>
                            deliveryTextPromise
                                .should.eventually.to.have.string(
                                    text,
                                    `В тексте о доставке должен быть текст: ${text}`
                                )
                        )
                    );
                }

                if (showReturnPolicy != null) {
                    await this.deliveryPolicy.isExisting()
                        .should.eventually.to.equal(
                            showReturnPolicy,
                            `Политика возврата ${showReturnPolicy ? '' : 'не '} должна отображаться`
                        );
                }

                if (policyText) {
                    await this.deliveryPolicy.root.getText()
                        .should.eventually.to.have.string(
                            policyText,
                            `В тексте о доставке должен быть текст: ${policyText}`
                        );
                }

                if (policyHintText) {
                    await this.deliveryPolicy.questionIconClick();

                    await this.bottomDrawerPopup.content.isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Попап с описанием политики отмены должен стать видимым'
                        );

                    await this.bottomDrawerPopup.content.getText()
                        .should.eventually.to.have.string(
                            policyHintText,
                            `Попап должен содержать текст: "${policyHintText}"`
                        );
                }
            },
        }),
    },
});
