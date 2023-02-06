'use strict';

const PO = require('../DistrPopup.page-object');

specs({
    feature: 'Popup на СЕРПе',
    type: 'Плоский дизайн',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'test test',
            srcskip: 'YABS_DISTR',
            foreverdata: '465425020',
            data_filter: 'distr-popup',
        }, PO.distrPopupFlat(), { staticHeader: false });
    });

    hermione.also.in('chrome-desktop-1920');
    it('Внешний вид', async function() {
        await this.browser.assertView('flat', PO.distrPopup());
    });

    it('Клик на кнопку отмены', async function() {
        await this.browser.yaCheckBaobabCounter(PO.distrPopup.closeButton(), {
            path: '/$page/promo_popup/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Попап-блок дистрибуции не закрылся');
    });

    it('Клик на кнопку установки', async function() {
        const url = await this.browser.yaParseHref(PO.distrPopup.installButton());
        assert.equal(url.hostname, 'browser.yandex.ru');

        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/install',
        });

        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Попап-блок дистрибуции не закрылся');
    });

    it('Изменение разрешения экрана', async function() {
        await this.browser.setViewportSize({
            width: 1200,
            // запас 10px для edge@18 https://st.yandex-team.ru/SERP-85698
            height: 450,
        });

        await this.browser.yaWaitForHidden(
            PO.distrPopup(),
            'Попап-блок дистрибуции должен скрыться на маленьком экране',
        );

        await this.browser.setViewportSize({
            width: 1200,
            height: 1000,
        });

        await this.browser.yaWaitForVisible(
            PO.distrPopup(),
            'Попап-блок дистрибуции должен снова показаться на большом экране',
        );
    });

    it('Отображение попап-блока при скролле', async function() {
        let offsetDelta = 0;

        await this.browser.scroll(0, 100);

        const result1 = await this.browser.execute(function() {
            return $('.distr-popup').offset().top - $('.serp-user__login-link').offset().top;
        });

        offsetDelta = result1;
        await this.browser.scroll(0, -50);

        const result2 = await this.browser.execute(() => {
            return $('.distr-popup').offset().top - $('.serp-user__login-link').offset().top;
        });

        assert.equal(offsetDelta, result2, 'попап-попап не остаётся на одном месте на экране');
    });
});
