describe('b-banner-preview2__flags', function() {
    var clock,
        model,
        block,
        constStub;

    function initBlock(data) {
        clock = sinon.useFakeTimers();
        constStub = sinon.stub(u, 'consts');

        constStub
            .withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub())
            .withArgs('rights').returns({ addRemoveFlags: true });

        model = BEM.MODEL.create('b-banner-preview2_type_text', data);

        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'tests-only' },
            data: model.toJSON(),
            modelsParams: {
                vmParams: {
                    name: model.name,
                    id: model.id
                }
            }
        });
    }

    afterEach(function() {
        block && block.destruct();
        model && model.destruct();
        clock && clock.restore();
        constStub && constStub.restore();
    });

    it('Элемент не отрисовывается при отсутсвии поля flagsSettings', function() {
        initBlock();

        model.set('flags', { alcohol: true });
        clock.tick(500);
        expect(block).to.not.haveElem('flags');
    });

    it('Элемент не отрисовывается при наличии значения в поле disclaimer', function() {
        initBlock({
            flagsSettings: {
                age: { dontShow: false },
                other: { dontShow: false }
            },
            flags: { alcohol: true },
            disclaimer: 'lerjhe'
        });

        expect(block).to.not.haveElem('flags');
    });

    describe('Если в поле flagsSettings установлен параметр', function() {
        it('{ dontShow: true } - не должно быть элемента', function() {
            initBlock();

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
            initBlock();

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
        initBlock();

        model.update({
            // без flagSettings не будут отрисованы предупреждения
            flagsSettings: {
                age: { dontShow: false },
                other: { dontShow: false }
            },
            flags: { baby_food: 10, alcohol: true }
        });
        clock.tick(500);

        expect(block.findBlockInside('flags', 'b-banner-adv-alert2').params.value).to.eql({ baby_food: 10, alcohol: true });
    });
});
