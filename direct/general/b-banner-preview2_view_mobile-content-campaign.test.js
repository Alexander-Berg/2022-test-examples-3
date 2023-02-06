describe('b-banner-preview2_view_mobile-content-campaign', function() {
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
            mods: { type: 'mobile-content', view: 'mobile-content-campaign' },
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

    describe('Переопределение элементов', function() {
        ['flag-age', 'flags'].forEach(function(elemName) {
            describe('b-banner-preview2__' + elemName, function() {
                describe('Настройки flagsSettings переопределяются корректно', function() {
                    [
                        {
                            addRemove: true,
                            edit: true
                        },
                        {
                            addRemove: true,
                            edit: false
                        },
                        {
                            addRemove: false,
                            edit: true
                        },
                        {
                            addRemove: false,
                            edit: false
                        }
                    ].forEach(function(settings) {
                        it(JSON.stringify(settings), function() {
                            model.update({
                                flagsSettings: {
                                    age: settings,
                                    other: settings
                                }
                            });
                            sandbox.clock.tick(500);

                            var adWarningsBlock = block.findBlockInside(elemName, {
                                    'flag-age': 'b-banner-age-label2',
                                    flags: 'b-banner-adv-alert2'
                                }[elemName]);

                            expect(adWarningsBlock.params.can).to.eql(settings);
                        });
                    });
                });
            });
        });
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

            'loadVCardFromClient',
            'isArchived',
            'worktime',
            'phone',
            'showIcon',
            'icon',
            'showRating',
            'showRatingVotes',
            'rating',
            'ratingVotes',
            'showPrice',
            'price'
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

