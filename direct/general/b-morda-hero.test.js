describe('b-morda-hero', function() {
    var block;

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-morda-hero',
            mods: options.mods,
            links: {
                newCamp: '#'
            },
            support: {
                phone: '777-777'
            }
        });
    }

    describe('Наличие элементов', function() {

        beforeEach(function() {
            createBlock({ mods: { type: '1' } });
        });

        afterEach(function() {
            block.destruct();
        });

        it('Отображается телефон', function() {
            expect(block.elem('note').text().indexOf('777-777')).to.not.eq(-1);
        });

    });

});
