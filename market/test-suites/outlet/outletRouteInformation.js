import {makeCase, makeSuite} from 'ginny';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import OutletInfoContent from '@self/root/src/widgets/content/OutletInfo/components/OutletInfoContent/__pageObject';

import {nonBrandedOutletMock} from './helpers';


export default makeSuite('Как добраться до ПВЗ', {
    issue: 'MARKETFRONT-52439',
    environment: 'kadavr',
    feature: 'Как добраться до ПВЗ',
    story: {
        async beforeEach() {
            this.setPageObjects({
                outletInfoContent: () => this.createPageObject(OutletInfoContent),
            });

            await this.browser.yaScenario(this, setReportState, {
                state: {
                    data: {
                        results: [
                            {
                                ...nonBrandedOutletMock,
                                yandexMapPermalink: this.params.outletYandexMapPermalink,
                            },
                        ],
                        search: {
                            results: [],
                        },
                        blueTariffs: deliveryConditionMock,
                    },
                },
            });

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.OUTLET_PAGE, {
                outletId: nonBrandedOutletMock.id,
            });
        },

        'У ПВЗ есть yandexMapPermalink.': {
            'Ссылка на Яндекс.Карты работает корректно': makeCase({
                id: 'bluemarket-4070',
                defaultParams: {
                    outletYandexMapPermalink: 98631801263,
                },
                async test() {
                    await this.outletInfoContent.isYandexMapsLinkVisible()
                        .should.eventually.be.equal(
                            true,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах должна отобразиться'
                        );

                    await this.outletInfoContent.getYandexMapsLinkText()
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
                            this.outletInfoContent.getYandexMapsUrl()
                                .should.eventually.be.link({
                                    protocol: 'http:',
                                    hostname: 'maps.yandex.ru',
                                    pathname: `/org/${this.params.outletYandexMapPermalink}`,
                                    query: {
                                        'no-distribution': '1',
                                    },
                                })
                        )
                    );

                    return this.outletInfoContent.getDoesYandexMapsLinkOpenInNewTab()
                        .should.eventually.be.equal(
                            true,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах должна открываться в новой вкладке'
                        );
                },
            }),
        },

        'У ПВЗ нет yandexMapPermalink.': {
            'Ссылка на Яндекс.Карты не отображается': makeCase({
                id: 'bluemarket-4074',
                defaultParams: {
                    outletYandexMapPermalink: null,
                },
                test() {
                    return this.outletInfoContent.isYandexMapsLinkVisible()
                        .should.eventually.be.equal(
                            false,
                            'Ссылка на страницу ПВЗ на Яндекс.Картах не должна быть отображена'
                        );
                },
            }),
        },
    },
});
