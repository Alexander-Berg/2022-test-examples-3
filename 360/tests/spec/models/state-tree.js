import 'models/tree/state-tree';

describe('Модель состояния дерева папок', () => {
    beforeEach(function() {
        this.state = ns.Model.get('stateTree');
    });
    it('должна по дефолту хранить /disk', function() {
        expect(this.state.get('.selected')).to.be.eql('/disk');
    });

    describe('#select', () => {
        beforeEach(function() {
            this.state.select('/disk/foo');
        });
        it('должна поставить значени в поле .selected', function() {
            expect(this.state.get('.selected')).to.be.eql('/disk/foo');
        });
    });

    describe('#deselet', () => {
        beforeEach(function() {
            this.state.deselect();
        });
        it('должна сбросить .selected до дефолтного /disk', function() {
            expect(this.state.get('.selected')).to.be.eql('/disk');
        });
    });
});
