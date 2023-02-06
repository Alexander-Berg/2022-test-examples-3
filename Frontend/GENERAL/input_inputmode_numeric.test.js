describe('input_inputmode_numeric', function() {
    var numericVal = '12\u2006345,67',
        bemjson, block;

    beforeEach(function() {
        bemjson = {
            block: 'input',
            mods: { size: 's', inputmode: 'numeric' },
            content: { elem: 'control', content: numericVal }
        };
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
    });

    describe('user interface events', function() {
        var input = stubBlockPrototype('input', [
            '_onCopy',
            '_onCut',
            '_onPaste',
            '_onKeyUp',
            '_onKeyDown',
            '_onInput'
        ]);

        beforeEach(function() {
            input.init();
            block = buildDomBlock('input', bemjson);
        });

        afterEach(function() {
            input.restore();
        });

        it('should bind to copy event', function() {
            block.control.trigger('copy');
            assert.calledOnce(input.get('_onCopy'));
        });

        it('should bind to cut event', function() {
            block.control.trigger('cut');
            assert.calledOnce(input.get('_onCut'));
        });

        it('should bind to paste event', function() {
            block.control.trigger('paste');
            assert.calledOnce(input.get('_onPaste'));
        });

        it('should bind to keyup event', function() {
            block.control.trigger('keyup');
            assert.calledOnce(input.get('_onKeyUp'));
        });

        it('should bind to keydown event', function() {
            block.control.trigger('keydown');
            assert.calledOnce(input.get('_onKeyDown'));
        });

        it('should bind to input event', function() {
            block.control.trigger('input');
            assert.calledOnce(input.get('_onInput'));
        });
    });

    describe('pure value', function() {
        var val = '12345,67',
            pureVal = '12345,67';

        beforeEach(function() {
            block = buildDomBlock('input', bemjson);
            block.val(val);
        });

        it('should have correct value', function() {
            assert.equal(block.val(), pureVal);
        });
    });

    describe('numeric value', function() {
        var val = '12345,67';

        beforeEach(function() {
            block = buildDomBlock('input', bemjson);
            block.val(val);
        });

        it('should have correct value', function() {
            assert.equal(block._val, numericVal);
        });
    });

    describe('setCaret', function() {
        var val = '12345,67',
            caret = 5;

        beforeEach(function() {
            block = buildDomBlock('input', bemjson);
            block.val(val);
            block.setCaret(caret);
        });

        it('should set correct caret position', function() {
            assert.equal(block.control[0].selectionStart, caret);
        });
    });

    describe('getCaret', function() {
        var val = '12345,67',
            caret = 5;

        beforeEach(function() {
            block = buildDomBlock('input', bemjson);
            block.val(val);
            block.control[0].setSelectionRange(caret, caret);
        });

        it('should get correct caret position', function() {
            assert.equal(block.getCaret(), caret);
        });
    });
});
