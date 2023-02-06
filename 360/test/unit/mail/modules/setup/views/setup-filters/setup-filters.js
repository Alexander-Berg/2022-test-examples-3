describe('setup-filters', function() {
    beforeEach(function() {
        /*
        ns.Model.get('account-information').setData(mock['handler.account-information']);
        ns.Model.get('filters').setData({});
        ns.Model.get('filters-blacklist').setData({});
        ns.Model.get('filters-whitelist').setData({});
        ns.Model.get('folders').setData(mock['handler.folders']);
        ns.Model.get('labels').setData(mock['handler.labels']);
        ns.Model.get('settings').setData(mock['handler.settings']);
        */

        this.bSetupFiters = ns.View.create('setup-filters');
    });

    it('should create block setup-filters', function() {
        expect(this.bSetupFiters).to.be.ok;
    });

    xdescribe('#onhtmlinit', function() {
        beforeEach(function() {
            this.sinon.stub(nb, 'init');
        });

        xit('Должен инициализировать островные блоки', function() {
            return this.bSetupFiters.update().then(function() {
                expect(nb.init).to.have.callCount(1);
            }, this);
        });
    });
});
