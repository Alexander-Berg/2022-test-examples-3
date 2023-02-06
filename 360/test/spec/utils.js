describe('Utils', function() {
    describe('#getSafePath ->', function() {
        [
            ['/foo/bar', '/foo/bar'],
            ['/а/б', '/%D0%B0/%D0%B1'],
            ['/foo/<script>', '/foo/%3Cscript%3E'],
            ['/foo/?', '/foo/%3F'],
            ['/foo/%', '/foo/%25'],
            ['/foo/#', '/foo/%23']
        ].forEach(function(test) {
            it(test[0] + ' -> ' + test[1], function() {
                expect(WidgetSaveApi.utils.getSafePath(test[0])).to.be.equal(test[1]);
            });
        });
    });
    describe('#getSafeParentPath ->', function() {
        [
            ['/foo/bar', '/foo'],
            ['/а/б', '/%D0%B0'],
            ['/foo/<script>', '/foo'],
            ['/foo/?/hello', '/foo/%3F'],
            ['/foo/%/hello', '/foo/%25']
        ].forEach(function(test) {
            it(test[0] + ' -> ' + test[1], function() {
                expect(WidgetSaveApi.utils.getSafeParentPath(test[0])).to.be.equal(test[1]);
            });
        });
    });
    describe('#getDialogOrigin ->', function() {
        [
            ['ru', 'https://disk.yandex.ru'],
            ['com', 'https://disk.yandex.com'],
            ['tr', 'https://disk.yandex.com.tr'],
            ['ua', 'https://disk.yandex.ua'],
            ['localhost', 'https://disk.yandex.ru']
        ].forEach(function(test) {
            it(test[0] + ' -> ' + test[1], function() {
                expect(WidgetSaveApi.utils.getDialogOrigin(test[0])).to.be.equal(test[1]);
            });
        });
    });
});
