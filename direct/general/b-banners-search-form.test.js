describe('b-banners-search-form', function() {
    var sandbox,
        block,
        whatRadiobox,
        whereRadioButton;

    /**
     * Возвращает инициализированное DOM-дерево из bemjson
     * @param {Object} data bemjson
     * @returns {jQuery}
     */
    function getDOMTree(data) {
        data = data || {};

        return $(BEMHTML.apply({
            block: 'b-banners-search-form',
            js: true,
            content: {
                block: 'b-banners-search-form',
                elem: 'rows',
                data: {
                    where: data.where || 'direct',
                    what: data.what || 'num',
                    textSearch: data.text_search || '',
                    groupCamp: data.group_camp,
                    includeCurrencyArchivedCampaigns: data.include_currency_archived_campaigns,
                    strictSearch: data.strict_search || '',
                    searchOrNet: data.search_or_net || '',
                    retargetings: data.retargetings || '',
                    strategy: data.strategy || '',
                    isBsShown: true
                }
            }
        }));
    }

    before(function() {
        sandbox = sinon.sandbox.create();
        sandbox.useFakeTimers();

        block = getDOMTree({}).appendTo(document.body).bem('b-banners-search-form');
        whatRadiobox = block.findBlockOn('what', 'radiobox');
        whereRadioButton = block.findBlockOn('where', 'radio-button');
    });

    after(function() {
        sandbox.restore();
        block.destruct();
    });

    it('По дефолту должна быть активна вкладка "Директ"', function() {
        expect(whereRadioButton.val()).to.equal('direct');
    });

    describe('Директ: Изменение лейбла рядом с полем в ввода при переключении "Искать по"', function() {
        [
            {
                val: 'num',
                inputTitle: 'Номер объявления:'
            },
            {
                val: 'phrase',
                inputTitle: 'Фраза:'
            },
            {
                val: 'domain',
                inputTitle: 'Домен:'
            },
            {
                val: 'image_id',
                inputTitle: 'Номер изображения:'
            },
            {
                val: 'group',
                inputTitle: 'Номер группы:'
            }
        ].forEach(function(opt) {
            it('Если выбрано "Искать по ' + opt.val + '" лейбл при поле ввода должен содержать текст ' + opt.title, function() {
                whatRadiobox.val(opt.val);

                sandbox.clock.tick(500);

                expect(block.elem('search-label').text()).to.equal(opt.inputTitle);
            });
        });
    });

    describe('МКБ: Изменение лейбла рядом с полем в вода при переключении "Искать по"', function() {
        before(function() {
            whereRadioButton.val('mcb');
        });

        after(function() {
            whereRadioButton.val('direct');
        });

        [
            {
                val: 'num_media',
                inputTitle: 'Номер объявления:'
            },
            {
                val: 'domain_media',
                inputTitle: 'Домен:'
            }
        ].forEach(function(opt) {
            it('Если выбрано "Искать по ' + opt.val + '" лейбл при поле ввода должен содержать текст ' + opt.title, function() {
                whatRadiobox.val(opt.val);

                sandbox.clock.tick(500);

                expect(block.elem('search-label').text()).to.equal(opt.inputTitle);
            });
        });
    });

    describe('Видимые блоки на вкладке "Директ"', function() {
        var rowsList = [
            'active-only-row', 'group-row', 'currency-archived-row', 'strict-search-row',
            'retargeting-row', 'strategy-choose', 'domain-row'
        ];

        [
            {
                what: 'num',
                visible: ['active-only-row', 'group-row', 'currency-archived-row']
            },
            {
                what: 'phrase',
                visible: ['active-only-row', 'group-row', 'currency-archived-row', 'retargeting-row', 'strategy-choose']
            },
            {
                what: 'domain',
                visible: rowsList
            },
            {
                what: 'image_id',
                visible: ['active-only-row', 'group-row', 'currency-archived-row']
            },
            {
                what: 'group',
                visible: ['active-only-row', 'group-row', 'currency-archived-row']
            }
        ].forEach(function(opt) {
            rowsList.forEach(function(rowName) {
                var isVisible = (opt.visible.indexOf(rowName) !== -1);

                it('При выборе "Искать по ' + opt.what + '" должна быть ' + (isVisible ? '' : 'не') + 'видима строка ' + rowName, function() {
                    whatRadiobox.val(opt.what);

                    sandbox.clock.tick(500);

                    expect(block.elem(rowName).filter(':visible').length).to.equal(isVisible ? 1 : 0);
                });
            });
        });
    });


    describe('Видимые блоки на вкладке "МКБ"', function() {
        before(function() {
            whereRadioButton.val('mcb');
        });

        after(function() {
            whereRadioButton.val('direct');
        });

        var rowsList = [
            'active-only-row', 'group-row', 'currency-archived-row', 'strict-search-row'
        ];

        [
            {
                what: 'num_media',
                visible: ['active-only-row', 'group-row', 'currency-archived-row']
            },
            {
                what: 'domain_media',
                visible: rowsList
            }
        ].forEach(function(opt) {
            rowsList.forEach(function(rowName) {
                var isVisible = (opt.visible.indexOf(rowName) !== -1);

                it('При выборе "Искать по ' + opt.what + '" должна быть ' + (isVisible ? '' : 'не') + 'видима строка ' + rowName, function() {
                    whatRadiobox.val(opt.what);

                    sandbox.clock.tick(500);

                    expect(block.elem(rowName).filter(':visible').length).to.equal(isVisible ? 1 : 0);
                });
            });
        });
    });


    it('На вкладке Директ должен быть задизейблен чекбокс "искать только по активным баннерам", если выбран поиск по фразе', function() {
        whatRadiobox.val('phrase');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isDisabled()).to.be.true;
    });

    it('На вкладке Директ должен быть чекнут чекбокс "искать только по активным баннерам", если выбран поиск по фразе', function() {

        whatRadiobox.val('phrase');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isChecked()).to.be.true;
    });

    it('а вкладке Директ должен раздизейбливать чекбокс, если выбор с поиска по домену снят', function() {
        whatRadiobox.val('num');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isDisabled()).to.be.false;
    });

});
