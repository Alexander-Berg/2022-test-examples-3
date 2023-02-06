specs('Feature two', async function () {
    // 9da3f20
    it('Some it test', async function () {
        await this.browser.yaOpenSerp({}, PO.list());
    });
    // 2782e49
    it("Another it case", async function () {
        await this.browser.yaOpenSerp({}, PO.list());
    });
});
