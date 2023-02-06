describe('b-dont-show-domains', function() {
    var ctx = {
            block: 'b-dont-show-domains'
        },
        sspPlatforms = ['Google','Inner-active','MoPub','MobFox','Rubicon','Smaato','Smaato (test)','sspplatform.ru'],
        domains = ['003ms.ru', '1001goroskop.ru', '24open.ru', '8lap.ru' ],
        block,
        input,
        add,
        campaign,
        sandbox;

    beforeEach(function() {
        block = u.createBlock(ctx);
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        sandbox.stub(BEM.blocks['i-web-api-request'].disabledDomains, 'getInternalPagesDomains')
            .callsFake(function() {
                return Promise.resolve([]);
            });
        campaign = createModel(domains, [], sspPlatforms);
        input = block.findBlockOn('input', 'input');
        add = block.findBlockOn('add', 'button');

        block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

    });

    afterEach(function() {
        campaign.destruct();
        block.destruct();
        sandbox.restore();
    });


    function createDomainsList(limit, returnArray) {
        var line = '',
            array = [];
        for (var i = 0; i <= limit; i++) {
            var newDomain = 'ii' + i + '.ms.ru';
            line = line + newDomain + ', ';
            array.push(newDomain)
        }
        return returnArray ? array : line;
    }

    function createModel(domains, ssp, dictionary) {
        return BEM.MODEL.create({ id: '8027269', name: 'm-campaign' }, {
            DontShow: domains,
            disabled_ssp: ssp,
            ssp_platforms: dictionary
        });
    }

    describe('HTML', function() {
        it('Если в списке 1 площадка, в HTML только 1 чекбокс', function() {
            campaign = createModel(['003ms.ru'], [], []);

            block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

            expect(block.findBlocksInside('checkbox').length).to.equal(1);
        });

        it('Если в списке более 1 площадки, в HTML плоское дерево и чекбокс ВСЕ', function() {
            expect(block.elem('all').length).to.equal(1);
            expect(block.elem('domain').length).to.equal(4);
        });

        it('Если в списке нет площадок, в HTML нет чекбоксов', function() {
            campaign = createModel([], [], []);

            block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

            expect(block.findBlocksInside('checkbox').length).to.equal(0);
        });
    });

    describe('Метод утилит separatePlatforms', function() {
        var platforms;
        [
            {
                dontShow: [],
                dictionary: sspPlatforms,
                text: 'Если передан пустой список, возвращает объект с пустыми полями и переданным списком платформ-ssp',
                expectance: {
                    disabledDomains: [],
                    disabledSsp: [],
                    sspDictionary: sspPlatforms
                }
            },
            {
                dontShow: ['003ms.ru'],
                dictionary: sspPlatforms,
                text: 'Если передан только список доменов, возвращает объект с пустым полем ssp и заполненным - domains',
                expectance: {
                    disabledDomains: ['003ms.ru'],
                    disabledSsp: [],
                    sspDictionary: sspPlatforms
                }
            },
            {
                dontShow: ['MoPub'],
                dictionary: sspPlatforms,
                text: 'Если передан только список ssp, возвращает объект с пустым полем domains и заполненным - ssp',
                expectance: {
                    disabledDomains: [],
                    disabledSsp: ['MoPub'],
                    sspDictionary: sspPlatforms
                }
            },
            {
                dontShow: ['MoPub', '003.ms.ru'],
                dictionary: sspPlatforms,
                text: 'Если передан список с доменами и ssp, возвращает объект с заполненными полями',
                expectance: {
                    disabledDomains: ['003.ms.ru'],
                    disabledSsp: ['MoPub'],
                    sspDictionary: sspPlatforms
                }
            },
            {
                dontShow: ['003.ms.ru', 'MoPub'],
                dictionary: [],
                text: 'Если передан пустой словарь ssp, возвращает объект с пустым полем sspDictionary и ssp, поле domains соответствует переданному списку площадок',
                expectance: {
                    disabledDomains: ['003.ms.ru', 'MoPub'],
                    disabledSsp: [],
                    sspDictionary: []
                }
            },
            {
                dontShow: [],
                dictionary: [],
                text: 'Если передан пустой словарь ssp и пустой список, возвращает объект с пустыми полями',
                expectance: {
                    disabledDomains: [],
                    disabledSsp: [],
                    sspDictionary: []
                }
            }
        ].forEach(function(test) {
            it(test.text, function() {
                platforms = u['b-dont-show-domains'].separatePlatforms(test.dontShow, test.dictionary);

                expect(platforms).to.deep.equal(test.expectance);
            });
        });
    });

    describe('Метод prepareToShow', function() {
        it('После вызова метода очищается поле ввода', function() {
            expect(input.val()).to.equal('');
        });

        it('После вызова метода все переданные площадки отображаются в HTML', function() {
            var hasAllDomains = u._.every(domains, function(domain) {
                return !!block.findBlocksInside('domain', 'checkbox').filter(function(c) { return c.value == domain });
            });

            expect(hasAllDomains).to.equal(true);
        });

        it('После вызова метода все переданные площадки отображаются в HTML отмеченными', function() {
            var allDomainsChecked = u._.every(domains, function(domain) {
                return !!block.findBlocksInside('domain', 'checkbox').filter(function(c) { return c.isChecked });
            });

            expect(allDomainsChecked).to.equal(true);
        });
    });

    describe('Метод provideData', function() {
        it('Если выбрано площaдок больше лимита, показывается предупреждение', function() {
            sandbox.spy(BEM.blocks['b-confirm'], 'alert');

            sandbox.stub(window, 'iget2')
                .withArgs('b-dont-show-domains', 'vy-mozhete-otklyuchit-ne-bolee-ploshadok')
                .returns('test-error-message');

            sandbox.stub(u, 'consts').withArgs('limitPlatforms').returns(1000);

            input.val(createDomainsList(1001, false));
            add.trigger('click');
            block.provideData();

            sandbox.clock.tick(100);

            expect(BEM.blocks['b-confirm'].alert.calledWith('test-error-message')).to.equal(true);
            BEM.blocks['b-confirm']._buttonYes.trigger('click');
            BEM.blocks['b-confirm'].alert.restore();
        });

        it('Если выбрано площaдок меньше лимита, список отмеченных площадок записывается в модель', function() {
            input.val(createDomainsList(10, false));
            add.trigger('click');
            block.provideData();

            sandbox.clock.tick(100);

            // массив от модели с дополнительными методами
            expect(JSON.stringify(campaign.get('DontShow'))).to.equal('["ii0.ms.ru","ii1.ms.ru","ii2.ms.ru","ii3.ms.ru",' +
                '"ii4.ms.ru","ii5.ms.ru","ii6.ms.ru","ii7.ms.ru","ii8.ms.ru","ii9.ms.ru","ii10.ms.ru","003ms.ru",' +
                '"1001goroskop.ru","24open.ru","8lap.ru"]')
        });
    });

    describe('Вызов метода _processInput', function() {
        beforeEach(function() {
            sandbox.spy(block, '_processInput');
            input.val(createDomainsList(10, false));
        });

        it('При нажатии на ENTER дергается метод _processInput', function() {
            var e = $.Event('keypress');
            e.keyCode = 13; // Enter

            input.elem('control').trigger(e);

            sandbox.clock.tick(100);

            expect(block._processInput.called).to.equal(true);
        });

        it('При нажатии на ДОБАВИТЬ дергается метод _processInput', function() {
            add.trigger('click');

            sandbox.clock.tick(100);

            expect(block._processInput.called).to.equal(true);
        });
    });

    describe('Метод processInput', function() {
        beforeEach(function() {
            sandbox.spy(BEM.blocks['b-confirm'], 'alert');
            sandbox.stub(u, 'consts').withArgs('limitPlatforms').returns(1000);
        });

        afterEach(function() {
            BEM.blocks['b-confirm']._buttonYes.trigger('click');
            BEM.blocks['b-confirm'].alert.restore();
        });

        it('Если выбрано площадок больше лимита, показывается предупреждение', function() {

            campaign = createModel(createDomainsList(1001, true), [], []);
            block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

            add.trigger('click');
            expect(BEM.blocks['b-confirm'].alert.called).to.equal(true);
        });

        it('Если площадок в поле ввода больше лимита, показывается предупреждение', function() {
            campaign = createModel([], [], []);
            block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

            input.val(createDomainsList(1001, false));
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.called).to.equal(true);
        });

        it('Если сумма выбранных площадок и площадок в поле ввода больше лимита, показывается предупреждение', function() {
            input.val(createDomainsList(998, false));
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.called).to.equal(true);
        });

        describe('Если площадка - ssp', function() {
            it('Если площадки нет в списке выбранных, она туда добавляется', function() {
                campaign = createModel([], [], sspPlatforms);

                input.val('MobFox');
                add.trigger('click');

                var sspPlatform = block.findBlocksInside('ssp', 'checkbox').filter(function(c) { return c.val() == 'MobFox' });
                expect(sspPlatform.length).to.equal(1);
            });

            it('Если площадка есть в списке выбранных, она туда не добавляется', function() {
                campaign = createModel([], ['MobFox'], sspPlatforms);
                block.prepareToShow({ modelParams: { id: '8027269', name: 'm-campaign' } });

                input.val('MobFox');
                add.trigger('click');

                var sspPlatform = block.findBlocksInside('ssp', 'checkbox').filter(function(c) { return c.val() == 'MobFox' });
                expect(sspPlatform.length).to.equal(1);
            });

            it('Метод inWhiteList не дергается', function() {
                sandbox.spy(block, '_inWhiteList');
                campaign = createModel([], [], sspPlatforms);

                input.val('MobFox');
                add.trigger('click');

                expect(block._inWhiteList.called).to.equal(false);
            });

            it('Площадка исчезает из поля ввода', function() {
                campaign = createModel([], [], sspPlatforms);

                input.val('MobFox');
                add.trigger('click');

                expect(input.val()).to.equal('');
            })
        });

        describe('Если площадка не ssp', function() {
            beforeEach(function() {
                sandbox.spy(block, '_generateErrorsHtml');
            });

            it('Если площадка не Url и не AppId, она остается в поле ввода', function() {
                input.val('mmm.');
                add.trigger('click');

                expect(input.val().indexOf('mmm.')).not.to.equal(-1);
            });

            it('Если площадка не Url и не AppId, она добавляется в поле invalid списка ошибок', function() {
                input.val('mmm.');
                add.trigger('click');

                expect(block._generateErrorsHtml.args[0][0].invalid).to.deep.equal(['mmm.']);
            });

            it('Если площадка Yandex.ru, она остается в поле ввода', function() {
                input.val('yandex.ru');
                add.trigger('click');

                expect(input.val().indexOf('yandex.ru')).not.to.equal(-1);
            });

            it('Если площадка Яндекс.рф, она остается в поле ввода', function() {
                input.val('яндекс.рф');
                add.trigger('click');

                expect(input.val().indexOf('яндекс.рф')).not.to.equal(-1);
            });

            it('Если площадка xn--d1acpjx3f.xn--p1ai == яндекс.рф, она остается в поле ввода', function() {
                input.val('xn--d1acpjx3f.xn--p1ai');
                add.trigger('click');

                expect(input.val().indexOf('xn--d1acpjx3f.xn--p1ai')).not.to.equal(-1);
            });

            it('Если площадка mail.ru, она остается в поле ввода', function() {
                input.val('mail.ru');
                add.trigger('click');

                expect(input.val().indexOf('mail.ru')).not.to.equal(-1);
            });

            it('Если площадка в белом списке, она добавляется в поле whiteList списка ошибок', function() {
                input.val('yandex.ru');
                add.trigger('click');

                expect(block._generateErrorsHtml.args[0][0].whiteList).to.deep.equal(['yandex.ru']);
            });

            it('Если домен общего пользования, он остается в поле ввода', function() {
                input.val('msk.ru');
                add.trigger('click');

                expect(input.val().indexOf('msk.ru')).not.to.equal(-1);
            });

            it('Если домен общего пользования, он добавляется в поле common списка ошибок', function() {
                input.val('msk.ru');
                add.trigger('click');

                expect(block._generateErrorsHtml.args[0][0].common).to.deep.equal(['msk.ru']);
            });

            it('Если длина площадки больше 255, она убирается из поля ввода', function() {
                var longDomain = new Array(253).join('m') + '.ru';

                input.val(longDomain);
                add.trigger('click');

                expect(input.val().indexOf(longDomain)).to.equal(-1);
            });

            it('Если длина площадки больше 255, она добавляется в поле maxLen списка ошибок', function() {
                var longDomain = new Array(253).join('m') + '.ru';

                input.val(longDomain);
                add.trigger('click');

                expect(block._generateErrorsHtml.args[0][0].maxLen).to.deep.equal([longDomain]);
            });

            describe('Если домен валидный', function() {
                it('Если площадки нет в списке выбранных, она туда добавляется', function() {
                    input.val('mymom.ru');
                    add.trigger('click');

                    var domain = block.findBlocksInside('domain', 'checkbox').filter(function(c) { return c.val() == 'mymom.ru' });

                    expect(domain.length).to.equal(1);

                });

                it('Если площадка есть в списке выбранных, она туда не добавляется', function() {
                    campaign = createModel(['mymom.ru'], [], sspPlatforms);

                    input.val('mymom.ru');
                    add.trigger('click');

                    var domain = block.findBlocksInside('domain', 'checkbox').filter(function(c) { return c.val() == 'mymom.ru' });

                    expect(domain.length).to.equal(1);
                });

                it('Площадка исчезает из поля ввода', function() {
                    input.val('mymom.ru');
                    add.trigger('click');

                    block.findBlocksInside('domain', 'checkbox').filter(function(c) { return c.val() == 'mymom.ru' });

                    expect(input.val()).to.equal('');
                })
            });

            it('Если есть ошибка, показывается предупреждение', function() {
                input.val('msk.ru');
                add.trigger('click');

                expect(BEM.blocks['b-confirm'].alert.called).to.equal(true);
            });
        });
    });

    describe('Обработка ошибок', function() {
        beforeEach(function() {
            sandbox.spy(BEM.blocks['b-confirm'], 'alert');
        });

        afterEach(function() {
            BEM.blocks['b-confirm']._buttonYes.trigger('click');
            BEM.blocks['b-confirm'].alert.restore();
        });

        it('Если площадка не Url и не AppId, то в предупреждении есть текст "Неверно указана площадка/внешняя сеть:"', function() {
            input.val('mmm.');
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Неверно указана площадка/внешняя сеть: mmm.')).to.equal(true);
        });

        it('Если площадки не Url и не AppId, то в предупреждении есть текст "Неверно указаны площадки/внешние сети:"', function() {
            input.val('mmm., .uuu');
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Неверно указаны площадки/внешние сети: mmm., .uuu')).to.equal(true);
        });

        it('Если площадка в белом списке, то в предупреждении есть текст "Отключать показы на площадке"', function() {
            input.val('yandex.ru');
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Отключать показы на площадке yandex.ru нельзя.')).to.equal(true);
        });

        it('Если площадки в белом списке, то в предупреждении есть текст "Отключать показы на площадках"', function() {
            input.val('yandex.ru, яндекс.рф');
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Отключать показы на площадках yandex.ru, яндекс.рф нельзя.')).to.equal(true);
        });

        it('Если домен общего пользования, то в предупреждении есть текст "Нельзя отключать показы для доменов общего использования:"', function() {
            input.val('msk.ru');
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Нельзя отключать показы для доменов общего использования: msk.ru')).to.equal(true);
        });

        it('Если длина площадки больше 255, то в предупреждении есть текст "Превышена максимально допустимая длина домена:"', function() {
            var longDomain = new Array(253).join('m') + '.ru';

            input.val(longDomain);
            add.trigger('click');

            expect(BEM.blocks['b-confirm'].alert.calledWith('Превышена максимально допустимая длина домена: mmmmmmmmmmmmmmmmmmmmmmmmmmmmmm&hellip;')).to.equal(true);
        });
    })
});
