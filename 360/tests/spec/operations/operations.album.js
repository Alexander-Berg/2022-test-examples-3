describe('Операция', () => {
    beforeEach(() => {
        createFakeXHR();
        sinon.stub(ns.page, 'go', ns.nop);
    });

    afterEach(() => {
        deleteFakeXHR();
        ns.page.go.restore();
    });

    require('./common');
    require('./publish-album');
    require('./save-album');
    require('./edit-album');
    require('./remove-resource-album');
});
