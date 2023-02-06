import {makeSuite, makeCase} from 'ginny';
import Discount from '@self/platform/spec/page-objects/Discount';

/**
 * Тесты для блока VertProductSnippet на промо хабе
 * @param {PageObject.VertProductSnippet} snippet
 */
export default makeSuite('Сниппет продукта промо хаба.', {
    environment: 'testing',
    story: {
        beforeEach() {
            this.setPageObjects({
                discount: () => this.createPageObject(Discount, {parent: this.snippet}),
            });
        },
        'По умолчанию': {
            'имеет бейджик скидки и старой цены.': makeCase({
                id: 'marketfront-2589',
                issue: 'MARKETVERSTKA-28439',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что на сниппете есть блок скидки и старой цены',
                        () => this.discount
                            .isExisting()
                            .should.eventually.equal(true, 'В сниппете присутствует бейджик скидки и старая цена'));
                },
            }),
            'имеет блок категории.': makeCase({
                id: 'marketfront-2615',
                issue: 'MARKETVERSTKA-28932',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что на сниппете есть блок категории',
                        () => this.snippet.category
                            .isExisting()
                            .should.eventually.equal(true, 'В сниппете присутствует блок категории'));
                },
            }),
        },
        'При клике на цену': {
            'ведет на корректный URL.': makeCase({
                id: 'marketfront-2590',
                issue: 'MARKETVERSTKA-28439',
                test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('ADCONTENT-886 кейс ждет правок в цмс');
                    // eslint-disable-next-line no-unreachable
                    return this.browser.allure.runStep(
                        'Кликаем на цену в сниппете',
                        () => this.snippet.price
                            .click()
                            .then(() => this.browser.yaParseUrl())
                            .then(url => {
                                if (url.path.includes('product')) {
                                    return this.browser.allure.runStep(
                                        `Проверяем переход на вкладку предложений модели
                                        с зажатым фильтром всех видов акций`,
                                        () => this.browser.yaParseUrl()
                                            .should.eventually.be.link({
                                                pathname: 'product--.*/\\d+/offers',
                                                query: {
                                                    'promo-type': 'market',
                                                },
                                            },
                                            {
                                                mode: 'match',
                                                skipProtocol: true,
                                                skipHostname: true,
                                            }));
                                }

                                return this.browser.allure.runStep(
                                    'Проверяем, что ссылка ведет на карточку оффера',
                                    () => this.browser.yaParseUrl()
                                        .should.eventually.be.link({
                                            pathname: 'offer/\\w+',
                                        },
                                        {
                                            mode: 'match',
                                            skipProtocol: true,
                                            skipHostname: true,
                                            skipQuery: true,
                                        }));
                            }));
                },
            }),
        },
        'При клике на категорию': {
            'ведет на корректный URL.': makeCase({
                id: 'marketfront-2616',
                issue: 'MARKETVERSTKA-28932',
                test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('ADCONTENT-886 кейс ждет правок в цмс');
                    // eslint-disable-next-line no-unreachable
                    return this.browser.allure.runStep(
                        'Кликаем на категорию',
                        () => this.snippet.category
                            .click()
                            .then(() => this.browser.yaParseUrl())
                            .then(() => this.browser.allure.runStep(
                                'Проверяем переход на выдачу с зажатым фильтром всех видов акций',
                                () => this.browser.yaParseUrl()
                                    .should.eventually.be.link({
                                        pathname: 'catalog--.*/\\d+/list',
                                        query: {
                                            'promo-type': 'market',
                                        },
                                    },
                                    {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }))
                            ));
                },
            }),
        },
    },
});
