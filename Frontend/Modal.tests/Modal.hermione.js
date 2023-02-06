describe('Modal', () => {
    it('static', function() {
        return this.browser
            .url('Modal/hermione/hermione.html?scenario=simple')
            .assertView('invisible', ['body'])
            .click('.Hermione-Trigger')
            .assertView('visible', ['body']);
    });

    it('visibility', function() {
        return this.browser
            .url('Modal/hermione/hermione.html?scenario=visibility')
            .assertView('initial', ['body'])
            .click('.Hermione-Trigger')
            .assertView('visible', ['body'])
            .moveToObject('body', 250, 250)
            .buttonDown()
            .buttonUp()
            .assertView('invisible', ['body']);
    });

    it('outsideclick', function() {
        // prettier-ignore
        return this.browser
            .url('Modal/hermione/hermione.html?scenario=simple')
            .click('.Hermione-Trigger')
            // Нажимаем курсор в модалке и отпускаем за пределами модалки.
            .moveToObject('.Modal-Content')
            .buttonDown()
            .moveToObject('body', 250, 250)
            .buttonUp()
            .assertView('visible', ['body'])
            // Уводим курсор за пределы модалки и нажимаем.
            .moveToObject('body', 250, 250)
            .buttonDown()
            .buttonUp()
            .assertView('invisible', ['body']);
    });

    [10, 50].forEach((bodyLines) => {
        [10, 50].forEach((modalLines) => {
            ['auto', 'scroll'].forEach((overflow) => {
                it(`body-lines-${bodyLines}-modal-lines-${modalLines}-overflow-${overflow}`, function() {
                    const qs = `bodyLines=${bodyLines}&modalLines=${modalLines}&overflow=${overflow}`;

                    return this.browser
                        .url(`Modal/hermione/hermione.html?scenario=scrollbar&${qs}`)
                        .assertView('invisible', ['html'])
                        .click('.Hermione-Trigger')
                        .assertView('visible', ['html']);
                });
            });
        });
    });
});
