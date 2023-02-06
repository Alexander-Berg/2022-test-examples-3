specs({
    feature: 'LcTabs',
}, () => {
    it('Стрелки появляются', function() {
        return this.browser
            .url('/turbo?stub=lctabs/with-scroll.json')
            .yaWaitForVisible(PO.lcTabs(), 'Табы не появились')
            .yaShouldNotBeVisible(PO.lcTabs.arrowLeft(), 'Появилась левая стрелка')
            .yaShouldBeVisible(PO.lcTabs.arrowRight(), 'Не появилась правая стрелка')
            .click(PO.lcTabs.arrowRight())
            .pause(700) // время анимации скрола
            .assertView('plain', PO.lcTabs())
            .click(PO.lcTabs.arrowRight())
            .pause(700 + 200) // время анимации скрола + время исчезновения стрелки
            .yaShouldNotBeVisible(PO.lcTabs.arrowRight(), 'Не исчезла правая стрелка')
            .yaShouldBeVisible(PO.lcTabs.arrowLeft(), 'Исчезла левая стрелка');
    });
});
