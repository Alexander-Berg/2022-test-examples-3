specs({
    feature: 'beruSelect',
}, () => {
    hermione.only.notIn('safari13');
    it('Проверка функциональности блока', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/default.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');

        const selectedOptions = await browser.elements(PO.blocks.beruPopup.selectedOptions());
        assert.equal(selectedOptions.value.length, 0, 'Опции должны быть не выбраны');

        const optionTitle = await browser.element(PO.blocks.beruPopup.selectFirtsOption()).getText();

        await browser.click(PO.blocks.beruPopup.selectFirtsOption());
        await browser.yaWaitForHidden(PO.blocks.beruPopup(), 5, 'Дропдаун не закрылся');

        const buttonText = await browser.element(PO.blocks.beruSelect.trigger()).getText();
        assert.equal(optionTitle, buttonText, 'Текст кнопки должен измениться на текст выбранной опции');

        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        const activeOption = await browser.element(PO.blocks.beruPopup.selectedFirsOption());
        assert.ok(activeOption.value, 'Первая опция должна быть активна');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид в свернутом состоянии с не выбранной опцией', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/default.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.assertView('no-selected', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид в развернутом состоянии с не выбранной опцией', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/default.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        await browser.assertView('popup-no-selected-option', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Опция может быть ссылкой и должна быть кликабельна', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/default.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        await browser.yaCheckLink({
            message: 'Ссылка должна быть кликабельна',
            selector: PO.blocks.beruPopup.selectSecondOption(),
            url: {
                href: '/turbo?stub=beruheader/default.json',
                ignore: ['protocol', 'hostname'],
            },
        });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид в свернутом состоянии с выбранной опцией', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/preselected.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.assertView('pre-selected', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид в развернутом состоянии с выбранной опцией', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/preselected.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        await browser.assertView('popup-pre-selected-option', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Дропдаун должен показываться над кнопкой если места показаться с низу не хватает', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=beruselect/openup.json');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        await browser.assertView('open-up', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Должен корректно отображаться в iframe', async function() {
        const { browser } = this;

        await browser.yaOpenInIframe('?stub=beruselect/openup.json');
        await browser.yaWaitForVisible(PO.blocks.beruSelect(), 'BeruSelect не загрузился');
        await browser.click(PO.blocks.beruSelect.trigger());
        await browser.yaWaitForVisible(PO.blocks.beruPopup(), 5, 'Дропдаун не показался');
        await browser.assertView('iframe-render', PO.page());
    });
});
