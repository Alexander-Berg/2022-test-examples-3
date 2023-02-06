describe('b-crypta-inventory', function() {

    var block,
        sandbox;

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-crypta-inventory',
            data: options.data || {}
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('js', function() {

        describe('При инициализации', function() {

            before(function() {
                createBlock();
            });

            after(function() {
                destructBlock();
            });

            it('Содержит элемент "Копировать"', function() {
                expect(block).to.haveElem('copy');
            });

            it('Содержит элемент "Прогноз"', function() {
                expect(block).to.haveElem('predictor');
            });

            it('Содержит элемент "Рекомендации"', function() {
                expect(block).to.haveElem('advisor');
            });

        });

    });

});
