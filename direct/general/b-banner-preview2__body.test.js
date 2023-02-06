describe('b-banner-preview2__body', function() {
    var clock,
        model,
        block,
        replaceTemplSpy,
        constsStub;

    beforeEach(function() {
        replaceTemplSpy = sinon.spy(u, 'replaceTemplate');
        clock = sinon.useFakeTimers();
        constsStub = sinon.stub(u, 'consts');
        constsStub.withArgs('MAX_BODY_LENGTH').returns(81);
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
        replaceTemplSpy.restore();
        constsStub.restore();
    });

    it('Описанию объявления напрямую соответствует элемент body', function() {
        model.set('body', 'body');
        clock.tick(500);
        expect(block.elem('body').text()).to.equal('body');
    });

    it('Внутри должна вызываться функция подстановки шаблона', function() {
        model.set('body', 'body');
        clock.tick(500);

        expect(replaceTemplSpy.called).to.equal(true);
    });

    it('Если в описании есть символ шаблона и существует фраза - то она туда должна подставиться', function() {
        model.update({
            body: 'body ##',
            phrase: 'phrase'
        });
        clock.tick(500);
        expect(block.elem('body').text()).to.equal('body phrase');
    });

    it('Есть ограничение на максимальное количество символов', function() {
        // задаем строку в 90 символов
        model.set('body', Array(90 + 1).join('b'));
        clock.tick(500);

        // при привыжении мы должны получить строку длинной равной MAX_BODY_LENGTH
        expect(block.elem('body').text()).to.equal(Array(u.consts('MAX_BODY_LENGTH') + 1).join('b'));
    });
});
