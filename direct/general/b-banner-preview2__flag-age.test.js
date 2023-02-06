describe('b-banner-preview2__flag-age', function() {
    var clock,
        model,
        block,
        constStub;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        constStub = sinon.stub(u, 'consts');

        constStub
            .withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub())
            .withArgs('rights').returns({ addRemoveFlags: true });

        model = BEM.MODEL.create('b-banner-preview2_type_text');

        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'tests-only' },
            data: model.toJSON(),
            modelsParams: { vmParams: { name: model.name,  id: model.id } }
        });

    });

    afterEach(function() {
        block.destruct();
        model.destruct();
        clock.restore();
        constStub.restore();
    });

    it('Элемент не отрисовывается при отсутсвии поля flagsSettings', function() {
        model.set('flags', { age: 12 });
        clock.tick(500);
        expect(block).to.not.haveElem('flag-age');
    });

    describe('Если в поле flagsSettings установлен параметр', function() {
        it('{ dontShow: true } - не должно быть элемента', function() {
            model.update({
                flagsSettings: {
                    age: { dontShow: true },
                    other: { dontShow: true }
                }
            });
            clock.tick(500);

            expect(block).to.not.haveElem('flags');
        });

        it('{ dontShow: false } - должен быть элемент', function() {
            model.update({
                flagsSettings: {
                    age: { dontShow: false },
                    other: { dontShow: false }
                }
            });
            clock.tick(500);

            expect(block).to.haveElem('flags');
        });
    });

    it('Значение пробрасывается корректно', function() {
        model.update({
            // без flagSettings не будут отрисованы предупреждения
            flagsSettings: {
                age: { dontShow: false },
                other: { dontShow: false }
            },
            flags: { age: 12, alcohol: true }
        });
        clock.tick(500);

        expect(block.findBlockInside('flag-age', 'b-banner-age-label2').params.value).to.eql(12);
    });

});
