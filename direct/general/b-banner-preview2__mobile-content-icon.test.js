describe('b-banner-preview2__mobile-content-icon', function() {
    var sandbox,
        model,
        block,
        constStub;

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

        sandbox.useFakeTimers();
        constStub = sinon.stub(u, 'consts');
        constStub.withArgs('rights').returns({});

        model = BEM.MODEL.create('b-banner-preview2_type_mobile-content');

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
        sandbox.restore();
        constStub.restore();
    });

    [
        {
            showIcon: false,
            icon: 'https://avatars.mds.yandex.net/get-google-play-app-icon/39706/06ec32a477f5946291ee01036a5046bf/icon',
            result: false
        },
        {
            showIcon: false,
            icon: '',
            result: false
        },
        {
            showIcon: true,
            icon: '',
            result: true
        },
        {
            showIcon: true,
            icon: 'https://avatars.mds.yandex.net/get-google-play-app-icon/39706/06ec32a477f5946291ee01036a5046bf/icon',
            result: true
        }
    ].forEach(function(data) {
        var modelData = u._.pick(data, 'showIcon', 'icon');

        it('При данных в модели ' + JSON.stringify(modelData) + ' иконка приложения ' + (data.result ? '' : 'не ') + 'показывается', function() {
            model.update(modelData);
            sandbox.clock.tick(500);
            data.result ?
                expect(block).to.haveElem('mobile-content-icon') :
                expect(block).to.not.haveElem('mobile-content-icon');
        });
    });

    it('Если showIcon = true но нет icon, то показывается дефолтная картинка', function() {
        model.update({ icon: '', showIcon: true });
        sandbox.clock.tick(500);
        expect(block.elem('mobile-content-icon').find('img').attr('src')).to.equal(u.preview.getEmptyIcon());
    });

    it('При изменении icon в модели атрибут src у иконки принимает то же значение', function() {
        model.update({ icon: '/img/test.ru', showIcon: true });
        sandbox.clock.tick(500);
        expect(block.elem('mobile-content-icon').find('img').attr('src')).to.equal('/img/test.ru');
    });
});
