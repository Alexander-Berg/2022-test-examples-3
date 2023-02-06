describe('b-creative-wrapper', function() {
    var ctx = {
            block: 'b-creative-wrapper',
            mods: { view: 'groups' },
            options: {
                hasStatusTabs: true,
                hasFilters: true
            }
        },
        block,
        stubData = u['i-test-stubs__creatives-wrapper-fetch'](),
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        sandbox.stub(BEM.blocks['i-creative-request'], 'fetch').callsFake(function() {
            var def = $.Deferred();

            setTimeout(function() {
                def.resolve(stubData);
            }, 100);

            return def.promise();
        });
    });

    afterEach(function() {
        sandbox.restore();
    });

    function createBlock() {
        block = u.createBlock(ctx, { inject: true });
    }

    describe('Инициализация', function() {

        beforeEach(function() {
            createBlock();
            sandbox.clock.tick(200)
        });

        afterEach(function() {
            block && block.destruct();
        });

        it('Создана view-модель блока', function() {
            expect(block._viewModel).to.be.instanceof(BEM.MODEL);
        });

        it('Построен список табов', function() {
            expect(block.findBlockInside('b-creative-list-status-tabs').domElem.length).to.be.equal(1);
        });

        it('Построен список фильтров', function() {
            expect(block.findBlockInside('b-creative-list-filters').domElem.length).to.be.equal(1);
        });

        it('Построен список групп', function() {
            expect(block.findBlockInside('b-creative-groups-list').elem('item').length).to.be.equal(stubData.groups.length);
        });

    });

    describe('API', function() {

        beforeEach(function() {
            createBlock();
            sandbox.clock.tick(200);
        });

        afterEach(function() {
            block && block.destruct();
        });

        it('isSelectedChanged - по умолчанию false', function() {
            expect(block.isSelectedChanged()).to.be.equal(false);
        });

        it('getSelectedIds - по умолчанию пустой массив', function() {
            expect(block.getSelectedIds().length).to.be.equal(0);
        });

        it('getSelectedCountMessage - по умолчанию пустая строка', function() {
            expect(block.getSelectedCountMessage()).to.be.equal('');
        });

    });

});
