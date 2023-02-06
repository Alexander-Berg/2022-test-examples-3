describe('b-minus-phrases-manager', function() {
    function createInstance(params) {
        var phrases = params.phrases || [],
            groupsSymbolsCounter = params.groupsSymbolsCounter || u._.uniq(phrases)
                .map(function(phr) { return phr.adgroupId; })
                .reduce(function(res, id) {
                    res[id] = 0;
                    return res;
                }, {}),
            campaignsSymbolsCounter = params.campaignsSymbolsCounter || u._.uniq(phrases)
                .map(function(phr) { return phr.cid; })
                .reduce(function(res, id) {
                    res[id] = 0;
                    return res;
                }, {}),
            statType = params.statType || 'mol',
            scrollableElemMix = params.scrollableElemMix;

        return u.getInitedBlock({
            block: 'b-minus-phrases-manager',
            mods: { 'stat-type': statType },
            js: {
                ctxData: {
                    phrases: phrases,
                    groupsSymbolsCounter: groupsSymbolsCounter,
                    campaignsSymbolsCounter: campaignsSymbolsCounter
                },
                scrollableElemMix: scrollableElemMix
            }
        });
    }

    function buildSelector() {
        return BEM.blocks['b-minus-phrases-manager'].buildSelector.apply(
            BEM.blocks['b-minus-phrases-manager'],
            arguments
        );
    }

    function getInputs(instance) {
        return instance.findBlocksInside('input');
    }

    function getInputByValue(instance, value) {
        return u._.find(getInputs(instance), function(input) {
            return input.val() === value;
        });
    }

    function getItemByValue(instance, value) {
        return getInputByValue(instance, value).domElem.closest(buildSelector('phrase-item'));
    }

    function removeByValue(instance, value) {
        var item = getItemByValue(instance, value),
            removeButtonDomElem = instance.findBlockInside(item, 'b-control-remove-button').elem('button');

        removeButtonDomElem.trigger('click');
    }

    function trueDestruct(instance) {
        BEM.DOM.destruct(instance.domElem);

        // Иначе не удалится `tooltip`, т.к. в `destruct` блока `tipman` написан `afterCurrentEvent`
        sandbox.clock && sandbox.clock.tick(0);
    }

    function getInternalTargetId(phrase, isNeedInvertedValue) {
        var isTargetCampaign = phrase.target === 'campaign',
            isTotalTargetCampaign = isNeedInvertedValue ? !isTargetCampaign : isTargetCampaign;

        return isTotalTargetCampaign ? ('c' + phrase.cid) : ('g' + phrase.adgroupId);
    }

    var sandbox,
        constsStub;

    beforeEach(function() {
        sandbox = sinon.sandbox.create();

        constsStub = sandbox.stub(u, 'consts');

        constsStub.withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('При отрисовке', function() {
        it('Фразы отсортированы по порядку следования кампаний и по номерам групп', function() {
            var instance = createInstance({
                phrases: [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ]
            });

            expect(instance.findBlocksInside('input').map(function(input) { return input.val() })).to.eql([
                '24-10',
                '24-50',
                '12-30',
                '12-40',
                '400-12'
            ]);

            trueDestruct(instance);
        });

        it('При модификаторе `_stat-type_moc` (статистика на кампанию) не отображается столбец камапнии', function() {
            var instance = createInstance({
                    phrases: [{ text: '24-50', cid: 24, adgroupId: 50 }],
                    statType: 'moc'
                }),
                campaignColumns = instance.findElem('target-info-campaign header-target-info-campaign');

            expect(!!campaignColumns.length).to.be.false;

            trueDestruct(instance);
        });

        it(
            'Только у первой фразы, и у фразы следующей за фразой из другой кампании, есть модификатор `_separated_yes`',
            function() {
                var instance = createInstance({
                    phrases: [
                        { text: '12-30', cid: 12, adgroupId: 30 },
                        { text: '12-40', cid: 12, adgroupId: 40 },
                        { text: '24-10', cid: 24, adgroupId: 10 },
                        { text: '24-50', cid: 24, adgroupId: 50 }
                    ]
                });

                expect(instance).to.haveMod(getItemByValue(instance, '12-30'), 'separated', 'yes');
                expect(instance).to.not.haveMod(getItemByValue(instance, '12-40'), 'separated', 'yes');
                expect(instance).to.haveMod(getItemByValue(instance, '24-10'), 'separated', 'yes');
                expect(instance).to.not.haveMod(getItemByValue(instance, '24-50'), 'separated', 'yes');

                trueDestruct(instance);
            }
        );

        it('При превышениях в лимитах ошибки подсветятся сразу', function() {
            sandbox.useFakeTimers({ toFake: ['setInterval', 'setTimeout'] });

            constsStub.withArgs('GROUP_MINUS_WORDS_LIMIT').returns(0);

            var phrases = [
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '24-50', cid: 24, adgroupId: 50 }
                ],
                instance = createInstance({ phrases: phrases });

            sandbox.clock.tick(550);

            expect(instance.findElem('error', 'showed', 'yes').length).to.eql(phrases.length);

            trueDestruct(instance);
        });

        it('До внесения изенений `isChanged()` возвращает `false`', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            var instance = createInstance({ phrases: [{ text: '24-50', cid: 24, adgroupId: 50 }] });

            // До подсчета количества свободных символов
            expect(instance.isChanged()).to.be.false;

            sandbox.clock.tick(500);

            // После подсчета количества свободных символов
            expect(instance.isChanged()).to.be.false;

            trueDestruct(instance);
        });

        it('Корзина видна при ховере на фразы (фраз > 1)', function() {
            var phrases = [
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ],
                instance = createInstance({ phrases: phrases });

            phrases.forEach(function(phr) {
                var item = getItemByValue(instance, phr.text);

                instance.setMod(item, 'test-only-hover', 'mod');

                expect(instance.findElem(item, 'phrase-item-remove').is(':visible')).to.be.true;
            });

            trueDestruct(instance);
        });

        it('Корзина не видна для последней (единственной) фразы', function() {
            var phrases = [{ text: '400-12', cid: 400, adgroupId: 12 }],
                instance = createInstance({ phrases: phrases });

            phrases.forEach(function(phr) {
                var item = getItemByValue(instance, phr.text);

                instance.setMod(item, 'test-only-hover', 'mod');

                expect(instance.findElem(item, 'phrase-item-remove').is(':visible')).to.be.false;
            });

            trueDestruct(instance);
        });
    });

    describe('При изменении цели (кампания/группа)', function() {
        it('минус-фразы в шапке, меняется цель каждой фразы', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            var instance = createInstance({
                phrases: [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40, target: 'campaign' },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12, target: 'campaign' }
                ]
            });

            ['group', 'campaign', 'group'].forEach(function(target) {
                instance.findBlockOn('header-target', 'select').val(target);

                sandbox.clock.tick(100);

                expect(instance.findBlocksInside('phrases-list', 'select').every(function(select) {
                    return select.val() === target
                }));
            });

            trueDestruct(instance);
        });

        it('Конкретной минсу-фразы, пересчитывается цель в шапке', function() {
            var instance = createInstance({
                    phrases: [
                        { text: '24-50', cid: 24, adgroupId: 50 },
                        { text: '12-40', cid: 12, adgroupId: 40 },
                        { text: '12-30', cid: 12, adgroupId: 30 },
                        { text: '24-10', cid: 24, adgroupId: 10 },
                        { text: '400-12', cid: 400, adgroupId: 12 }
                    ]
                }),
                phrasesSelects = instance.findBlocksInside('phrases-list', 'select'),
                headerSelect = instance.findBlockOn('header-target', 'select');

            phrasesSelects[2].val('campaign');

            expect(headerSelect.val()).to.eql('undefined');

            phrasesSelects[2].val('group');

            expect(headerSelect.val()).to.eql('group');

            phrasesSelects.forEach(function(select) {
                return select.val('campaign');
            });

            expect(headerSelect.val()).to.eql('campaign');

            trueDestruct(instance);
        });

        it('Должен вызваться пересчет символов', function() {
            // sandbox.useFakeTimers();

            var phrases = [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ],
                instance = createInstance({ phrases: phrases }),
                phrasesSelects = instance.findBlocksInside('phrases-list', 'select'),
                vmAddToQueueToCalcCountersSpy = sandbox.spy(instance._vm, '_addToQueueToCalcCounters');

            ['12-30', '400-12'].forEach(function(value) {
                var index = u._.findIndex(phrases, function(phr) { return phr.text === value });

                phrasesSelects[index].val('campaign');

                var targetIds = [
                    getInternalTargetId(phrases[index], true),
                    getInternalTargetId(phrases[index])
                ];

                expect(vmAddToQueueToCalcCountersSpy.calledWith(targetIds)).to.be.true;
            });

            trueDestruct(instance);
        })
    });

    describe('При удалении минус-фразы', function() {
        it('`isChanged()` возвращает `true`', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var instance = createInstance({
                phrases: [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ]
            });

            sandbox.clock.tick(500);

            removeByValue(instance, '24-10');

            sandbox.clock.tick(0);

            expect(instance.isChanged()).to.be.true;

            trueDestruct(instance);
        });

        it('Модификатор `_separated_yes` переходит к следующей фразе из той же кампании', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var instance = createInstance({
                phrases: [
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ]
            });

            sandbox.clock.tick(0);

            var notSeparatedItem = getItemByValue(instance, '24-50');

            expect(instance).to.not.haveMod(notSeparatedItem, 'separated', 'yes');

            removeByValue(instance, '24-10');

            sandbox.clock.tick(0);

            expect(instance).to.haveMod(notSeparatedItem, 'separated', 'yes');

            trueDestruct(instance);
        });

        it('Должен вызваться пересчет символов', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var phrases = [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12, target: 'campaign' }
                ],
                instance = createInstance({ phrases: phrases }),
                vmAddToQueueToCalcCountersSpy = sandbox.spy(instance._vm, '_addToQueueToCalcCounters');

            ['24-10', '400-12'].forEach(function(value) {
                var internalVmIndex = getInternalTargetId(u._.find(phrases, function(phr) {
                    return phr.text === value
                }));

                removeByValue(instance, value);

                sandbox.clock.tick(0);

                expect(vmAddToQueueToCalcCountersSpy.calledWith([internalVmIndex])).to.be.true;
            });

            trueDestruct(instance);
        });
    });

    describe('При изменении текста', function() {
        it('`isChanged()` возвращает `true`', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            var instance = createInstance({
                phrases: [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12 }
                ]
            });

            sandbox.clock.tick(500);

            getInputByValue(instance, '12-40').val('12312412312');

            sandbox.clock.tick(0);

            expect(instance.isChanged()).to.be.true;

            trueDestruct(instance);
        });

        it('Должен вызваться пересчет символов', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            var phrases = [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12, target: 'campaign' }
                ],
                instance = createInstance({ phrases: phrases }),
                vmAddToQueueToCalcCountersSpy = sandbox.spy(instance._vm, '_addToQueueToCalcCounters');

            ['24-10', '400-12'].forEach(function(value) {
                var internalVmIndex = getInternalTargetId(u._.find(phrases, function(phr) {
                    return phr.text === value;
                }));

                getInputByValue(instance, value).val('12312412312');

                sandbox.clock.tick(0);

                expect(vmAddToQueueToCalcCountersSpy.calledWith([internalVmIndex])).to.be.true;
            });

            trueDestruct(instance);
        });

        it('Если текст пустой - должна быть ошибка', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            var instance = createInstance({
                phrases: [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '12-40', cid: 12, adgroupId: 40 },
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 24, adgroupId: 10 },
                    { text: '400-12', cid: 400, adgroupId: 12, target: 'campaign' }
                ]
            });

            ['24-10', '400-12'].forEach(function(oldVal) {
                var item = getItemByValue(instance, oldVal);

                getInputByValue(instance, oldVal).val('');

                sandbox.clock.tick(0);

                expect(instance).to.haveMod(instance.findElem(item, 'error'), 'showed', 'yes');
            });

            trueDestruct(instance);
        });
    });

    describe('Пересчет количества свободных символов', function() {
        it('Вычисления не начнутся пока после последнего добавления в очередь не пройдет 500мс', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var instance = createInstance({}),
                calcSpy = sandbox.spy(instance._vm, '_calcOverflowSymbolsCount');
            instance._vm, '_addToQueueToCalcCounters'

            instance._vm._addToQueueToCalcCounters([1, 2]);
            instance._vm._addToQueueToCalcCounters([2, 3]);
            sandbox.clock.tick(100);
            instance._vm._addToQueueToCalcCounters([2, 3]);
            instance._vm._addToQueueToCalcCounters([0, 2, 5]);

            expect(calcSpy).to.not.be.called;

            sandbox.clock.tick(500);

            expect(calcSpy.calledWith([1, 2, 3, 0, 5])).to.be.true;

            trueDestruct(instance);
        });

        it('Происходит верно для групп', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var maxCount = 100,
                startCount = 50;

            constsStub.withArgs('GROUP_MINUS_WORDS_LIMIT').returns(maxCount);

            var phrases = [
                    { text: '12-30', cid: 12, adgroupId: 30 },
                    { text: '12-40', cid: 12, adgroupId: 30 },
                    { text: '24-10', cid: 12, adgroupId: 30 }
                ],
                instance = createInstance({
                    phrases: phrases,
                    groupsSymbolsCounter: { 30: startCount }
                });

            sandbox.clock.tick(500);

            var mustBeValue = phrases.reduce(function(res, phrase) {
                return res += phrase.text.length
            }, startCount) - maxCount;

            expect(instance._vm.toJSON().phrases.every(function(phrase) {
                return phrase.overflowSymbolsCount === mustBeValue;
            })).to.be.true;

            trueDestruct(instance);
        });

        it('Происходит верно для кампаний', function() {
            sandbox.useFakeTimers({toFake: ['setInterval', 'setTimeout']});

            var maxCount = 100,
                startCount = 50;

            constsStub.withArgs('CAMPAIGN_MINUS_WORDS_LIMIT').returns(maxCount);

            var phrases = [
                    { text: '12-30', cid: 12, adgroupId: 30, target: 'campaign' },
                    { text: '12-40', cid: 12, adgroupId: 30, target: 'campaign' },
                    { text: '24-10', cid: 12, adgroupId: 30, target: 'campaign' }
                ],
                instance = createInstance({
                    phrases: phrases,
                    campaignsSymbolsCounter: { 12: startCount }
                });

            sandbox.clock.tick(500);

            var mustBeValue = phrases.reduce(function(res, phrase) {
                return res += phrase.text.length
            }, startCount) - maxCount;

            expect(instance._vm.toJSON().phrases.every(function(phrase) {
                return phrase.overflowSymbolsCount === mustBeValue;
            })).to.be.true

            trueDestruct(instance);
        });
    });

    describe('Отображение ошибок', function() {
        it('При наведении мышки на ошибку отображается подсказка с текстом ошибки, по уходу пропадает', function() {
            sandbox.useFakeTimers({toFake: ['setInterval']});

            constsStub.withArgs('GROUP_MINUS_WORDS_LIMIT').returns(0);

            var phrases = [{ text: '12-30', cid: 12, adgroupId: 30 }],
                instance = createInstance({ phrases: phrases }),
                tipmanShowSpy,
                tipmanHideSpy;

            sandbox.clock.tick(500);

            // Задаем блок подсказки, чтобы использовался он, а не создался новый, и можно было отследить его показ
            instance._tipman = BEM.create('tipman');

            tipmanShowSpy = sandbox.spy(instance._tipman, 'show');
            tipmanHideSpy = sandbox.spy(instance._tipman, 'hide');

            instance.findElem('error', 'showed', 'yes').trigger('pointerover');

            expect(tipmanShowSpy).to.have.been.calledOnce;

            instance.findElem('error', 'showed', 'yes').trigger('pointerout');

            expect(tipmanHideSpy).to.have.been.calledOnce;

            trueDestruct(instance);
        });

        it('При изменении текста - ошибка должна исчезнуть', function() {
            var phrases = [{ text: '24-50', cid: 24, adgroupId: 50 }],
                instance = createInstance({ phrases: phrases });

            instance._vm.get('phrases').forEach(function(phraseItemVm) {
                phraseItemVm.set('errorFromServer', 'ошибонька')
            });

            expect(instance.findElem('error', 'showed', 'yes').length).to.eql(phrases.length);

            phrases.forEach(function(phr) {
                getInputByValue(instance, phr.text).val('111')
            });

            expect(instance.findElem('error', 'showed', 'yes').length).to.eql(0);

            trueDestruct(instance);
        });

        it('при попытке сохранения, если есть ошибки', function() {
            var phrases = [{ text: '24-50', cid: 24, adgroupId: 50 }],
                instance = createInstance({ phrases: phrases }),
                showErrorsSpy = sandbox.spy(instance, '_showErrors');

            instance._vm.get('phrases').forEach(function(phraseItemVm) {
                phraseItemVm.set('errorFromServer', 'ошибонька')
            });

            instance.save();

            expect(showErrorsSpy).to.have.been.calledOnce;

            trueDestruct(instance);
        });

        it('При показе ошибок, если ошибка есть в поле видимости - то тултип покажется к ней', function() {
            var phrases = [
                    { text: '24-50', cid: 24, adgroupId: 50 },
                    { text: '24-60', cid: 24, adgroupId: 50 },
                    { text: '24-70', cid: 24, adgroupId: 50 },
                    { text: '24-80', cid: 24, adgroupId: 50 }
                ],
                instance = createInstance({ phrases: phrases }),
                scrollableElem = instance.elem('phrases-list'),
                itemHeight = instance.elem('phrase-item').eq(0).outerHeight(),
                showErrorTooltipSpy = sandbox.spy(instance, '_showErrorTooltip'),
                scrollNodeToSpy = sandbox.spy(u, 'scrollNodeTo');

            instance.elem('phrases-list').css({ 'overflow-y': 'auto', height: itemHeight });
            scrollableElem.scrollTop(2 * itemHeight + 1);

            instance._vm.get('phrases', 'raw').forEach(function(phraseItem) {
                phraseItem.set('errorFromServer', 'ошибонька');
            });

            instance._showErrors();

            expect(showErrorTooltipSpy).to.have.been.calledOnce;
            expect(scrollNodeToSpy).to.not.have.been.called;

            trueDestruct(instance);
        });

        it(
            'При показе ошибок, если ошибки в поле видимости нет - то подскроллим и покажем тултип к первой ошибке',
            function() {
                var phrases = [
                        { text: '24-50', cid: 24, adgroupId: 50 },
                        { text: '24-60', cid: 24, adgroupId: 50 },
                        { text: '24-70', cid: 24, adgroupId: 50 },
                        { text: '24-80', cid: 24, adgroupId: 50 }
                    ],
                    instance = createInstance({ phrases: phrases }),
                    itemHeight = instance.elem('phrase-item').eq(0).outerHeight(),
                    showErrorTooltipSpy = sandbox.spy(instance, '_showErrorTooltip'),
                    scrollNodeToSpy = sandbox.spy(u, 'scrollNodeTo');

                instance.elem('phrases-list').css({ 'overflow-y': 'auto', height: itemHeight });

                instance._vm.get('phrases', 'raw').slice(2).forEach(function(phraseItem) {
                    phraseItem.set('errorFromServer', 'ошибонька');
                });

                instance._showErrors();

                expect(showErrorTooltipSpy).to.have.been.calledOnce;
                expect(scrollNodeToSpy).to.have.been.calledOnce;

                trueDestruct(instance);
            }
        );
    });

    describe('Сохранение на сервере', function() {
        it('Запрос на пересечение с ключевыми фразами');

        it('Запрос на сохранение');

        it('Обработка ошибок серверной валидации');
    });

});
