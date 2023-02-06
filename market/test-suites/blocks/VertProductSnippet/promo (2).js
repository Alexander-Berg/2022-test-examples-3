import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты для блока VertProductSnippet на промо хабе
 * @param {PageObject.Snippet} snippet
 */
export default makeSuite('Сниппет продукта промо хаба', {
    environment: 'testing',
    story: {
        'по умолчанию': {
            'имеет бейджик скидки': makeCase({
                id: 'm-touch-1982',
                issue: 'MOBMARKET-7899',
                test() {
                    return this.snippet.priceDiscount
                        .isExisting()
                        .should.eventually.equal(true, 'В сниппете присутствует бейджик скидки');
                },
            }),
            'имеет блок категории.': makeCase({
                id: 'm-touch-2135',
                issue: 'MOBMARKET-8380',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что на сниппете есть блок категории',
                        () => this.snippet.category
                            .isExisting()
                            .should.eventually.equal(true, 'В сниппете присутствует блок категории'));
                },
            }),
        },
        'при клике на цену': {
            'ведет на корректный URL': makeCase({
                id: 'm-touch-1983',
                issue: 'MOBMARKET-7907',
                test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('ADCONTENT-886 кейс ждет правок в цмс');
                    // eslint-disable-next-line no-unreachable
                    return this.snippet.price
                        .click()
                        .then(() => this.browser.yaParseUrl())
                        .then(url => url.path)
                        .should.eventually.match(
                            /(offer\/\w+)|(product--.*\/\d+\?(.+)?promo-type=market)/,
                            'Урл соответствует ожидаемому'
                        );
                },
            }),
        },
        'при клике на категорию': {
            'ведет на корректный URL.': makeCase({
                id: 'm-touch-2136',
                issue: 'MOBMARKET-8381',
                test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('ADCONTENT-886 кейс ждет правок в цмс');
                    // eslint-disable-next-line no-unreachable
                    return this.browser.allure.runStep(
                        'Кликаем на категорию',
                        () => this.snippet.category
                            .click()
                            .then(() => this.browser.allure.runStep(
                                'Проверяем переход на выдачу с зажатым фильтром скидок',
                                () => this.browser.yaParseUrl()
                                    .should.eventually.be.link({
                                        pathname: 'catalog--[\\w-]+/\\d+/list',
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
