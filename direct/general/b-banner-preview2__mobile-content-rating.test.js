describe('b-banner-preview2__mobile-content-rating', function() {
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

    it('Если showRating = false то рейтинг не показывается', function() {
        model.update({
            showRating: false,
            rating: 5
        });
        sandbox.clock.tick(500);
        expect(block).to.not.haveElem('mobile-content-rating');
    });

    it('Если showRating = true то рейтинг показывается', function() {
        model.update({
            showRating: true,
            rating: 5
        });
        sandbox.clock.tick(500);
        expect(block).to.haveElem('mobile-content-rating');
    });

    [
        {
            showRatingVotes: false,
            ratingVotes: false,
            showRating: true,
            result: false
        },
        {
            showRatingVotes: true,
            showRating: true,
            ratingVotes: false,
            result: false
        },
        {
            showRatingVotes: true,
            showRating: true,
            ratingVotes: '1000',
            result: true
        }
    ].forEach(function(data) {
        var modelData = u._.pick(data, ['showRatingVotes', 'ratingVotes', 'showRating']);

        it('Если в модели ' + JSON.stringify(modelData) + ' то кол-во оценков в рейтинге ' + (data.result ? '' : 'не ') + 'показывается', function() {
            model.update(modelData);
            sandbox.clock.tick(500);
            data.result ?
                expect(block).haveElem('rating-count') :
                expect(block).not.haveElem('rating-count');
        });
    });

    it('Ссылка с рейтинга должна соответствовать полю url в модели', function() {
        model.update({
            showRating: true,
            rating: 5,
            url: 'test.ru'
        });
        sandbox.clock.tick(500);
        expect(block.findBlockInside('mobile-content-rating', 'link').domElem.attr('href')).to.equal('test.ru');
    });

    it('Ссылка с кол-ва оценок должна соответствовать полю url в модели', function() {
        model.update({
            showRatingVotes: true,
            showRating: true,
            rating: 5,
            ratingVotes: '1000',
            url: 'test.ru'
        });
        sandbox.clock.tick(500);
        expect(block.findBlockOn('rating-count', 'link').domElem.attr('href')).to.equal('test.ru');
    });

    it('Кол-во оценок в рейтинге должно соответствовать полю ratingVotes модели', function() {
        model.update({
            showRatingVotes: true,
            showRating: true,
            rating: 5,
            ratingVotes: '1000'
        });
        sandbox.clock.tick(500);

        expect(block.elem('rating-count').text()).to.equal('1000');
    });
});
