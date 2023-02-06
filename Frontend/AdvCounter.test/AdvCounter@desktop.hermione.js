'use strict';

const findResourceRequested = require('../../../../hermione/client-scripts/find-resource-requested');
const YABS_COUNTER = 'yabs.yandex.ru/count/';
const YABS_COUNTER_ERROR = 'yabs-счётчик не содержит ожидаемый url';
const YABS_COUNTER_DELIMITER = '~1=';
const PO = require('./AdvCounter.page-object');

hermione.only.in('chrome-desktop', 'Счетчики не браузерозависимые');
specs({
    feature: 'Счётчики учёта видимости рекламы',
}, function() {
    it('premium', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'пластиковые окна',
            data_filter: 'adv',
        }, PO.serpAdv());

        assert.include(
            await browser.getAttribute(PO.serpAdvPremium1.serpAdvCounter(), 'style'),
            YABS_COUNTER,
            YABS_COUNTER_ERROR,
        );
    });

    it('premium в распиле', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'купить квартиру в москве',
            exp_flags: [
                'init_meta=need_stream_pre_main_on_main_grouping=1,enable_inter_search=1,pre_main_report_doc_count=2',
                'velocity_serplist_delimiter=1',
            ],
            data_filter: false,
        }, PO.serpAdv());

        // Счётчик, который вернулся из inter-search
        const counterIntersearch = await browser.getAttribute(PO.serpAdvPremium1.serpAdvCounter(), 'style');
        assert.include(counterIntersearch, YABS_COUNTER, YABS_COUNTER_ERROR);

        // Счётчик, который вернулся из post-search
        const counterPostsearch = await browser.getAttribute(PO.serpAdvPremium3.serpAdvCounter(), 'style');
        assert.include(counterPostsearch, YABS_COUNTER, YABS_COUNTER_ERROR);

        // LINK_HEAD + LINK_TAIL1 + LINK_TAIL2
        const partsIntersearch = counterIntersearch.split(YABS_COUNTER_DELIMITER);
        assert.equal(partsIntersearch.length, 3);

        // LINK_HEAD + LINK_TAIL3 + LINK_TAIL4
        const partsPostsearch = counterPostsearch.split(YABS_COUNTER_DELIMITER);
        assert.equal(partsPostsearch.length, 3);

        // LINK_HEAD должен совпадать у обеих ссылок
        assert.equal(partsIntersearch[0], partsPostsearch[0]);

        // LINK_TAIL не должны пересекаться
        assert.notInclude(partsIntersearch, partsPostsearch[1]);
        assert.notInclude(partsIntersearch, partsPostsearch[2]);
    });

    describe('halfpremium', function() {
        it('Базовые проверки', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 1447433776,
                data_filter: false,
            }, PO.serpAdv());

            const dataVnl = await this.browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');
            const params = JSON.parse(dataVnl);

            assert.include(params.counterUrl, YABS_COUNTER, YABS_COUNTER_ERROR);

            const result = await this.browser.execute(findResourceRequested, params.counterUrl);
            assert.isNotString(result, 'Счётчик сработал до того, как до халфпремиума доскроллили');

            await this.browser.yaScrollToEnd(PO.serpAdvHalfpremium1());
            await this.browser.pause(100); // чтобы наверняка успели отработать запросы

            const result2 = await this.browser.execute(findResourceRequested, params.counterUrl);
            assert.isNotString(result2, 'Счётчик сработал до того, как до халфпремиума доскроллили');

            await this.browser.yaScrollToEnd(PO.serpAdvHalfpremium2());
            await this.browser.pause(100); // чтобы наверняка успели отработать запросы

            const result3 = await this.browser.execute(findResourceRequested, params.counterUrl);
            assert.isString(result3, 'Счётчик не сработал при доскролливании до халфпремиума');
        });

        describe('Первая страница', function() {
            checkHalfpremiumOnLoad(0);
            checkHalfpremiumOnScroll(0);
            checkHalfPremiumAfterAjax();
        });

        describe('Вторая страница', function() {
            checkHalfpremiumOnLoad(1);
            checkHalfpremiumOnScroll(1);
        });
    });
});

function checkHalfpremiumOnLoad(page) {
    it('Дёргается на загрузку', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            p: page,
            foreverdata: 1069206273,
            data_filter: 'no_results',
        }, PO.serpAdv());

        const dataVnl = await browser.getAttribute(PO.serpAdvHalfpremium1.serpAdvCounter(), 'data-vnl');
        const params = JSON.parse(dataVnl);

        assert.include(params.counterUrl, YABS_COUNTER, YABS_COUNTER_ERROR);

        const result = await browser.execute(findResourceRequested, params.counterUrl);
        assert.isString(result, 'Счётчик не сработал при показе');
    });
}

function checkHalfpremiumOnScroll(page) {
    it('Дёргается при скролле', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'пластиковые окна',
            p: page,
            data_filter: false,
        }, PO.serpAdv());

        const dataVnl = await browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');
        const params = JSON.parse(dataVnl);

        assert.include(params.counterUrl, YABS_COUNTER, YABS_COUNTER_ERROR);

        await browser.scroll(PO.beforeHalfPremiumSnippet());

        const result = await browser.execute(findResourceRequested, params.counterUrl);
        assert.isNotString(result, 'Счётчик сработал до того, как до халфпремиума доскроллили');
        await browser.scroll(PO.pager());

        // скроллим вниз страницы
        await browser.pause(100);

        // чтобы успели отработать все события после подскролла
        const dataVnl2 = await browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');

        const params2 = JSON.parse(dataVnl2);
        const result2 = await browser.execute(findResourceRequested, params2.counterUrl);
        assert.isString(result2, 'Счётчик не сработал при доскролливании до халфпремиума');
    });
}

function checkHalfPremiumAfterAjax() {
    it('Проверить счётчики после аякс-перезапроса', async function() {
        const { browser } = this;

        let paramsBeforeAjax;

        await browser.yaOpenSerp({
            text: 'пластиковые окна',
            foreverdata: 2720531710,
            data_filter: false,
        }, PO.serpAdv());

        const dataVnl = await browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');
        paramsBeforeAjax = JSON.parse(dataVnl);
        await browser.yaWaitUntilSerpReloaded(() => browser.click(PO.header.arrow.button()));
        const dataVnl2 = await browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');
        const params = JSON.parse(dataVnl2);

        assert.include(params.counterUrl, YABS_COUNTER, YABS_COUNTER_ERROR);

        await browser.scroll(PO.pager());

        // скроллим вниз страницы
        await browser.pause(100);

        // чтобы успели отработать все события после подскролла
        const result = await browser.execute(findResourceRequested, paramsBeforeAjax.counterUrl);

        assert.isNotString(result, 'Старый счётчик сработал после аякс-перезапроса');
        const dataVnl3 = await browser.getAttribute(PO.serpAdvHalfpremium2.serpAdvCounter(), 'data-vnl');
        const params2 = JSON.parse(dataVnl3);
        const result2 = await browser.execute(findResourceRequested, params2.counterUrl);

        assert.isString(
            result2,
            'Счётчик не сработал при доскролливании до халфпремиума после аякс-перезапроса',
        );
    });
}
