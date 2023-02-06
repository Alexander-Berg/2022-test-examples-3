describe('b-href-params', function() {
    var sandbox,
        block,
        createBlock = function(value, isActive, help) {
            block = u.createBlock({
                block: 'b-href-params',
                mods: {
                    active: isActive ? 'yes' : ''
                },
                help: help,
                value: value
            });
        };

    beforeEach(function() {
        block = undefined;
        sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });
    });

    afterEach(function() {
        block && block.destruct();
        sandbox.restore();
    });

    it ('в неактивном состоянии значение - пустая строка', function() {
        createBlock('test', false);

        var value = block.getValue();
        expect(value).to.be.equal('');
    });

    it ('в активном состоянии значение - которое было передано', function() {
        createBlock('test2', true);

        var value = block.getValue();
        expect(value).to.be.equal('test2');
    });

    it ('значение не теряется в неактивном состоянии', function() {
        createBlock('test3', false);

        block.setMod('active', 'yes');
        sandbox.clock.tick(10);

        var value = block.getValue();
        expect(value).to.be.equal('test3');
    });

    it ('генерируется событие change при изменении в активном сосотянии', function() {
        createBlock('test4', true);

        expect(block).to.triggerEvent('change', 'test4-2', function() {
            block.setInputValue('test4-2');
            sandbox.clock.tick(10);
        });
    });

    it ('не генерируется событие change при изменении в неактивном сосотянии', function() {
        createBlock('test5', false);

        expect(block).to.not.triggerEvent('change', function() {
            block.setInputValue('test5-2');
            sandbox.clock.tick(10);
        });
    });

    it ('генерируется событие change при изменении сосотяниия: неактивное => активное', function() {
        createBlock('test6', false);

        expect(block).to.triggerEvent('change', 'test6', function() {
            block.setMod('active', 'yes');
            sandbox.clock.tick(10);
        });
    });

    it ('генерируется событие change при изменении сосотяниия: активное => неактивное', function() {
        createBlock('test7', true);

        expect(block).to.triggerEvent('change', '', function() {
            block.setMod('active', '');
            sandbox.clock.tick(10);
        });
    });
});
