describe('Daria.mSetupIndexMenu', function() {
    function getIds(itemsData) {
        return itemsData.map(function(item) {
            return item.id || item.type;
        });
    }

    function getOtherItemsIds(itemsData) {
        var ids = [];

        itemsData.every(function(item) {
            if (item.type === 'other-items') {
                ids = getIds(item.items || []);

                return false;
            }

            return true;
        });

        return ids;
    }

    afterEach(function() {
        delete Daria.Config.allowThemes;
        delete Daria.Config.TLD;
    });

    it('Должен вернуть все пункты', function() {
        Daria.Config.allowThemes = true;

        expect(getIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql([
            'interface',
            'sender',
            'collectors',
            'folders',
            'filters',
            'security',
            'unsubscribe-filters',
            'separator',
            'abook',
            'other-items',
            'todo'
        ]);
    });

    it('Должен вернуть все пункты, кроме "interface"', function() {
        expect(getIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql([
            'sender',
            'collectors',
            'folders',
            'filters',
            'security',
            'unsubscribe-filters',
            'separator',
            'abook',
            'other-items',
            'todo'
        ]);
    });

    it('Должен вернуть все пункты, включая backup и beautiful-email', function() {
        Daria.Config.allowThemes = true;
        Daria.Config.TLD = 'ru';

        expect(getIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql([
            'interface',
            'sender',
            'beautiful-email',
            'collectors',
            'folders',
            'filters',
            'security',
            'unsubscribe-filters',
            'backup',
            'separator',
            'abook',
            'other-items',
            'todo'
        ]);
    });

    it('Должен вернуть все пункты, кроме "collectors" и "unsubscribe-filters"', function() {
        Daria.Config.allowThemes = true;
        this.sinon.stub(Daria, 'IS_CORP').value(true);

        expect(getIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql([
            'interface',
            'sender',
            'folders',
            'filters',
            'security',
            'separator',
            'abook',
            'other-items',
            'todo'
        ]);
    });

    it('Должен вернуть все пункты, кроме "interface", "collectors" и "unsubscribe-filters"', function() {
        this.sinon.stub(Daria, 'IS_CORP').value(true);

        expect(getIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql([
            'sender',
            'folders',
            'filters',
            'security',
            'separator',
            'abook',
            'other-items',
            'todo'
        ]);
    });

    it('Должен вернуть все пункты "other-items"', function() {
        expect(getOtherItemsIds(ns.Model.get('setup-index-menu').data['setup-index-items'])).to.be.eql(
            [ 'client', 'other' ]
        );
    });

    it('Должен вернуть все пункты "other-items" кроме "client"', function() {
        Daria.Config['pdd-domain'] = 'domain';

        var items = getOtherItemsIds(ns.Model.get('setup-index-menu').data['setup-index-items']);

        delete Daria.Config['pdd-domain'];

        expect(items).to.be.eql([ 'other' ]);
    });
});
