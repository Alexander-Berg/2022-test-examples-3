describe('b-banner-preview2_view_base', function() {
    var clock,
        model,
        block,
        constStub;

    beforeEach(function() {
        constStub = sinon.stub(u, 'consts');

        constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
        constStub.withArgs('rights').returns({});

        clock = sinon.useFakeTimers();
        model = BEM.MODEL.create('b-banner-preview2_type_text');
        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { type: 'text', view: 'base' },
            data: model.toJSON(),
            modelsParams: {
                vmParams: { name: model.name,  id: model.id }
            }
        });
    });

    afterEach(function() {
        model.destruct();
        block.destruct();
        clock.restore();
        constStub.restore()
    });

    describe('Переопределение элементов', function() {
        ['flag-age', 'flags'].forEach(function(elemName) {
            describe('b-banner-preview2__' + elemName, function() {
                describe('в поле flagsSettings установлены разные параметры addRemove и edit и в блоки предупреждений они пробрасываются корректно', function() {
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
                            clock.tick(500);

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
            'rating',
            'geo',
            'loadVCardFromClient',
            'isArchived',
            'worktime',
            'phone',
            'street',
            'house',
            'country',
            'auto_bounds',
            'country_code',
            'city_code',
            'ext'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - не должно быть изменений', function() {
                model.trigger('change', { changedFields: [field] });
                clock.tick(500);
                expect(generateHtmlSpy.called).to.equal(false);
            });
        });

        [
            'title',
            'body',
            'image',
            'sitelinks',
            'url',
            'domain',
            'vcard',
            'flags',
            'flagsSettings',
            'phrase',
            'isTemplateBanner',
            'isHrefHasParams'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - должен вызывать метод перерисовки', function() {
                model.trigger('change', { changedFields: [field] });
                clock.tick(500);
                expect(generateHtmlSpy.called).to.equal(true);
            });
        });
    });

});
