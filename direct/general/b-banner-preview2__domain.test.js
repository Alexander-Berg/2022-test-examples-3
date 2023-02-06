describe('b-banner-preview2__domain', function() {
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

    it('При отсутствующем домене (ссылке), но при наличии визитки - не показываем элемента domain', function() {
        model.update({
            domain: '',
            vcard: true
        });
        clock.tick(500);
        expect(block).to.not.haveElem('domain');
    });

    it('При отсутствующем домене (ссылке) и выключенной визитке, показывем домен с текстом по умолчанию', function() {
        model.update({
            domain: '',
            vcard: false
        });
        clock.tick(500);
        expect(block.elem('domain').text()).to.equal('домен');
    });

    it('При наличии домена - отображаем его в элементе domain', function() {
        model.update({
            domain: 'yandex.ru'
        });
        clock.tick(500);
        expect(block.elem('domain').text()).to.equal('yandex.ru');
    });

    it('При наличии домена и параметрах в ссылке (или шаблона) - отображаем значок шаблона', function() {
        model.update({
            domain: 'yandex.ru',
            isHrefHasParams: true
        });
        clock.tick(500);
        expect(block).to.haveElem('domain-warning');
    });

    it('При наличии домена и параметрах в ссылке (или шаблона) - ссылку на хелп', function() {
        model.update({
            domain: 'yandex.ru',
            isHrefHasParams: true
        });
        clock.tick(500);
        expect(block).to.haveBlock('domain', 'b-help-link');
    });
});
