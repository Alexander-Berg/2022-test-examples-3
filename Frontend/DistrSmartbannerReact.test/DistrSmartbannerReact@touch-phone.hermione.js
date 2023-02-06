'use strict';

const PO = require('./DistrSmartbannerReact.page-object');
hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении и на винфонах');

specs({ feature: 'Смартбаннер React' }, function() {
    describe('Вертикальная ориентация', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'дарт вейдер',
                foreverdata: 1239240421,
                exp_flags: ['smartbanner_atom=1'],
                data_filter: false,
            }, PO.smartInfo());
        });
        it('Внешний вид', async function() {
            await this.browser.yaAssertViewIsolated('default', PO.smartInfo());
        });
        it('При показе саджеста смартбаннер должен скрываться', async function() {
            await this.browser.yaTouch(PO.yandexSearch.input.clear());
            await this.browser.setValue(PO.yandexSearch.input.control(), 'энакен скайуокер');
            await this.browser.yaWaitForVisible(PO.miniSuggest.content(), 'Должен открыться саджест');
            await this.browser.yaWaitForHidden(PO.smartInfo(), 'смартбаннер не исчез');
        });
        it('При открытии бургер-меню смартбаннер должен скрываться', async function() {
            await this.browser.yaWaitForVisible(PO.smartInfo(), 'смартбаннер не появился на выдаче');
            await this.browser.yaOpenBurgerMenu(PO.headerBurgerButton(), PO.burgerMenu());
            await this.browser.yaWaitForHidden(PO.smartInfo(), 'смартбаннер не исчез');
        });
        it('При скролле смартбаннер должен остаться на выдаче', async function() {
            await this.browser.yaWaitForVisible(PO.footer());
            await this.browser.yaScroll(500);
            await this.browser.yaShouldBeVisible(PO.smartInfo());
        });
        it('При скролле до футера смартбаннер должен скрываться', async function() {
            await this.browser.yaWaitForVisible(PO.footer());
            await this.browser.yaShouldBeVisible(PO.smartInfo(), 'смартбаннер был скрыт преждевременно');
            await this.browser.yaScroll(PO.footer());
            await this.browser.yaWaitForHidden(PO.smartInfo(), 'смартбаннер не исчез');
        });
        it('По клику на кнопку \'Скачать\' смартбаннер должен скрываться', async function() {
            await this.browser.yaCheckBaobabCounter(PO.smartInfo.action(), {
                path: '/$page/distr-smartbanner/ok',
            });

            await this.browser.yaWaitForHidden(PO.smartInfo());
        });
        it('По клику на кнопку \'Скачать\' ссылка открывается в текущей вкладке', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.smartInfo.action(),
                target: '_self',
            });
        });
        it('По клику на кнопку \'Закрыть\' смартбаннер должен скрываться', async function() {
            await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
                path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic"]',
            });

            await this.browser.yaWaitForHidden(PO.smartInfo());
        });
    });

    describe('Горизонтальная ориентация', function() {
        hermione.only.notIn(['iphone'], 'orientation is not supported');
        it('При перекрывании табов смартбаннер должен скрываться', async function() {
            await this.browser.yaOpenSerp({
                text: 'дарт вейдер',
                foreverdata: 2162081419,
                exp_flags: ['smartbanner_atom=1'],
                data_filter: false,
            }, PO.smartInfo());

            await this.browser.setOrientation('landscape');
            await this.browser.yaWaitForHidden(PO.smartInfo(), 'смартбаннер не исчез');
            await this.browser.yaScroll(500);
            await this.browser.yaShouldBeVisible(PO.smartInfo());
        });
    });

    it('Выключение смартбаннера', async function() {
        await this.browser.yaOpenSerp({
            text: 'дарт вейдер',
            foreverdata: 2162081419,
            exp_flags: [
                'smartbanner_atom=1',
                'distr_hide_smartbanner=1',
            ],
            data_filter: false,
        }, PO.page());

        await this.browser.yaShouldNotBeVisible(PO.smartInfo(), 'Смартбаннер не должен показаться');
    });

    it('Выключение смартбаннера в Тыкве', async function() {
        await this.browser.yaOpenPumpkin({
            text: 'дарт вейдер',
            foreverdata: 2162081419,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await this.browser.yaShouldNotBeVisible(PO.smartInfo(), 'Смартбаннер не должен показаться');
    });

    describe('Якорь для незрячих', function() {
        it('Должен иметь ссыку для незрячих', async function() {
            await this.browser.yaOpenSerp({
                text: 'дарт вейдер',
                foreverdata: 1239240421,
                exp_flags: ['smartbanner_atom=1'],
                data_filter: false,
            }, PO.smartInfo());

            const hasLink = await this.browser.execute(function(selector) {
                return Boolean(document.querySelector(selector));
            }, '.a11y-smartbanner-anchor');
            assert.isTrue(hasLink, 'ссылка не найдена');
        });

        it('Не должен иметь ссыку для незрячих когда смартбаннер скрыт', async function() {
            await this.browser.yaOpenSerp({
                text: 'дарт вейдер',
                foreverdata: 1239240421,
                exp_flags: [
                    'smartbanner_atom=1',
                    'distr_hide_smartbanner=1',
                ],
                data_filter: false,
            }, PO.page());

            const hasLink = await this.browser.execute(function(selector) {
                return Boolean(document.querySelector(selector));
            }, '.a11y-smartbanner-anchor');
            assert.isFalse(hasLink, 'ссылка не найдена');
        });
    });

    it('Заголовок и текст сниппета должен поддерживать html', async function() {
        await this.browser.yaOpenSerp({
            text: 'txt',
            foreverdata: 111180584,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await this.browser.yaAssertViewIsolated('html-support', PO.smartInfo());
    });
});
