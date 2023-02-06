describe('Drawer', () => {
    hermione.skip.in(['win-ie11']);
    it('big', function() {
        return this.browser
            .url('DrawerBig/hermione/hermione.html')
            .click('[data-testid="opener"]')
            .pause(200)
            .assertView('visible', ['body', '.Drawer'])
            .click('.Drawer-Overlay')
            .assertView('invisible', ['body']);
    });

    hermione.skip.in(['win-ie11']);
    it('nested', function() {
        return this.browser
            .url('DrawerNested/hermione/hermione.html')
            .click('[data-testid="opener-primary"]')
            .pause(400)
            .assertView('visible-primary', ['body', '.Drawer'])
            .click('[data-testid="opener-secondary"]')
            .pause(400)
            .assertView('visible-secondary', ['body', '.Drawer.Drawer_nested'])
            .moveToObject('body', 250, 250)
            .buttonDown()
            .buttonUp()
            .pause(400)
            .assertView('invisible-secondary', ['body', '.Drawer'])
            .buttonDown()
            .buttonUp()
            .pause(400)
            .assertView('invisible-primary', ['body']);
    });
});
