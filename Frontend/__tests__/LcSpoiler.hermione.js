specs({
    feature: 'LcSpoiler',
}, () => {
    hermione.only.notIn('safari13');
    it('Расхлоп с границами, стрелками и иконками', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/default.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .yaCheckClientErrors()
            .assertView('default', PO.lcSpoiler());
    });

    hermione.only.notIn(['safari13', 'firefox']);
    it('Расхлоп с открытым пунктом', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/default.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .click(PO.blocks.lcSpoiler.firstItem())
            .yaWaitForVisible(PO.blocks.lcSpoiler.firstItemText(), 'Пункт не открылся')
            .yaCheckClientErrors()
            .assertView('default-one-opened', PO.lcSpoiler());
    });

    hermione.only.notIn(['safari13', 'firefox']);
    it('Расхлоп с двумя открытыми пунктами', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/default.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .click(PO.blocks.lcSpoiler.firstItem())
            .yaWaitForVisible(PO.blocks.lcSpoiler.firstItemText(), 'Первый пункт не открылся')
            .yaCheckClientErrors()
            .click(PO.blocks.lcSpoiler.secondItem())
            .yaWaitForVisible(PO.blocks.lcSpoiler.secondItemText(), 'Второй пункт не открылся')
            .yaCheckClientErrors()
            .assertView('default-two-opened', PO.lcSpoiler());
    });

    hermione.only.notIn(['safari13', 'firefox']);
    it('Расхлоп с максимум одним открытым пунктом', async function() {
        await this.browser
            .url('/turbo?stub=lcspoiler/only-one-open.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .click(PO.blocks.lcSpoiler.firstItem())
            .yaWaitForVisible(PO.blocks.lcSpoiler.firstItemText(), 'Первый пункт не открылся')
            .yaCheckClientErrors()
            .click(PO.blocks.lcSpoiler.secondItem())
            .yaWaitForVisible(PO.blocks.lcSpoiler.secondItemText(), 'Второй пункт не открылся')
            .yaCheckClientErrors()
            // Скроллим до конца страницы и потом до элемента, иначе тесты флапают с неккоректным склеиванием
            // Кастомные методы нивелируют разницу в работе scroll на тачах и десктопах
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcSpoiler())
            .assertView('only-one-open', PO.lcSpoiler());
    });

    hermione.only.notIn('safari13');
    it('Расхлоп с выравниванием по центру', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/align-center.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .assertView('align-center', PO.lcSpoiler());
    });

    hermione.only.notIn('safari13');
    it('Расхлоп большого размера', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/size-xl.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .assertView('size-xl', PO.lcSpoiler());
    });

    hermione.only.notIn('safari13');
    it('Расхлоп без иконок', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/without-icons.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .assertView('without-icons', PO.lcSpoiler());
    });

    hermione.only.notIn('safari13');
    it('Расхлоп без стрелок', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/without-arrow.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .assertView('without-arrow', PO.lcSpoiler());
    });

    hermione.only.notIn('safari13');
    it('Расхлоп без границ между пунктами', function() {
        return this.browser
            .url('/turbo?stub=lcspoiler/without-border.json')
            .yaWaitForVisible(PO.lcSpoiler(), 'Расхлоп не появился')
            .assertView('without-border', PO.lcSpoiler());
    });
});
