describe('b-banners-search-form_type_agency', function() {
    var sandbox,
        block,
        whatRadiobox;

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
            mods: { type: 'agency' },
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
    });

    after(function() {
        sandbox.restore();
        block.destruct();
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

    describe('Видимые блоки', function() {
        var rowsList = [
            'active-only-row', 'group-row', 'phrase-fields',
            'strict-search', 'retargeting-row', 'strategy-choose', 'domain-row'
        ];

        [
            {
                what: 'num',
                visible: ['active-only-row', 'group-row']
            },
            {
                what: 'phrase',
                visible: ['active-only-row', 'group-row', 'retargeting-row', 'strategy-choose', 'phrase-fields']
            },
            {
                what: 'domain',
                visible: ['active-only-row', 'group-row', 'strict-search', 'domain-row', 'retargeting-row', 'strategy-choose']
            },
            {
                what: 'group',
                visible: ['active-only-row', 'group-row']
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

    it('Должен быть задизейблен чекбокс "искать только по активным баннерам", если выбран поиск по фразе', function() {
        whatRadiobox.val('phrase');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isDisabled()).to.be.true;
    });

    it('Должен быть чекнут чекбокс "искать только по активным баннерам", если выбран поиск по фразе', function() {

        whatRadiobox.val('phrase');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isChecked()).to.be.true;
    });

    it('Должен раздизейбливать чекбокс, если выбор с поиска по домену снят', function() {
        whatRadiobox.val('num');
        sandbox.clock.tick(500);

        expect(block.findBlockInside('active-only-row', 'checkbox').isDisabled()).to.be.false;
    });

});
