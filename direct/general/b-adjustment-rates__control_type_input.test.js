
describe('b-adjustment-rates__control_for_common-input', function() {
    var model,
        block,
        BLOCK_NAME = 'b-adjustment-rates';

    function createBlock(ctxData) {
        block = $(BEMHTML.apply({
            block: BLOCK_NAME,
            content: u._.extend({
                elem: 'control',
                modelId: 'modelId',
                elemMods: { type: 'input' }
            }, ctxData || {})
        })).bem(BLOCK_NAME);
    }

    beforeEach(function() {
        model = BEM.MODEL.create('m-adjustment-base-rate', {});
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
    });

    it('В блоке должен содержаться элемент input', function() {
        createBlock({ value: 10, max: 100 });
        expect(block.findBlockInside('input')).to.not.be.undefined;
    });

    it('Значение в инпуте должно совпадать с ctx.value', function() {
        createBlock({ value: 10, max: 100 });
        expect(block.findBlockInside('input').val()).to.equal('10');
    });

    it('Значение в хинте инпута должно быть max this.ctx.max', function() {
        createBlock({ value: 10, max: 100 });
        expect(block.findBlockInside('input').elem('hint').html()).to.equal('max 100');
    });

    it('Значение в подсказке инпута должно быть пустое если max в контексте не передано }', function() {
        createBlock({ value: 10 });
        expect(block.findBlockInside('input').elem('control').attr('placeholder')).to.equal('');
    });
});
