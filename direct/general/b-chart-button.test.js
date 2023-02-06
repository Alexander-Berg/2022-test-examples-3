describe('b-chart-button', function() {
    var block;

    beforeEach(function() {
    });

    afterEach(function() {
        block.destruct();
    });

    function createBlock() {
        block = u.createBlock({
            block: 'b-chart-button',
            label: 'name'
        });
    }

    it('При постановке модификатора loading происходит дизейбл кнопки и появляется спиннер', function() {
        createBlock();
        block.setMod('loading', 'yes');
        expect(block.findBlockInside('button')).to.haveMod('disabled', 'yes');
        expect(block.findBlockInside('spin')).to.haveMod('progress', 'yes')
    });

    it('При снятии модификатора loading происходит энейбл кнопки и прячетя спиннер', function() {
        createBlock();
        block.setMod('loading', 'no');
        expect(block.findBlockInside('button')).not.to.haveMod('disabled');
        expect(block.findBlockInside('spin')).not.to.haveMod('progress')
    });
});
