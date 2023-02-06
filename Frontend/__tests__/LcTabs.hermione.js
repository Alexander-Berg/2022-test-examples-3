specs({
    feature: 'LcTabs',
}, () => {
    describe('Размеры табов', () => {
        function checkSize(browser, size) {
            return browser
                .url(`/turbo?stub=lctabs/size-${size}.json`)
                .yaWaitForVisible(PO.lcTabs(), 'Табы не появились')
                .assertView('plain', PO.lcTabs());
        }

        hermione.only.notIn('safari13');
        it('Размер XS', function() {
            return checkSize(this.browser, 'xs');
        });

        hermione.only.notIn('safari13');
        it('Размер S', function() {
            return checkSize(this.browser, 's');
        });

        hermione.only.notIn('safari13');
        it('Размер M', function() {
            return checkSize(this.browser, 'm');
        });

        hermione.only.notIn('safari13');
        it('Размер L', function() {
            return checkSize(this.browser, 'l');
        });

        hermione.only.notIn('safari13');
        it('Размер XL', function() {
            return checkSize(this.browser, 'xl');
        });
    });

    describe('Темы табов', () => {
        hermione.only.notIn('safari13');
        it('Тема "табы"', function() {
            return this.browser
                .url('/turbo?stub=lctabs/theme-tabs.json')
                .yaWaitForVisible(PO.lcTabs(), 'Табы без подчеркивания не появились')
                .assertView('regular', PO.lcTabs())
                .url('/turbo?stub=lctabs/theme-tabs-underline.json')
                .yaWaitForVisible(PO.lcTabs(), 'Табы с подчеркиванием не появились')
                .assertView('underlined', PO.lcTabs());
        });

        hermione.only.notIn('safari13');
        it('Тема "кнопки"', function() {
            return this.browser
                .url('/turbo?stub=lctabs/theme-buttons.json')
                .yaWaitForVisible(PO.lcTabs(), 'Табы не появились')
                .assertView('plain', PO.lcTabs());
        });
    });

    describe('Скрол табов', () => {
        hermione.only.notIn('safari13');
        it('Табы скролятся', function() {
            return this.browser
                .url('/turbo?stub=lctabs/with-scroll.json')
                .yaWaitForVisible(PO.lcTabs(), 'Табы не появились')
                .yaShouldBeScrollable(PO.lcTabs.scrollWrapper(), { h: true, v: false });
        });
    });

    describe('Табы с контентом', () => {
        hermione.only.notIn('safari13');
        it('Табы открываются', function() {
            return this.browser
                .url('/turbo?stub=lctabs/with-content.json')
                .yaWaitForVisible(PO.lcTabs.contentWrapper(), 'Контент табов не появился')
                .assertView('plain', PO.lcTabs())
                .yaShouldNotBeVisible(PO.lcTabs.lastContent(), 'Появился не тот контент')
                .click(PO.lcTabs.lastTab())
                .yaShouldBeVisible(PO.lcTabs.lastContent(), 'Контент таба не появился');
        });
    });

    describe('Вертикальные табы с контентом', () => {
        hermione.only.notIn('safari13');
        it('Табы открываются', function() {
            return this.browser
                .url('/turbo?stub=lctabs/vertical-with-content.json')
                .yaWaitForVisible(PO.lcTabs.contentWrapper(), 'Контент табов не появился')
                .assertView('plain', PO.lcTabs())
                .yaShouldNotBeVisible(PO.lcTabs.lastContent(), 'Появился не тот контент')
                .click(PO.lcTabs.lastTab())
                .yaShouldBeVisible(PO.lcTabs.lastContent(), 'Контент таба не появился');
        });
    });

    describe('Вертикальные нумерованые табы с контентом', () => {
        hermione.only.notIn('safari13');
        it('Табы открываются', function() {
            return this.browser
                .url('/turbo?stub=lctabs/vertical-numeric-with-content.json')
                .yaWaitForVisible(PO.lcTabs.contentWrapper(), 'Контент табов не появился')
                .assertView('plain', PO.lcTabs())
                .yaShouldNotBeVisible(PO.lcTabs.lastContent(), 'Появился не тот контент')
                .click(PO.lcTabs.lastTab())
                .yaShouldBeVisible(PO.lcTabs.lastContent(), 'Контент таба не появился');
        });
    });
});
