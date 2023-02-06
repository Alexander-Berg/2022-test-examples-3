'use strict';

const PO = require('./VisitsHistogram.page-object').touchPhone;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Гистограмма посещаемости',
}, function() {
    hermione.also.in(['safari13', 'iphone-dark']);
    it('Основные проверки', async function() {
        await this.browser.yaOpenSerp(
            { text: 'кафе пушкин', data_filter: 'companies' },
            PO.oneOrg(),
        );

        await this.browser.yaShouldNotBeVisible(PO.oneOrg.visitsHistogram());
        await this.browser.yaOpenOverlayAjax(
            PO.oneOrg.tabsMenu.about(),
            PO.overlay.oneOrg(),
            'Оверлей не показался',
        );
        await this.browser.yaWaitForVisible(PO.overlay.oneOrg.visitsHistogram());
        // иначе скриншот ниже мигает (отзывы дозагружаются асинхронно и влияют на высоту контента)
        await this.browser.yaWaitForVisible(PO.overlay.oneOrg.reviewsPreview(), 'Отзывы не загрузились в оверлее');

        await this.browser.yaScrollOverlay(PO.overlay.oneOrg.visitsHistogram(), { offsetTop: 100 });
        await this.browser.assertView('plain', PO.overlay.oneOrg.visitsHistogram());

        // TODO: разобраться, как проверять баобаб-счётчик скролла SERP-97686
        // await this.browser.yaCheckBaobabCounter(
        //     () => this.browser.yaScrollContainer(PO.overlay.oneOrg.visitsHistogram.scroller.wrap(), 0),
        //     {
        // eslint-disable-next-line max-len
        //         path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/visits-histogram/scroller',
        //     }
        // );
    });

    hermione.also.in('safari13');
    it('Подскролл к теущему дню', async function() {
        await this.browser.yaOpenSerp(
            { text: 'ТРЦ Европолис Спб', lr: '2', data_filter: 'companies' },
            PO.oneOrg(),
        );
        await this.browser.yaOpenOverlayAjax(
            PO.oneOrg.tabsMenu.about(),
            PO.overlay.oneOrg(),
            'Оверлей не показался',
        );
        await this.browser.yaShouldBeVisible(PO.overlay.oneOrg.visitsHistogram());

        // Подскролл к текущему дню после открытия сайдблока
        await this.browser.yaWaitUntil('Не изменилось значение скролла гистограммы', async () => {
            const value = await this.browser.execute(
                function(selector) {
                    return document.querySelector(selector).scrollLeft;
                },
                PO.overlay.oneOrg.visitsHistogram.scroller.wrap(),
            );
            return Number(value) > 0;
        }, 1500);

        // Подскролл к текущему дню после повторного открытия сайдблока
        await this.browser.click(PO.overlay.back());
        await this.browser.yaWaitForVisible(PO.oneOrg());
        await this.browser.click(PO.oneOrg.tabsMenu.about());
        await this.browser.yaShouldBeVisible(PO.overlay.oneOrg.visitsHistogram());
        await this.browser.yaWaitUntil(
            'Не изменилось значение скролла гистограммы после переоткрытия сайдблока',
            async () => {
                const value = await this.browser.execute(
                    function(selector) {
                        return document.querySelector(selector).scrollLeft;
                    },
                    PO.overlay.oneOrg.visitsHistogram.scroller.wrap(),
                );
                return Number(value) > 0;
            },
            1500,
        );
    });
});
