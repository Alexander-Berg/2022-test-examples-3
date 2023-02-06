describe('b-adjustment-rates__control_for_common-input', function() {
    var model,
        block,
        BLOCK_NAME = 'b-adjustment-rates';

    beforeEach(function() {
        model = BEM.MODEL.create('m-adjustment-base-rate', {});
        block = $(BEMHTML.apply({
                block: BLOCK_NAME,
                content: {
                    elem: 'control',
                    modelId: 'modelId',
                    elemMods: { for: 'common-input' }
                }
            })).bem(BLOCK_NAME);
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
    });

    it('В блоке должен быть элемент для вывода ошибок', function() {
        expect(block).to.haveElems('error-text');
    });

    describe('Проверяем, что есть провязка в i-glue', function() {
        it('На контролл должен быть замиксован i-glue', function() {
            expect(block.findBlockOn('control', 'i-glue')).to.not.be.undefined;
        });

        it('В параметры i-glue должны передаваться modelName: "m-adjustment-base-rate"', function() {
            expect(block.findBlockOn('control', 'i-glue').params).to.have.property('modelName')
              .that.to.equal('m-adjustment-base-rate');
        });

        it('В параметры i-glue должны передаваться modelId: modelId }', function() {
            expect(block.findBlockOn('control', 'i-glue').params).to.have.property('modelId')
              .that.to.equal('modelId');
        });
    });
});
