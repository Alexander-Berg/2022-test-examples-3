describe('b-banner-preview2__favicon', function() {
    var clock,
        model,
        block,
        constsStub;

    beforeEach(function() {
        clock = sinon.useFakeTimers();

        constsStub = sinon.stub(u, 'consts');
        constsStub.withArgs('rights').returns({});

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
        constsStub.restore();
    });

    it('Если нет урла - не выводим элемент favicon', function() {
        model.update({
            url: ''
        });
        clock.tick(500);
        expect(block).to.not.haveElem('favicon');
    });

    it('Если урл есть - получаем фавиконку по урлу', function() {
        model.update({ url: 'https://yandex.ru/search/?text=sinon&clid=1823140&win=39' });
        clock.tick(500);
        expect(block.elem('favicon').css('background-image'))
            .to.contain('https://favicon.yandex.net/favicon/yandex.ru/' );
    });

});
