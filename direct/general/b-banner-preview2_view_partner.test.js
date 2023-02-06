describe('b-banner-preview2_view_partner', function() {
    var sandbox,
        model,
        block,
        render = function(data) {
            model = BEM.MODEL.create('b-banner-preview2_type_text', data);
            return block = u.getInitedBlock({
                block: 'b-banner-preview2',
                mods: { type: 'text', view: 'partner' },
                data: model.toJSON(),
                modelsParams: {
                    vmParams: { name: model.name, id: model.id }
                }
            });
        };

    window.dna = {
        reactDOMRender: function () {},
        components: {},
        reactCreateElement: function () {}
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });

        var constStub = sandbox.stub(u, 'consts');

        constStub.withArgs('clientFeaturesAll').returns([]);
        constStub.withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
        sandbox.stub(BEM.blocks['b-banner-preview2__partner-code'], 'render');
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
        sandbox.restore();
    });

    describe('Модификатор `with_image`', function() {
        describe('При первом рендеринге', function() {
            it('Должен быть равен `yes` при наличии изображения, ', function() {
                render({ image: 'image' });
                expect(block).to.haveMod('with-image', 'yes');
            });

            it('Должен быть равен `` при отсутствии изображения', function() {
                render({ image: '' });
                expect(block).to.not.haveMod('with-image');
            });
        });

        describe('После обновления модели', function() {
            beforeEach(function() {
                render();
            });

            it('Должен быть равен `yes` при наличии изображения, ', function() {
                model.update({ image: 'image' });
                sandbox.clock.tick(500);
                expect(block).to.haveMod('with-image', 'yes');
            });

            it('Должен быть равен `` при отсутствии изображения', function() {
                model.update({ image: '' });
                sandbox.clock.tick(500);
                expect(block).to.not.haveMod('with-image');
            });
        });
    });

    describe('Проверка зависимостей', function() {
        var updateStub;

        beforeEach(function() {
            render();
            updateStub = sandbox.stub(block, '_updatePartnerCodePreview');
        });

        [
            'cid',
            'bid',
            'rating',
            'displayHref',
            'isTemplateBanner',
            'isHrefHasParams',
            'loadVCardFromClient',
            'country',
            'city',
            'city_code',
            'country_code',
            'phone',
            'ext',
            'name',
            'contactperson',
            'worktime',
            'street',
            'house',
            'build',
            'apart',
            'auto_point',
            'auto_bounds',
            'auto_precision',
            'manual_point',
            'manual_bounds',
            'metro',
            'contact_email',
            'im_client',
            'im_login',
            'extra_message',
            'org_details_id',
            'ogrn',
            'disclaimer',
            'flagsSettings',
            'geo',
            'titleSubstituteOn',
            'isArchived'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - не должно быть изменений', function() {
                model.trigger('change', { changedFields: [field] });
                sandbox.clock.tick(500);
                expect(updateStub.called).to.equal(false);
            });
        });

        [
            'title',
            'body',
            'image',
            'image_type',
            'mdsGroupId',
            'sitelinks',
            'url',
            'domain',
            'vcard',
            'phrase',
            'callouts',
            'flags'
        ].forEach(function(field) {
            it('при изменении поля ' + field + ' - должен вызывать метод перерисовки', function() {
                model.trigger('change', { changedFields: [field] });
                sandbox.clock.tick(500);
                expect(updateStub.called).to.equal(true);
            });
        });
    });

});
