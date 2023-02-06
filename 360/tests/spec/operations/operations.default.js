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
    require('./delete');
    require('./publish');
    require('./copy');
    require('./move');
    require('./restore');
    require('./rename');
    require('./get-contacts');

    // альбом внутри Диска
    require('./publish-album');
    require('./edit-album');
    require('./remove-album');
    require('./remove-resource-album');

    require('./create-folder');
});
