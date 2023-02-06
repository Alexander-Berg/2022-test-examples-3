describe('b-banner-preview2_view_mobile-content-search', function() {
    var sandbox,
        model,
        block;

    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        sandbox.stub(u, 'consts').callsFake(function(name) {
            if (name == 'AD_WARNINGS') {
                return u.getAdWarningsForStub();
            } else {
                return {};
            }
        });

        sandbox.useFakeTimers();
        model = BEM.MODEL.create('b-banner-preview2_type_mobile-content');
        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { type: 'mobile-content', view: 'mobile-content-search' },
            data: model.toJSON(),
            modelsParams: {
                vmParams: { name: model.name,  id: model.id }
            }
        });
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
        sandbox.restore();
    });

    describe('Проверка зависимостей', function() {
        var generateHtmlSpy;

        beforeEach(function() {
            generateHtmlSpy = sinon.spy(block, '_generateHtml');
        });

        afterEach(function() {
            generateHtmlSpy.restore();
        });

        [
            'city',
            'metro',
            'titleSubstituteOn',
            'image',
            'sitelinks',
            'domain',
            'vcard',
            'isHrefHasParams',
            'geo',
            'loadVCardFromClient',
            'isArchived',
            'worktime',
            'phone'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - не должно быть изменений', function() {
                model.trigger('change', { changedFields: [field] });
                sandbox.clock.tick(500);
                expect(generateHtmlSpy.called).to.equal(false);
            });
        });

        [
            'title',
            'body',
            'url',
            'showIcon',
            'icon',
            'showRating',
            'showRatingVotes',
            'rating',
            'ratingVotes',
            'showPrice',
            'price',
            'flags',
            'flagsSettings',
            'phrase',
            'isTemplateBanner'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - должен вызывать метод перерисовки', function() {
                model.trigger('change', { changedFields: [field] });
                sandbox.clock.tick(500);
                expect(generateHtmlSpy.called).to.equal(true);
            });
        });
    });

});

