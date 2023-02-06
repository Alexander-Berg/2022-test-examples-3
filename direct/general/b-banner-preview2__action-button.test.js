describe('b-banner-preview2__action-button', function() {
    var sandbox,
        viewModel,
        block,
        constStub;

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

        sandbox.useFakeTimers();

        constStub = u.stubCurrencies();
        constStub.withArgs('rights').returns({});

        viewModel = BEM.MODEL.create('b-banner-preview2_type_mobile-content');

        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { view: 'tests-only' },
            data: viewModel.toJSON(),
            modelsParams: { vmParams: { name: viewModel.name,  id: viewModel.id } }
        });

    });

    afterEach(function() {
        block.destruct();
        u.restoreCurrencies();
        viewModel.destruct();
        sandbox.restore();
        constStub.restore();
    });

    it('Если флаг showPrice = false, на кнопке нет цены', function() {
        viewModel.update({ showPrice: false, actionType: 'download' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Загрузить');
    });

    it('Если флаг showPrice = true, но цена не пришла, на кнопке нет цены', function() {
        viewModel.update({ showPrice: true, price: 0, actionType: 'download' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Загрузить');
    });

    it('Если цена есть, но флаг showPrice = false , на кнопке нет цены', function() {
        viewModel.update({ showPrice: false, price: 10, actionType: 'buy' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Купить');
    });

    it('Если цена есть и флаг showPrice = true , на кнопке отображается цена', function() {
        viewModel.update({ showPrice: true, price: 10, actionType: 'download' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Загрузить – 10.00 у.е.');
    });

    it('Если цена поменялась, то и цена на кнопке поменяется', function() {
        viewModel.update({ showPrice: true, price: 20, actionType: 'play' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Играть – 20.00 у.е.');
    });

    it('Если цены нет, на кнопке нет цены в любом случае', function() {
        viewModel.update({ showPrice: true, price: 0, actionType: 'buy' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Бесплатно');
    });

    it('Если цена есть и showPrice=true, отображаем цену', function() {
        viewModel.update({ showPrice: true, price: 10, actionType: 'buy' });
        sandbox.clock.tick(500);

        expect(block.findBlockInside('action-button', 'button').elem('text').text()).to.equal('Купить – 10.00 у.е.');
    });
});
