describe('ns.page', function() {

    describe('.followRoute', function() {

        beforeEach(function() {
            this.sinon.stub(Jane.Services, 'run').callsFake(function(arr, cb) {
                cb();
            });
            this.sinon.stub(Jane.Services, 'hasNewVersion');
            this.sinon.stub(ns.page, 'forceReload');

            this.sinon.stub(Daria.Page, 'loadingHash').value('#sent');
            this.sinon.stub(ns.page, 'currentUrl').value('#inbox');
        });

        it('должен вызвать forceReload, если есть новая версия', function() {
            Jane.Services.hasNewVersion.returns(true);
            ns.page.followRoute();

            expect(ns.page.forceReload).to.have.callCount(1);
        });

        it('не должен вызвать forceReload, если есть новая версия, но текущий и будущий урлы совпадают', function() {
            Jane.Services.hasNewVersion.returns(true);
            this.sinon.stub(ns.page, 'currentUrl').value('#sent');

            ns.page.followRoute();

            expect(ns.page.forceReload).to.have.callCount(0);
        });

        it('не должен вызвать forceReload, если есть новая версия, но мы находимся в композе', function() {
            Jane.Services.hasNewVersion.returns(true);
            this.sinon.stub(ns.page, 'currentUrl').value('#compose2');

            ns.page.followRoute();

            expect(ns.page.forceReload).to.have.callCount(0);
        });

        it('не должен вызвать forceReload, если есть новая версия, но мы переходим на #done', function() {
            Jane.Services.hasNewVersion.returns(true);
            this.sinon.stub(Daria.Page, 'loadingHash').value('#done');

            ns.page.followRoute();

            expect(ns.page.forceReload).to.have.callCount(0);
        });

        it('не должен вызвать forceReload, если нет новой версии', function() {
            Jane.Services.hasNewVersion.returns(false);
            ns.page.followRoute();

            expect(ns.page.forceReload).to.have.callCount(0);
        });

    });

});
