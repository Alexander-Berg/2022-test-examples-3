import {makeCase, makeSuite} from 'ginny';
import assert from 'assert';

import {formatDate} from '@self/root/src/spec/utils/formatDate';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';

import {skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import {postOutlet, alcohol as alcoOutlet} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {recipient} from '@self/root/src/spec/hermione/configs/checkout/formData/office-address-and-recipient';

import {OrderHeader} from '@self/root/src/components/OrderHeader/__pageObject';
import {OrderInfo} from '@self/root/src/components/OrderInfo/__pageObject';
import OrderOutlet from '@self/root/src/components/OrderOutlet/__pageObject';

import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

const ORDER_ID = 123456;

export default makeSuite('Как добраться до ПВЗ', {
    environment: 'kadavr',
    feature: 'Как добраться до ПВЗ',
    params: {
        isPost: 'Является ли пункт в заказе почтовым отделением',
    },
    story: {
        async beforeEach() {
            assert(
                this.orderCard,
                'PageObject orderCard must be defined'
            );

            assert(
                this.outletInformation,
                'PageObject outletInformation must be defined'
            );

            this.setPageObjects({
                orderHeader: () => this.createPageObject(OrderHeader, {
                    parent: this.orderCard,
                }),
                orderInfo: () => this.createPageObject(OrderInfo, {
                    parent: this.orderCard,
                }),
                orderOutletLink: () => this.createPageObject(OrderOutlet),
            });

            await setStateAndOpenOrderPage.call(this, {
                isPost: this.params.isPost,
                yandexMapPermalink: this.params.outletYandexMapPermalink,
            });

            await this.orderOutletLink.clickOnOutletInfoMapLink();
            await this.outletInformation.waitForVisible();
        },

        'У ПВЗ есть yandexMapPermalink.': {
            'Ссылка на Яндекс.Карты в попапе с информацией о ПВЗ работает корректно': makeCase({
                id: 'bluemarket-4068',
                defaultParams: {
                    outletYandexMapPermalink: 98631801263,
                },
                params: {
                    outletYandexMapPermalink: 'Идентификатор объекта на Яндекс.Картах',
                },
                async test() {
                    await this.outletInformation.isYandexMapsLinkVisible()
                        .should.eventually.be.equal(
                            true,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах должна отобразиться'
                        );

                    await this.outletInformation.getYandexMapsLinkText()
                        .should.eventually.be.equal(
                            'Посмотреть на Яндекс.Картах',
                            'Текст ссылки на страницу ПВЗ на Яндекс.Картах должен быть корректным'
                        );

                    /**
                     * Проверяем именно таким образом, а не открываем физически ссылку,
                     * так как после перехода по ссылке она видоизменяется,
                     * а нам важно проверить первоначальный url
                     */
                    await this.browser.allure.runStep(
                        'Проверяем корректность url у ссылки на страницу ПВЗ на Яндекс.Картах', () => (
                            this.outletInformation.getYandexMapsUrl()
                                .should.eventually.be.link({
                                    protocol: 'http:',
                                    hostname: 'maps.yandex.ru',
                                    pathname: `/org/${this.params.outletYandexMapPermalink}`,
                                    query: {
                                        'no-distribution': '1',
                                    },
                                })
                        ));

                    return this.outletInformation.getDoesYandexMapsLinkOpenInNewTab()
                        .should.eventually.be.equal(
                            true,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах должна открываться в новой вкладке'
                        );
                },
            }),
        },

        'У ПВЗ нет yandexMapPermalink.': {
            'Ссылка на Яндекс.Карты в попапе с информацией о ПВЗ не отображается': makeCase({
                id: 'bluemarket-4069',
                defaultParams: {
                    outletYandexMapPermalink: null,
                },
                params: {
                    outletYandexMapPermalink: 'Идентификатор объекта на Яндекс.Картах',
                },
                test() {
                    return this.outletInformation.isYandexMapsLinkVisible()
                        .should.eventually.be.equal(
                            false,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах не должна быть отображена'
                        );
                },
            }),
        },
    },
});


async function setStateAndOpenOrderPage({yandexMapPermalink, isPost}) {
    const outlet = isPost ? postOutlet : alcoOutlet;

    await this.browser.yaScenario(this, setReportState, {
        state: {
            data: {
                results: [
                    {
                        ...outlet,
                        yandexMapPermalink,
                    },
                ],
                search: {
                    results: [],
                },
            },
        },
    });

    await this.browser.yaScenario(
        this,
        prepareOrder,
        {
            status: 'DELIVERY',
            region: this.params.region,
            orders: [{
                orderId: ORDER_ID,
                items: [{
                    skuId: skuMock.id,
                    count: 1,
                    buyerPrice: 500,
                }],
                recipient,
                deliveryType: 'POST',
                [isPost ? 'postOutletId' : 'outletId']: outlet.id,
                currency: 'RUR',
                buyerCurrency: 'RUR',
                delivery: {
                    buyerPrice: 0,
                    dates: {
                        fromDate: formatDate(),
                        toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
                    },
                    [isPost ? 'postOutlet' : 'outlet']: outlet,
                },
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
        }
    );

    return this.browser.yaOpenPage(this.params.pageId, {orderId: ORDER_ID});
}
