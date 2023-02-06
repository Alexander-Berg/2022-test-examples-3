describe('b-video-creative-selector', function() {
    var sandbox,
        block;

    function createBlock(opitons) {
        opitons || (opitons = {});

        block = u.getInitedBlock({
            block: 'b-video-creative-selector',
            mods: opitons.mods || {},
            video: {
                resource_type: 'creative'
            }
        });
    }

    function containElemsCount(items) {
        it('Содержит ' + items.length + ' шт. вариантов ', function() {
            expect(block.elem('item').length).to.be.eq(items.length);
        });
    }

    function containElemsTest(items) {
        items.forEach(function(item) {
            it('Содержит пункт выбора ' + item, function() {
                expect(block).to.haveElem('item', 'name', item);
            });
        });
    }

    function triggersTest(items) {
        items.forEach(function(item) {
            it('При нажатии на пункт выбора ' + item + 'генерируется событие select', function() {
                expect(block).to.triggerEvent('select', function() {
                    block.elem('item', 'name', item).click();
                });
            });
        });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Без модификатора', function(){

        beforeEach(function() {
            createBlock();
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        var items = ['new', 'recent'];

        containElemsTest(items);
        triggersTest(items);
        containElemsCount(items);
    });

    describe('С модификатором action_edit', function(){

        beforeEach(function() {
            createBlock({ mods: { action: 'edit' }});
        });

        afterEach(function() {
            block.destruct && block.destruct();
        });

        var items = ['new', 'recent', 'edit'];

        containElemsTest(items);
        triggersTest(items);
        containElemsCount(items);
    });
});

