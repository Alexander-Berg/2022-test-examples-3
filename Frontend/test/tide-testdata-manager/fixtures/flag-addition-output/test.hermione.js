specs('Feature one', async function () {
    describe('Describe in specs', async function () {
        // b9696af
        it('The first it case', async function () {
            await this.browser.yaOpenSerp({ exp_flags: ['universe=42', 'font-size=10'], foreverdata: '1234', text: 'something' }, PO.list());
        });
        // 3b2105f
        it('The second it case', async function () {
            await this.browser.yaOpenSerp({ exp_flags: ['universe=42', 'hideads=1', 'font-size=10'], foreverdata: '1234', text: 'something2' }, PO.list());
        });
        // c8cedc7
        it('The last it case', async function () {
            await this.browser.yaOpenSerp({ exp_flags: ['universe=42', 'reviews=1', 'url_font-size=20'], foreverdata: '1579473100', text: 'another' }, PO.list());
        });
    });
});
