describe('b-teaser', function() {
    var sandbox,
        block;

    function createBlock(teaser) {
        return u.createBlock({
            block: 'b-teaser',
            mods: {
                view: 'full',
                mode: 'regular',
                infoblock: 'yes',
                type: 'autopay'
            },
            teaser: teaser
        }, { inject: true });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Проверка на наличие/отсутствие элемента close', function(){
        afterEach(function() {
            block.destruct && block.destruct();
        });

        it('Тизер должен содержать блок close-button', function() {
            block = createBlock();

            expect(block).to.haveElem('close-button')
        });

        it('Если у тизера есть noClose: true и он проброшен, то кнопка Закрыть отстутсвует', function() {
            block = createBlock({ noClose: true });

            expect(block).not.to.haveElem('close-button')
        })
    });
});

