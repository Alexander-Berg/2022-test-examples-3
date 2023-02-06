specs('Feature one', async function () {
    describe('Describe in specs', async function () {
        // b9696af
        it('The first it case', async function () {
            await this.browser.yaOpenSerp({ foreverdata: '1234', text: 'something', exp_flags: 'font-size=10' }, PO.list());
        });
        // 3b2105f
        it('The second it case', async function () {
            await this.browser.yaOpenSerp({ foreverdata: '1234', text: 'something2', exp_flags: ['hideads=1', 'font-size=10'] }, PO.list());
        });
        // c8cedc7
        it('The last it case', async function () {
            await this.browser.yaOpenSerp({
                foreverdata: '1579473100',
                text: 'another',
                exp_flags: [
                    'reviews=1',
                    'url_font-size=20'
                ]
            }, PO.list());
        });
    });
});
