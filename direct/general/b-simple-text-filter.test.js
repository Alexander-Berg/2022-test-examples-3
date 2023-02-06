describe('b-simple-text-filter', function() {
    var bemBlock;

    function createBlock() {
        var blockTree = u.getDOMTree({
            block: 'b-simple-text-filter',
            hashMap: [
                { id: 1, text: 'hello' },
                { id: 2, text: 'well' },
                { id: 3, text: 'yellow' }
            ]
                .reduce(function(hash, item) {
                    hash[item.id] = item.text;

                    return hash;
                }, {}),
            filterByKey: true
        });

        bemBlock = BEM.DOM.init(blockTree).bem('b-simple-text-filter');
    }

    beforeEach(function() {
        createBlock();
    });

    afterEach(function() {
        bemBlock.destruct();
    });

    describe('При инициализации фильтра ', function() {

        it('у блока input выставлен модификатор _focused_yes', function() {
            bemBlock.init();

            expect(bemBlock.findBlockOn('input', 'input').hasMod('focused', 'yes')).to.be.equal(true);
        });

    });

    describe('При сбросе фильтра ', function() {

        it('сбрасывается значение поля ввода', function() {
            bemBlock.findBlockOn('input', 'input').val('reset me plz');

            bemBlock.reset();

            expect(bemBlock.findBlockOn('input', 'input').val()).to.be.empty;
        });
    });

    describe('При введенном значении "hel"', function() {

        beforeEach(function() {
            bemBlock.findBlockOn('input', 'input').val('hel');
        });

        it('поиск по ключу 1 должен пройти', function() {
            expect(bemBlock.filter(1)).to.be.equal(true);
        });

        it('поиск по ключу 2 НЕ должен пройти', function() {
            expect(bemBlock.filter(2)).to.be.equal(false);
        });

        it('поиск по ключу 3 НЕ должен пройти', function() {
            expect(bemBlock.filter(3)).to.be.equal(false);
        });

    });

    describe('При введенном значении "ello"', function() {

        beforeEach(function() {
            bemBlock.findBlockOn('input', 'input').val('ello');
        });

        it('поиск по ключу 1 должен пройти', function() {
            expect(bemBlock.filter(1)).to.be.equal(true);
        });

        it('поиск по ключу 2 НЕ должен пройти', function() {
            expect(bemBlock.filter(2)).to.be.equal(false);
        });

        it('поиск по ключу 3 должен пройти', function() {
            expect(bemBlock.filter(3)).to.be.equal(true);
        });
    });

    describe('При введенном значении "2"', function() {

        beforeEach(function() {
            bemBlock.findBlockOn('input', 'input').val('2');
        });

        it('поиск по ключу 1 НЕ должен пройти', function() {
            expect(bemBlock.filter(1)).to.be.equal(false);
        });

        it('поиск по ключу 2 должен пройти', function() {
            expect(bemBlock.filter(2)).to.be.equal(true);
        });

        it('поиск по ключу 3 НЕ должен пройти', function() {
            expect(bemBlock.filter(3)).to.be.equal(false);
        });
    });
});
