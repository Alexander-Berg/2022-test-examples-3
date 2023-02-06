'use strict';

specs({
    feature: 'Popup на СЕРПе',
    type: 'Справа сверху',
}, function() {
    beforeEach(async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'test',
            foreverdata: '118631519',
            data_filter: 'distr-popup',
        }, PO.page(), { staticHeader: false });

        await this.browser.execute(function(selector) {
            $(selector).removeClass('distr-popup_animation_fade-show');
        }, PO.distrPopup());

        await this.browser.yaWaitForVisible(PO.distrPopup());
    });

    it('Клик на кнопку установки', async function() {
        const PO = this.PO;

        const url = await this.browser.yaParseHref(PO.distrPopup.installButton(), { target: '_self' });
        assert.include(url.href, 'https://passport.yandex.ru/passport', 'Ссылка должна вести на паспорт');
        assert.include(url.href, 'retpath=', 'В ссылке должен быть retpath');

        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/click',
        });
    });

    it('Изменения размера окна браузера', async function() {
        const PO = this.PO;

        await this.browser.setViewportSize({
            width: 850,
            height: 1000,
        });

        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Попап-блок дистрибуции не закрылся');

        await this.browser.setViewportSize({
            width: 1200,
            height: 1000,
        });

        await this.browser.yaWaitForVisible(PO.distrPopup(), 3000, 'Попап-блок дистрибуции не показался');
    });

    it('Взаимодействие с саджестом', async function() {
        const PO = this.PO;

        await this.browser.click(PO.header.arrow.input.control());
        await this.browser.yaWaitForVisible(PO.mainSuggest.content(), 'Должен открыться саджест');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 'Попап-блок дистрибуции не закрылся');
        await this.browser.moveToObject('body', 10, 10);
        await this.browser.buttonDown();
        await this.browser.yaWaitForHidden(PO.mainSuggest.content(), 'Саджест не скрылся');
        await this.browser.yaShouldNotBeVisible(PO.distrPopup(), 'Попап-блок дистрибуции не должен показываться');
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

    describe('При авторизованном пользователе', function() {
        it('Взаимодействие с блоком пользователя', async function() {
            const PO = this.PO;

            await this.browser.yaOpenSerp({
                srcskip: 'YABS_DISTR',
                yandex_login: 'User1',
                text: 'test',
                foreverdata: '2982326350',
                data_filter: 'distr-popup',
            }, PO.page(), { staticHeader: false });

            await this.browser.execute(function(selector) {
                $(selector).removeClass('distr-popup_animation_fade-show');
            }, PO.distrPopup());

            await this.browser.yaWaitForVisible(PO.distrPopup());
            await this.browser.click(PO.user());
            await this.browser.yaWaitForVisible(PO.userPopup());

            await this.browser.execute(function(selector) {
                // В тесты проверяем, что юзер перекрывают попап диструбуции.
                // Так как содержимое попапа может меняться, скрываем его содержимое через прозрачность

                // Это нужно, чтобы iframe через postmessage не обновил высоту блока
                $('.user2').bem('user2').updateUserHeight = () => {};
                $(selector).css('opacity', 0);
                $(selector).css('height', 400);
            }, PO.userContent());

            await this.browser.assertView('right-only', [PO.distrPopupFlat(), PO.userPopup()], {
                allowViewportOverflow: true,
                excludeElements: PO.distrPopup(),
            });
        });
    });
});
