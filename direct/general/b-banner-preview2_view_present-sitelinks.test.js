describe('b-banner-preview2_view_present-sitelinks', function() {
    var clock,
        model,
        block,
        constStub;

    function createBlock() {
        constStub = sinon.stub(u, 'consts');
        constStub.withArgs('rights').returns({});
        constStub.withArgs('SITELINKS_NUMBER').returns(4);
        constStub.withArgs('AD_WARNINGS').returns(u.getAdWarningsForStub());
        model = BEM.MODEL.create('b-banner-preview2_type_text');
        block = u.getInitedBlock({
            block: 'b-banner-preview2',
            mods: { type: 'text', view: 'present-sitelinks' },
            data: model.toJSON(),
            modelsParams: {
                vmParams: { name: model.name,  id: model.id }
            }
        });
    }

    function destroyBlock() {
        block.destruct();
        constStub.restore()
    }

    function testLinks(links) {

        u._.range(4).forEach(function(index) {
            var title = u._.get(links[index], 'title', 'Текст ссылки №' + (index + 1)),
                url = u._.get(links[index], 'url', '#');

            it('ссылка №' + (index + 1) + ' содержит текст: ' + title, function() {
                expect(block.elem('sitelink').eq(index).text()).to.be.eq(title)
            });

            it('ссылка №' + (index + 1) + ' содержит адрес: ' + url, function() {
                expect(block.elem('sitelink').eq(index).attr('href')).to.be.eq(url)
            });
        });

    }

    beforeEach(function() {
        clock = sinon.useFakeTimers();
    });

    afterEach(function() {
        clock.restore();
    });

    describe('Переопределение элементов', function() {
        describe('b-banner-preview2__sitelinks', function() {

            describe('Заполненые ссылки должны отображаться', function() {
                var links = [
                    { title: 'Пользовательский текст 1', url: 'http://ya.ru' },
                    { title: 'Пользовательский текст 2', url: 'http://ya2.ru' },
                    { title: 'Пользовательский текст 3', url: 'http://ya3.ru' },
                    { title: 'Пользовательский текст 4', url: 'http://ya4.ru' }
                ];

                before(function() {
                    createBlock();
                    model.set('sitelinks', links);
                });

                after(function() {
                    destroyBlock();
                });

                testLinks(links);
            })
            describe('Для пустых быстрых ссылок должна быть заглушка', function() {
                describe('Все пустые', function() {

                    before(function() {
                        createBlock();
                        model.set('sitelinks', []);
                    });

                    after(function() {
                        destroyBlock();
                    });

                    testLinks([]);
                });
                describe('Часть заполнены (2)', function() {

                    var links = [
                        { title: 'Пользовательский текст 1', url: 'http://ya.ru' },
                        { title: 'Пользовательский текст 2', url: 'http://ya2.ru' },
                    ];

                    before(function() {
                        createBlock();
                        model.set('sitelinks', links);
                    });

                    after(function() {
                        destroyBlock();
                    });

                    testLinks(links)
                });
            });
        });
    });

    describe('Проверка зависимостей', function() {
        var generateHtmlSpy;

        beforeEach(function() {
            createBlock();
            generateHtmlSpy = sinon.spy(block, '_generateHtml');
        });

        afterEach(function() {
            destroyBlock();
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
            'image',
            'ext',
            'flags',
            'flagsSettings',
            'isTemplateBanner'
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
            'sitelinks',
            'url',
            'vcard',
            'phrase',
            'domain',
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
