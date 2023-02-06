describe('b-banners-search', function() {
    var sandbox,
        block;

    function createBlock(options) {
        block = u.getInitedBlock({
            block: 'b-banners-search',
            mediaType: options.mediaType || 'base',
            cid: options.cid || 17114101,
            ulogin: options.ulogin || 'yndx-kosmana',
            isCampaignArchived: options.isCampaignArchived || false
        }, true);
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        });
        sandbox.stub(u, 'consts').withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Содержимое блока в зависимости от типа кампании', function(){

        function checkRadioSet(options, value) {
            createBlock(options);

            expect(block.findBlockOn('search-by', 'radio-button')).to.haveElem('radio', 'value', value);
        }

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('ТГО, ГО, РМП', function() {
            var options;

            beforeEach(function() {
                options = {
                    mediaType: 'base',
                    cid: 1,
                    ulogin: 'test'
                }
            });

            it('Содержит 2 таба', function() {
                createBlock(options);

                expect(block.findBlockInside('tabs-menu').elem('tab').length).to.be.eq(2);
            });

            it('Есть 4 типа поиска по объявлению', function() {
                createBlock(options);
                var radioButton = block.findBlockOn('search-by', 'radio-button');

                expect(radioButton.elem('radio').length)
                    .to.be.eq(4);
            });

            ['num', 'context', 'phrase', 'group'].forEach(function(val) {
                it('Есть выбор поиска по ' + val, function() {
                    checkRadioSet(options, val);
                });
            });

            it('Есть выбор поиска группы по статусу мало показов', function() {
                createBlock(options);

                expect(block).to.haveElem('group-filter-rarely');
            });

            it('Есть выбор поиска группы по баннерам с законодательными ограничениями', function() {
                createBlock(options);

                expect(block).to.haveElem('group-filter-disabled-geo');
            });
        });

        describe('ДО', function() {
            var options;

            beforeEach(function() {
                options = {
                    mediaType: 'dynamic',
                    cid: 1,
                    ulogin: 'test'
                }
            });

            it('Содержит 1 таб', function() {
                createBlock(options);

                expect(block.findBlockInside('tabs-menu').elem('tab').length).to.be.eq(1);
            });

            it('Есть 4 типа поиска по объявлению', function() {
                createBlock(options);
                var radioButton = block.findBlockOn('search-by', 'radio-button');

                expect(radioButton.domElem.find('[name=search_by]').length)
                    .to.be.eq(4);
            });

            ['num', 'context', 'group', 'phrase'].forEach(function(val) {
                it('Есть выбор поиска по ' + val, function() {
                    checkRadioSet(options, val);
                });
            });

            it('Отсутствует выбор поиска группы по статусу мало показов', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-rarely');
            });

            it('Отсутствует выбор поиска группы по баннерам с законодательными ограничениями', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-disabled-geo');
            });

        });

        describe('Смарт', function() {
            var options;

            beforeEach(function() {
                options = {
                    mediaType: 'performance',
                    cid: 1,
                    ulogin: 'test'
                }
            });

            it('Содержит 1 таб', function() {
                createBlock(options);

                expect(block.findBlockInside('tabs-menu').elem('tab').length).to.be.eq(1);
            });

            it('Есть 3 типа поиска по объявлению', function() {
                createBlock(options);
                var radioButton = block.findBlockOn('search-by', 'radio-button');

                expect(radioButton.domElem.find('[name=search_by]').length)
                    .to.be.eq(3);
            });

            ['num', 'filter', 'group'].forEach(function(val) {
                it('Есть выбор поиска по ' + val, function() {
                    checkRadioSet(options, val);
                });
            });

            it('Отсутствует выбор поиска группы по статусу мало показов', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-rarely');
            });

            it('Отсутствует выбор поиска группы по баннерам с законодательными ограничениями', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-disabled-geo');
            });
        });

        describe('Архивная компания', function() {
            var options;

            beforeEach(function() {
                options = {
                    mediaType: 'base',
                    cid: 1,
                    ulogin: 'test',
                    isCampaignArchived: true
                }
            });

            it('Содержит только 1 таб', function() {
                createBlock(options);

                expect(block.findBlockInside('tabs-menu').elem('tab').length).to.be.eq(1);
            });

            it('Отсутствует выбор поиска группы по статусу мало показов', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-rarely');
            });

            it('Отсутствует выбор поиска группы по баннерам с законодательными ограничениями', function() {
                createBlock(options);

                expect(block).to.not.haveElem('group-filter-disabled-geo');
            });
        });
    });

    describe('Поведение блока', function(){

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Общее поведение', function() {
            var options;

            beforeEach(function() {
                options = {
                    mediaType: 'base',
                    cid: 1,
                    ulogin: 'test'
                };
                createBlock(options);
            });

            it('При поиске по объявлениям с пустым критерием, должна показаться ошибка', function() {
                var form = block.findBlockInside(block._tabsPanes.findPane('active', 'yes'), 'b-banners-search__form');

                block.findBlockOn('submit', 'button').trigger('click');

                expect(form._getTooltip()).to.haveMod('shown', 'yes');
            });

            it('При фокусе на критерии. должна пропадать ошибка', function() {
                var form = block.findBlockInside(block._tabsPanes.findPane('active', 'yes'), 'b-banners-search__form');

                block.findBlockOn('submit', 'button').trigger('click');
                block.findBlockOn('input-value', 'input').setMod('focused', 'yes');

                expect(form._getTooltip()).to.haveMod('shown', 'yes');
            });

            it('При смене типа поиска, должна пропадать ошибка', function() {
                var form = block.findBlockInside(block._tabsPanes.findPane('active', 'yes'), 'b-banners-search__form');

                block.findBlockOn('submit', 'button').trigger('click');
                block.findBlockOn('search-by', 'radio-button').val('context');

                expect(form._getTooltip()).to.not.haveMod('shown', 'yes');
            });

            it('При смене типа поиска, должен поменяться placeholder у критерия', function() {
                var input = block.findBlockOn('input-value', 'input'),
                    radioButton = block.findBlockOn('search-by', 'radio-button'),
                    initialHint = input.elem('control').attr('placeholder'),
                    changedHint;

                radioButton.val('context');
                changedHint = input.elem('control').attr('placeholder');

                expect(initialHint).to.not.be.eq(changedHint);
            });

            it('Если группы не найдены, должен выставиться модификатор result_no', function() {
                block.findBlockOn('input-value', 'input').val('123456');
                block.findBlockOn('submit', 'button').trigger('click');
                sandbox.server.respond([200, { 'Content-Type': 'application/json' }, '0']);

                expect(block).to.haveMod('result', 'no');
            });
        });

    });
});

