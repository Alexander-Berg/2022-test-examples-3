describe('b-banner-preview2__title', function() {
    var clock,
        model,
        block,
        replaceTemplSpy,
        constsStub;

    beforeEach(function() {
        replaceTemplSpy = sinon.spy(u, 'replaceTemplate');
        clock = sinon.useFakeTimers();
        constsStub = sinon.stub(u, 'consts');
        constsStub.withArgs('rights').returns({});
        constsStub.withArgs('NEW_MAX_TITLE_LENGTH').returns(35);

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

    it('Внутри заголовка должны быть ссылка', function() {
        expect(block).to.haveBlock('title', 'link');
    });

    it('Ссылка в заголовке должна вести на адрес указанный в поле url', function() {
        model.set('url', 'http://yandex.ru');
        clock.tick(500);
        expect(block.findBlockInside('title', 'link').domElem.attr('href')).to.equal('http://yandex.ru');
    });

    it ('Если url не указан, но есть поле vcard - к ссылке из заголовка примиксовывается элемент title-to-vcard (который открывает попап визитки)', function() {
        model.update({
            vcard: true,
            domain: '',
            url: ''
        });
        clock.tick(500);
        expect(block).to.haveElems('title-to-vcard', 1);
    });

    it('Заголовку объявления напрямую соответствует элемент title', function() {
        model.set('title', 'title');
        clock.tick(500);
        expect(block.elem('title').text()).to.equal('title');
    });

    it('Внутри должна вызываться функция подстановки шаблона', function() {
        model.set('title', 'title');
        clock.tick(500);

        expect(replaceTemplSpy.called).to.equal(true);
    });

    it('Если в заголовке есть символ шаблона и существует фраза - то она туда должна подставиться', function() {
        model.update({
            phrase: 'phrase',
            title: 'title ##'
        });
        clock.tick(500);
        expect(block.elem('title').text()).to.equal('title phrase');
    });

    it('В заголовке учитывается максимальное ограничение на максимальное количество символов', function() {
        // задаем строку в 90 символов
        model.set('title', Array(90 + 1).join('t'));
        clock.tick(500);

        // при привыжении мы должны получить строку длинной равной MAX_TITLE_LENGTH
        expect(block.elem('title').text()).to.equal(Array(u.consts('NEW_MAX_TITLE_LENGTH') + 1).join('t'));
    });
});
