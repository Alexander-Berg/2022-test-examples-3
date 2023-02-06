import {isArray} from 'ambar';
import {makeSuite, makeCase} from 'ginny';
import {has, map} from 'lodash';
import {FILTER_IDS} from '@self/root/src/constants/filters';

/**
 * @param {PageObject} button - кнопка "Все фильтры"
 * @param {number} params.nid - идентификатор категории
 * @param {number} params.slug - идентификатор категории
 */
export default makeSuite('Кнопка "Все фильтры".', {
    feature: 'Бэйджи',
    issue: 'MARKETVERSTKA-29907',
    params: {
        nid: 'Идентификатор категории',
    },
    story: {
        'По умолчанию': {
            'должна отображаться': makeCase({
                id: 'marketfront-1500',
                test() {
                    return Promise.all([
                        this.expect(this.button.isVisible()).to.be.equal(true, 'Кнопка отобразилась'),
                        this.expect(this.button.getButtonText()).to.be.equal('Все фильтры', 'Текст на кнопке совпадает'),
                    ]);
                },
            }),
        },

        'При клике': {
            'должен происходить переход на страницу всех фильтров': makeCase({
                id: 'marketfront-1501',
                test() {
                    return this.button.click()
                        .then(() => this.browser.yaWaitForPageReady())
                        .then(() => this.browser.allure.runStep('Проверяем URL страницы после перехода', () => (
                            this.browser.getUrl()
                                .should.eventually.be.link({
                                    pathname: /^\/catalog--[\w-]+\/\d+\/filters$/,
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                        )));
                },
            }),

            'должны содержать текущие фильтры': makeCase({
                id: 'marketfront-2995',
                async test() {
                    const expectedUrl = await this.browser.yaBuildURL('market:all-filters', {
                        slug: this.params.slug,
                        nid: this.params.nid,
                    });

                    await this.filterVendor.clickItemByIndex(1);
                    await this.filterDeliveryType.clickItemByIndex(1);
                    await this.button.click();
                    await this.browser.yaWaitForPageReady();

                    const url = await this.browser.yaParseUrl();

                    const {query} = url;
                    const glfilter = isArray(query.glfilter) ? query.glfilter : [query.glfilter];
                    const filterIds = map(glfilter, filter => filter.split(':')[0]);

                    await this.expect(has(query, 'offer-shipping')).to.be.equal(true, 'Есть фильтр по типу доставки');
                    await this.expect(filterIds.includes(FILTER_IDS.PRODUCERS)).to.be.equal(true, 'Есть фильтр по вендору');
                    await this.expect(url.href, 'Проверяем что урл содержит правильный nid').to.be.link({
                        pathname: expectedUrl,
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    }, 'урл содержит правильный nid');
                },
            }),
        },
    },
});
