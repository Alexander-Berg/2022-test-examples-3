specs({
    feature: 'Accordion React',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=accordion/examples.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.blocks.turboAccordion())
            .assertView('plain', PO.blocks.turboAccordion0())
            .assertView('expanded', PO.blocks.turboAccordion1())
            .assertView('size-l', PO.blocks.turboAccordion2())
            .assertView('border-false', PO.blocks.turboAccordion3());
    });

    hermione.only.notIn('safari13');
    it('Аккордеон должен раскрываться и скрываться', function() {
        return this.browser
            .url('/turbo?stub=accordion/examples.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.blocks.turboAccordion())
            .assertView('plain', PO.blocks.turboAccordion())
            .click(PO.blocks.turboAccordion0.title())
            .yaWaitForVisible(PO.blocks.turboAccordion0.content(), 'Должен развернуться первый элемент')
            .assertView('expanded', PO.blocks.turboAccordion0())
            .click(PO.blocks.turboAccordion0.title())
            .yaWaitForHidden(PO.blocks.turboAccordion0.content(), 'Должен свернуться первый элемент')
            .assertView('collapsed', PO.blocks.turboAccordion0());
    });

    hermione.only.notIn('safari13');
    it('Разворот аккордеона сразу при наличии хэша', function() {
        return this.browser
            .url('/turbo?stub=page/anchor-infinite-1.json&exp_flags=force-react-accordion=1&patch=removeAjaxRelated#anchor-2')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.turboAccordion.content(), 1000, 'Содержимое первого элемента не развернулось');
    });

    hermione.only.notIn('safari13');
    it('Разворот аккордеона при изменении хэша', function() {
        return this.browser
            .url('/turbo?stub=page/anchor-infinite-1.json&exp_flags=force-react-accordion=1&patch=removeAjaxRelated')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForHidden(PO.blocks.turboAccordion.content(), 'Содержимое первого элемента уже развёрнуто')
            .url('/turbo?stub=page/anchor-infinite-1.json&exp_flags=force-react-accordion=1&patch=removeAjaxRelated#anchor-2')
            .yaWaitForVisible(PO.blocks.turboAccordion.content(), 500, 'Содержимое первого элемента не развернулось');
    });

    hermione.only.notIn('safari13');
    it('Разворот аккордеона при нажатии на якорную ссылку', function() {
        return this.browser
            .url('/turbo?stub=page/anchor-infinite-1.json&exp_flags=force-react-accordion=1&patch=removeAjaxRelated')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForHidden(PO.blocks.turboAccordion.content(), 'Содержимое первого элемента уже развёрнуто')
            .click('a[href="#anchor-2"]')
            .yaWaitForVisible(PO.blocks.turboAccordion.content(), 500, 'Содержимое первого элемента не развернулось');
    });

    hermione.only.notIn('safari13');
    it('Аккордеоны с разной темой на одной странице', function() {
        return this.browser
            .url('/turbo?stub=accordion/all-themes-example.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page.result());
    });
});
