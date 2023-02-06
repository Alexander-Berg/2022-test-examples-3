specs({
    feature: 'AdvertBottom',
}, () => {
    hermione.only.notIn('safari13');
    it('Показ залипающей плашки с рекламой', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась')
            .yaAssertViewportView('visible');
    });

    hermione.only.notIn('safari13');
    it('Скрытие и показ плашки', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась')
            .click(PO.blocks.advertBottomClose())
            .yaShouldNotBeVisible(PO.blocks.advertBottomVisible(), 'Плашка не исчезла по клику')
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForHidden(PO.blocks.advertBottomVisible(), 'Плашка появилась при повторном заходе')
            .execute(() => {
                sessionStorage.removeItem('turbo/sa-disabled');
            })
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась после очистки хранилища');
    });

    hermione.only.notIn('safari13');
    it('Плашка скрылась при проскролле', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage-10-200.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась')
            .yaScrollPageBy(250)
            .yaWaitForHidden(PO.blocks.advertBottomVisible(), 'Плашка не скрылась по проскроллу');
    });

    hermione.only.notIn('safari13');
    it('Плашка скрылась по таймауту', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage-3-200.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась')
            .pause(5000) // Ждем время таймера на скрытие
            .yaWaitForHidden(PO.blocks.advertBottomVisible(), 'Плашка не скрылась по таймауту');
    });

    hermione.only.notIn('safari13');
    it('Плашка не должна появляться, если реклама не загрузилась', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=fail')
            .yaScrollPageBy(300)
            .pause(1000) // Ждём секунду, вдруг появится.
            .yaWaitForHidden(PO.blocks.advertBottomVisible(), 'Плашка появилась');
    });

    hermione.only.notIn('safari13');
    it('Скрытие плашки при уничтожении вложенной в неё рекламы', function() {
        return this.browser
            .url('/turbo?stub=advertbottom%2Fpage.json&exp_flags=adv-disabled=0&hermione_advert=stub')
            .yaScrollPageBy(300)
            .yaWaitForVisible(PO.blocks.advertBottomVisible(), 'Плашка не появилась')
            .execute(function() {
                window.modules.require('advert__item', function(AdvertItem) {
                    // Имитация исчезновения рекламы.
                    AdvertItem.getFromDom(document.querySelector('.advert-bottom .advert__item'))._destroyBlock();
                });
            })
            .yaWaitForHidden(PO.blocks.advertBottomVisible(), 'Плашка не исчезла');
    });
});
