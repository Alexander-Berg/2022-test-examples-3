describe('b-banner-preview2__city', function() {
    var clock,
        model,
        block,
        constStub;

    beforeEach(function() {
        clock = sinon.useFakeTimers();

        constStub = sinon.stub(u, 'consts');
        constStub.withArgs('rights').returns({});

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

    it('Не показываем город при выключенной визитке', function() {
        model.update({
            vcard: false,
            city: 'Москва'
        });
        clock.tick(500);
        expect(block).to.not.haveElem('city');
    });

    it('Показываем город, если визитка включена', function() {
        model.update({
            vcard: true,
            city: 'Москва'
        });
        clock.tick(500);
        expect(block.elem('city').text()).to.equal('Москва');
    });

});
