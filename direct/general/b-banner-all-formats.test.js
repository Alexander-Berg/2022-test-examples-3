describe('b-banner-all-formats', function() {
    var sandbox,
        block;

    function createBlock(options) {
        block = u.getInitedBlock({
            block: 'b-banner-all-formats',
            previews: options.previews,
            active: options.active
        }, true);
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct && block.destruct();
    });

    describe('Зависимость от выходных параметров', function(){

        it('Если превью одно, то табов нет', function() {
            createBlock({
                previews: [
                    {
                        name: 'context',
                        data: {}
                    }
                ]
            });

            expect(block).to.not.haveBlock('tabs');
        });

        it('Если несколько превью, то табы есть', function() {
            createBlock({
                previews: [
                    {
                        name: 'context',
                        data: {}
                    },
                    {
                        name: 'video',
                        data: {}
                    }
                ]
            });

            expect(block).to.haveBlock('tabs');
        });

        it('По умолчанию активна первая вкладка', function() {
            createBlock({
                previews: [
                    {
                        name: 'context',
                        data: {}
                    },
                    {
                        name: 'video',
                        data: {}
                    }
                ]
            });

            var tabsPanes = block.findBlockInside('tabs-panes');

            expect(tabsPanes).to.haveMod(tabsPanes.findPane().eq(0), 'active', 'yes');
        });

        it('При переданном active выбирается нужная владка', function() {
            createBlock({
                active: 'video',
                previews: [
                    {
                        name: 'context',
                        data: {}
                    },
                    {
                        name: 'video',
                        data: {}
                    }
                ]
            });

            var tabsPanes = block.findBlockInside('tabs-panes');

            expect(tabsPanes).to.haveMod(tabsPanes.findPane().eq(1), 'active', 'yes');
        });

    });

});
