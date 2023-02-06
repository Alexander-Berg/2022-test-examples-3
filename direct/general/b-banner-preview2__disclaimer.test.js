describe('b-banner-preview2__disclaimer', function() {
    var clock,
        model,
        block,
        constStub,
        initBlock = function(data) {
            clock = sinon.useFakeTimers();
            constStub = sinon.stub(u, 'consts');

            constStub
                .withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
            constStub.withArgs('rights').returns({});

            model = BEM.MODEL.create('b-banner-preview2_type_text', data);

            block = u.getInitedBlock({
                block: 'b-banner-preview2',
                mods: { view: 'tests-only' },
                data: model.toJSON(),
                modelsParams: { vmParams: { name: model.name,  id: model.id } }
            });

        };

    afterEach(function() {
        block && block.destruct();
        model && model.destruct();
        clock && clock.restore();
        constStub && constStub.restore();
    });

    it('Элемент не отрисовывается при отсутсвии поля flagsSettings', function() {
        initBlock({ disclaimer: 'lsdkhglsdkh' });

        expect(block).to.not.haveElem('disclaimer');
    });

    describe('Если в поле flagsSettings установлен параметр', function() {
        it('{ dontShow: true } - не должно быть элемента', function() {
            initBlock({
                flagsSettings: {
                    age: { dontShow: true },
                    other: { dontShow: true }
                },
                disclaimer: 'laskfhalksfhlka'
            });

            expect(block).to.not.haveElem('disclaimer');
        });

        it('{ dontShow: false } - должен быть элемент', function() {
            initBlock({
                flagsSettings: {
                    age: { dontShow: false },
                    other: { dontShow: false }
                },
                disclaimer: 'laskfhalksfhlka'
            });

            expect(block).to.haveElem('disclaimer');
        });
    });

    it('Элемент не отрисовывается, если нет значения', function() {
        initBlock({
            flagsSettings: {
                age: { dontShow: false },
                other: { dontShow: false }
            }
        });

        expect(block).to.not.haveElem('disclaimer');
    });


    it('Значение пробрасывается корректно', function() {
        initBlock({
            flagsSettings: {
                age: { dontShow: false },
                other: { dontShow: false }
            },
            disclaimer: 'laskfhalksfhlka'
        });

        expect(block.elem('disclaimer-item').text()).to.eql('laskfhalksfhlka');
    });
});
