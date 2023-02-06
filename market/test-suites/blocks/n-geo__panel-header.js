
import {makeSuite, makeCase} from 'ginny';
import {OUTLETS_FILTERS} from '@self/project/src/entities/geo/geoState';

/**
 * Тест на хедер блока GeoPanel с ProductLevel
 * @param {PageObject.GeoSnippet} geoSnippet
 */

export default makeSuite('Шапка панели сниппетов.', {
    feature: 'Карта',
    environment: 'testing',
    story: {
        'По умолчанию': {
            'видна': makeCase({
                id: 'marketfront-2756',
                issue: 'MARKETVERSTKA-30700',
                test() {
                    return this.geoPanel.header.isVisible().should.eventually.be.equal(true, 'шапка видна');
                },
            }),

            'содержит фильтр "Пункты выдачи" с правильной подсказкой': makeCase({
                id: 'marketfront-2756',
                issue: 'MARKETVERSTKA-30700',
                async test() {
                    // Дожидаемся загрузки сниппетов, чтобы не сбрасывать состояние mouseEnter
                    await this.geoSnippet.waitForVisible();
                    const filterExists = await this.geoFilterGroup.getFilterByValue('pickup').isVisible();

                    filterExists.should.be.equal(true, 'есть фильтр "Пункты выдачи"');

                    await this.geoFilterGroup.hoverFilterByValue('pickup');

                    await this.tooltip.waitForContentVisible();
                    await this.tooltip.isVisible().should.eventually.to.be.equal(
                        true,
                        'есть подсказка для "Пункты выдачи"'
                    );

                    return this.tooltip.getContentText().should.eventually.to.be.equal(OUTLETS_FILTERS.pickup.hint);
                },
            }),
        },

        'при клике на фильтр "Пункты выдачи"': {
            'в url страницы должен добавиться параметр offer-shipping со значением pickup': makeCase({
                id: 'marketfront-2756',
                issue: 'MARKETVERSTKA-30700',
                async test() {
                    await this.geoFilterGroup.applyFilterByValue('pickup');

                    return checkFilterUrlParam.call(this, OUTLETS_FILTERS.store.hint);
                },
            }),
        },

        'при клике на фильтр "Торговые залы"': {
            'в url страницы должен добавиться параметр offer-shipping со значением store': makeCase({
                id: 'marketfront-2757',
                issue: 'MARKETVERSTKA-30701',
                async test() {
                    await this.geoFilterGroup.applyFilterByValue('pickup');

                    return checkFilterUrlParam.call(this, OUTLETS_FILTERS.store.hint);
                },
            }),
        },
    },
});

function checkFilterUrlParam(filter) {
    const matchOptions = {
        mode: 'match',
        skipProtocol: true,
        skipHostname: true,
        skipPathname: true,
    };

    const matchCondition = {
        query: {
            'offer-shipping': filter.value,
        },
    };

    const checkText = `в урле появился параметр "offer-shipping" со значением ${filter.value}`;

    return this.allure.runStep(
        `Проверяем что в урле появился параметр "offer-shipping" со значением ${checkText}`,
        () =>
            this.browser.getUrl().then(url =>
                this.expect(url, checkText).to.be.link(matchCondition, matchOptions)
            )
    );
}
