describe('b-infoblock-collapser', function() {
    var block,
        createBlock = function(ctx) {
            block = u.createBlock(ctx);
        },
        sandbox = sinon.sandbox.create({ useFakeTimers: true });

    afterEach(function() {
        sandbox.restore();
        block.destruct();
    });

    describe('Начальное состояние', function() {
        it('Если состояние collapsed, у блока появляется модификатор collapsed', function() {
            createBlock({
                block: 'b-infoblock-collapser',
                state: 'collapsed'
            });

            expect(block).to.haveMod('state', 'collapsed');
        });
        it('Если состояние uncollapsed,  у блока появляется модификатор uncollapsed', function() {
            createBlock({
                block: 'b-infoblock-collapser',
                state: 'uncollapsed'
            });

            expect(block).to.haveMod('state', 'uncollapsed');
        });
    });

    describe('Переключение состояний', function() {
        it('Если состояние collapsed и нажимаем кнопку, триггерится событие "развернуть блок"', function() {
            createBlock({
                block: 'b-infoblock-collapser',
                state: 'collapsed'
            });

            sandbox.spy(block, 'trigger');

            block.findBlockInside('link', 'link').trigger('click');

            expect(block.trigger.calledWith('stateChanged', { collapsed: 'no' }));
        });
        it('Если состояние uncollapsed и нажимаем кнопку, триггерится событие "свернуть блок"', function() {
            createBlock({
                block: 'b-infoblock-collapser',
                state: 'uncollapsed'
            });

            sandbox.spy(block, 'trigger');

            block.findBlockInside('link', 'link').trigger('click');

            expect(block.trigger.calledWith('stateChanged', { collapsed: 'yes' }));
        });
    });

    describe('Установка внешнего коллапсера', function() {
        it('После установки внешнего коллапсера клик по нему вызывает триггер события', function() {
            createBlock(
                {
                    block: 'b-infoblock-collapser',
                    state: 'uncollapsed'
                });

            var outsideCollapser = u.createBlock({
                block: 'link',
                js: true,
                mods: { pseudo: 'yes' },
                content: 'Кнопка'
            });

            block.setCollapserBlock(outsideCollapser);

            sandbox.spy(block, 'trigger');

            outsideCollapser.trigger('click');

            expect(block.trigger.calledWith('stateChanged', { collapsed: 'yes' }));
        });
    });
});
