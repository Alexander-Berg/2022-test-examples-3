describe('b-stat-phrases-manager', function() {
    var block,
        sandbox,
        constsStub,
        strategies = {
            // автоматическая стратегия
            autobudget: {
                is_net_stop: '',
                is_autobudget: '1',
                is_search_stop: '',
                search: { name: 'stop' },
                name: 'different_places',
                net: { avg_bid: '10', name: 'autobudget_avg_click', sum: null }
            },

            // ручное управление ставками на всех площадках
            netAndSearch: {
                name: 'different_places',
                is_net_stop: '',
                is_autobudget: '',
                is_search_stop: '',
                search: { name: '' },
                net: { name: 'maximum_coverage' }
            },

            // ручное управление ставками только на поиске
            netStop: {
                name: '',
                is_net_stop: '1',
                is_autobudget: '',
                is_search_stop: '',
                search: { place: 'both', name: 'min_price' },
                net: { name: 'default' }
            },

            // ручное управление ставками только в сетях
            searchStop: {
                name: '',
                is_net_stop: '',
                is_autobudget: '',
                is_search_stop: '1',
                search: { place: 'both', name: 'min_price' },
                net: { name: 'default' }
            }
        },
        blockData = {
            campaigns: {
                c1: { type: 'text', strategy: strategies.netAndSearch },
                c2: { type: 'text', strategy: strategies.netAndSearch },
                c3: { type: 'text', strategy: strategies.searchStop }
            },
            groups: {
                g1: { groupName: 'Группа 1', adgroupId: 'g1', campName: 'Кампания 1', cid: 'c1' },
                g2: { groupName: 'Группа 2', adgroupId: 'g2', campName: 'Кампания 1', cid: 'c1' },
                g3: { groupName: 'Группа 3', adgroupId: 'g3', campName: 'Кампания 2', cid: 'c2' },
                g4: { groupName: 'Группа 4', adgroupId: 'g4', campName: 'Кампания 3', cid: 'c3' }
            },
            phrases: [
                { pid: 'g1', src_phrase: 'Фраза 1', stat_target_phrase_id: '', src_type: 'ext_phrase' },
                { pid: 'g2', src_phrase: 'Фраза 2', stat_target_phrase_id: '', src_type: 'ext_phrase' }
            ],
            currency: 'RUB'
        },
        respondData = [
            {
                phrase: 'Фраза 1', src_phrase: 'Фраза 1', price: 179.7, price_context: 0.3,
                pid: 'g1', src_type: 'ext_phrase', stat_target_phrase_id: ''
            },
            {
                phrase: 'Фраза 2', src_phrase: 'Фраза 2', price: 172.5, price_context: 0.3,
                pid: 'g2', src_type: 'ext_phrase', stat_target_phrase_id: ''
            }
        ],
        /**
         * Строит блок
         * @param {Object} [data]
         *  @param {String} [data.type] - тип страницы статистики на которой используется блок
         *  @param {Array} [data.phrases] - список исходных фраз
         *  @param {Object} [data.groups] - информация о группах
         *  @param {Object} [data.campaigns] - информация о кампаниях(стратегия, тип)
         *  @param {String} [data.currency] - валюта
         * @param {Array} [respond] - нормализированный список фраз пришедший от сервера
         */
        createBlock = function(data, respond) {
            data || (data = {});

            block = u.createBlock({
                block: 'b-stat-phrases-manager',
                mods: { 'stat-type': data.type || 'mol' },
                js: u._.extend({}, blockData, data)
            }, {
                inject: true
                // hidden: false
            });

            // @belyanskii: очень странная правка,но с timeout отличным от 0 параллельные запросы падают
            block._request().params.timeout = 0;

            sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                { 'Content-Type': 'application/json' }, JSON.stringify(respond || respondData)]);

            sandbox.server.respond();
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeServer: true,
            useFakeTimers: true
        });

        u.stubCurrencies2(sandbox)
            .withArgs('MAX_PHRASE_LENGTH').returns(4096)
            .withArgs('MAX_WORDS_IN_KEYPHRASE').returns(7)
            .withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
    });

    afterEach(function() {
        block.destruct();
        u.restoreCurrencies();
        sandbox.restore();
    });

    describe('Отображение', function() {

        describe('Шапка', function() {

            it('Должен быть блок «Единая цена», если у кампании «Ручное управление ставками»', function() {
                createBlock();

                expect(block).to.haveElem('common-price', 1);
            });

            it('Не должен быть блок «Единая цена», если у кампаний «Автоматические стратегии»', function() {
                createBlock({
                    campaigns: {
                        c1: { type: 'text', strategy: strategies.autobudget }
                    }
                }, [
                    {
                        phrase: 'Фраза 1', src_phrase: 'Фраза 1', pid: 'g1',
                        src_type: 'ext_phrase', stat_target_phrase_id: ''
                    },
                    {
                        phrase: 'Фраза 2', src_phrase: 'Фраза 2', pid: 'g2',
                        src_type: 'ext_phrase', stat_target_phrase_id: ''
                    }
                ]);

                expect(block).not.haveElem('common-price');
            });

            it([
                'Должна перерисоваться шапка таблицы(блока «Единая цена» быть не должно)',
                'после удаления кампаний у которых «Ручное управление ставками»'
            ].join(', '), function() {
                createBlock({
                    campaigns: {
                        c1: { type: 'text', strategy: strategies.autobudget },
                        c2: { type: 'text', strategy: strategies.netAndSearch }
                    }
                }, [
                    {
                        phrase: 'Фраза 1', src_phrase: 'Фраза 1', pid: 'g3', price: 179.7, price_context: 0.3,
                        src_type: 'ext_phrase', stat_target_phrase_id: ''
                    },
                    {
                        phrase: 'Фраза 2', src_phrase: 'Фраза 2', pid: 'g1',
                        src_type: 'ext_phrase', stat_target_phrase_id: ''
                    }
                ]);

                block._removePhrase({
                    cid: 'c2',
                    phraseId: block.getMod(block.elem('phrase-item').eq(0), 'model-id')
                });

                expect(block).not.haveElem('common-price');
            });

            it('Должна быть колонка «Кампания», если блок используется на статистике по всем кампаниям', function() {
                createBlock();

                expect(block).to.haveElem('camp-title', 1);
            });

            it('Не должна быть колонка «Кампания», если блок используется на статистике по одной кампании', function() {
                createBlock({ type: 'moc' });

                expect(block).not.haveElem('camp-title');
            });

            it([
                'Должен дизейблится блок единой ставки (кнопка «Выставить»)',
                'пока блок находится по модификатором «progress» «yes»'
            ].join(', '), function() {
                createBlock();

                block.setMod('progress', 'yes');

                expect(block.findBlockInside('common-price', 'b-edit-phrase-price-stat-popup'))
                    .to.haveMod('disabled', 'yes');
            });

            it([
                'Должен раздизейбливатся блок единой ставки (кнопка «Выставить»)',
                'после снятия модификатора «progress» «yes»'
            ].join(', '), function() {
                createBlock();

                block
                    .setMod('progress', 'yes')
                    .delMod('progress');

                expect(block.findBlockInside('common-price', 'b-edit-phrase-price-stat-popup')).not.haveMod('disabled');
            });

        });

        describe('Список фраз', function() {

            describe('Общее поведение', function() {

                beforeEach(function() {
                    createBlock({}, [
                        {
                            phrase: 'Фраза 1', src_phrase: 'Фраза 1', pid: 'g3', price: 179.7, price_context: 0.3,
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        },
                        {
                            phrase: 'Фраза 2', src_phrase: 'Фраза 2', pid: 'g1', price: 179.7, price_context: 0.3,
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        }
                    ]);
                });

                it('Должна быть группировка по кампаниям', function() {
                    expect(block).to.haveElem('campaign-item', 2);
                });

                it('Должна быть видна кнопка удаления фразы, если общее количество фраз больше единицы', function() {
                    expect(block).to.haveMod('campaigns-list', 'show-remove-button', 'yes');
                });

                it('Не должна быть видна кнопка удаления фразы, если фраза является последней', function() {
                    block._removePhrase({
                        cid: 'c2',
                        phraseId: block.getMod(block.elem('phrase-item').eq(0), 'model-id')
                    });

                    expect(block).not.haveMod('campaigns-list', 'show-remove-button', 'yes');
                });

                it('Должен быть проставлен фокус первому инпуту с фразой', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                    expect(phraseInput).to.haveMod('focused', 'yes');
                });

                it('Должен подсвечиваться красным инпут у невалидной фразы', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                    phraseInput.val('');

                    expect(phraseInput).to.haveMod('highlight-border', 'red');
                });

                it('Должна сниматься с инпута красная подсветка, после того как фраза стала валидной', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                    phraseInput
                        .val('')
                        .val('фраза');

                    expect(phraseInput).not.haveMod('highlight-border');
                });

                it('Должна сбрасываться ставка если фраза невалидная', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input'),
                        modelData;

                    phraseInput.val('');

                    modelData = block.model.toJSON();

                    expect({
                        price: modelData.campaigns[0].list[0].price,
                        price_context: modelData.campaigns[0].list[0].price_context
                    }).to.eql({
                        price: 0,
                        price_context: 0
                    });
                });

                it('Должен открытся тултип с ошибкой по наведению курсора на невалидную фразу', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input'),
                        tipmanSpyDelShow;

                    phraseInput.val('');

                    // в первый pointerover создастся tipman
                    phraseInput.domElem.trigger('pointerover');
                    tipmanSpyDelShow = sandbox.spy(block._tipman, 'show');
                    phraseInput.domElem.trigger('pointerover');

                    expect(tipmanSpyDelShow.called).to.be.true;
                });

                it('Должен скрыться тултип с ошибкой, если курсора убрать курсор с невалидной фразы', function() {
                    var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input'),
                        tipmanSpyDelHide;

                    phraseInput.val('');
                    phraseInput.domElem.trigger('pointerover');

                    tipmanSpyDelHide = sandbox.spy(block._tipman, 'hide');

                    phraseInput.domElem.trigger('pointerout');

                    expect(tipmanSpyDelHide.called).to.be.true;
                });

            });

            describe('Ручное управление ставками', function() {

                beforeEach(function() {
                    createBlock({
                        campaigns: {
                            c1: { type: 'text', strategy: strategies.netAndSearch },
                            c2: { type: 'text', strategy: strategies.netStop },
                            c3: { type: 'text', strategy: strategies.searchStop }
                        }
                    }, [
                        {
                            phrase: 'Фраза 1', src_phrase: 'Фраза 1', pid: 'g1', price: 179.7, price_context: 0.3,
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        },
                        {
                            phrase: 'Фраза 2', src_phrase: 'Фраза 2', pid: 'g3', price: 179.7,
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        },
                        {
                            phrase: 'Фраза 3', src_phrase: 'Фраза 3', pid: 'g4', price_context: 0.3,
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        }
                    ]);
                });

                it('Должны быть указаны площадки', function() {
                    expect(block).to.haveElem('platform', 3);
                });

                it([
                    'Должен быть блок для одновременного редактирования ставок на поиске и в сетях',
                    'если реклама показывается на всех площадках'
                ].join(', '), function() {
                    var phraseItem = block.elem('phrase-item').eq(0),
                        editPhrasePricePopup = block.findBlockInside(phraseItem, 'b-edit-phrase-price-stat-popup');

                    expect(editPhrasePricePopup.domElem.length).to.equal(1);
                });

                it([
                    'Должен быть блок для корректировки ставки на поиске',
                    'если реклама показывается только на поиске'
                ].join(', '), function() {
                    var phraseItem = block.elem('phrase-item').eq(1),
                        editPhrasePrice = block.findBlockInside(phraseItem, 'b-edit-phrase-price');

                    expect(editPhrasePrice).to.haveMod('control-type', 'search');
                });

                it([
                    'Должен быть блок для корректировки ставки в сетях',
                    'если реклама показывается только в сетях'
                ].join(', '), function() {
                    var phraseItem = block.elem('phrase-item').eq(2),
                        editPhrasePrice = block.findBlockInside(phraseItem, 'b-edit-phrase-price');

                    expect(editPhrasePrice).to.haveMod('control-type', 'context');
                });

            });

            describe('Автоматическая стратегия', function() {

                beforeEach(function() {
                    createBlock({
                        campaigns: {
                            c1: { type: 'text', strategy: strategies.autobudget }
                        }
                    }, [
                        {
                            phrase: 'Фраза 1', src_phrase: 'Фраза 1', pid: 'g1',
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        },
                        {
                            phrase: 'Фраза 2', src_phrase: 'Фраза 2', pid: 'g2',
                            src_type: 'ext_phrase', stat_target_phrase_id: ''
                        }
                    ]);
                });

                it('Не должны быть указаны площадки', function() {
                    expect(block).not.haveElem('platform');
                });

            });

        });

    });

    describe('Методы', function() {

        beforeEach(function() {
            createBlock({
                campaigns: {
                    c1: { type: 'text', strategy: strategies.netAndSearch },
                    c2: { type: 'text', strategy: strategies.netStop },
                    c3: { type: 'text', strategy: strategies.searchStop }
                }
            }, [
                {
                    phrase: 'Фраза 1', src_phrase: 'Фраза 1', norm_phrase: 'Фраза 1', norm_phrase_unquoted: 'Фраза 1',
                    pid: 'g1', price: 179.7, price_context: 0.3, src_type: 'ext_phrase', stat_target_phrase_id: ''
                },
                {
                    phrase: 'Фраза 2', src_phrase: 'Фраза 2', norm_phrase: 'Фраза 2', norm_phrase_unquoted: 'Фраза 2',
                    pid: 'g3', price: 179.7, src_type: 'ext_phrase', stat_target_phrase_id: ''
                },
                {
                    phrase: 'Фраза 3', src_phrase: 'Фраза 3', norm_phrase: 'Фраза 3', norm_phrase_unquoted: 'Фраза 3',
                    pid: 'g4', price_context: 0.3, src_type: 'ext_phrase', stat_target_phrase_id: ''
                }
            ]);
        });

        describe('save', function() {

            it('Должен удалить добавленные группы, оставив только группы с ошибками', function() {
                block.save();

                // проверка ключевых фраз на пересечение с минус словами
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        ok: 1
                    })]);

                sandbox.server.respond();

                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        // не сохраненная группа, есть ошибки
                        g1: {
                            errors_by_phrases: [{
                                phrase: 'Фраза 1',
                                errors: ['Ошибка']
                            }]
                        },

                        // не сохраненная группа, есть ошибки
                        g3: {
                            errors_by_phrases: [{
                                phrase: 'Фраза 2',
                                errors: ['Ошибка']
                            }]
                        },

                        // успешно сохраненная группа, фраза не менялась
                        g4: {
                            phrases_added_qty: 1,
                            phrases: {
                                p4: {
                                    phrase: 'Фраза 3',
                                    norm_phrase: 'Фраза 3',
                                    numword: 2,
                                    price_context: 0.3,
                                    phrase_unglued_suffix: ''
                                }
                            }
                        }
                    })]);

                sandbox.server.respond();

                expect(block).to.haveElem('phrase-item', 2);
            });

            it('Должны подсвечиваться красным фразы с ошибками от сервера', function() {
                block.save();

                // проверка ключевых фраз на пересечение с минус словами
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        ok: 1
                    })]);

                sandbox.server.respond();

                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        // не сохраненная группа, есть ошибки
                        g1: {
                            errors_by_phrases: [{
                                phrase: 'Фраза 1',
                                errors: ['Ошибка']
                            }]
                        },

                        // не сохраненная группа, достигнут лимит фраз
                        g3: { is_group_oversized: '1' },

                        // успешно сохраненная группа, фраза не менялась
                        g4: {
                            phrases_added_qty: 1,
                            phrases: {
                                p2: {
                                    phrase: 'Фраза 3',
                                    norm_phrase: 'Фраза 3',
                                    numword: 2,
                                    price_context: 0.3,
                                    phrase_unglued_suffix: ''
                                }
                            }
                        }
                    })]);

                sandbox.server.respond();

                expect(block.findBlockInside(block.elem('phrase').eq(0), 'input'))
                    .to.haveMod('highlight-border', 'red');
            });

        });

        describe('findSourcePhrases', function() {

            it('Должен вернуть список фраз, добавленных в группы без изменений(поэтапное сохранение)', function() {
                block.save();

                // проверка ключевых фраз на пересечение с минус словами
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        ok: 1
                    })]);

                sandbox.server.respond();

                // ответ на первое сохранение
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        // успешно сохраненная группа, фраза не менялась
                        g1: {
                            phrases_added_qty: 1,
                            phrases: {
                                p1: {
                                    phrase: 'Фраза 1',
                                    norm_phrase: 'Фраза 1',
                                    norm_phrase_unquoted: 'Фраза 1',
                                    numword: 2,
                                    price_context: 0.3,
                                    phrase_unglued_suffix: ''
                                }
                            }
                        },

                        // не сохраненная группа, достигнут лимит фраз
                        g3: { is_group_oversized: '1' },

                        // успешно сохраненная группа, фраза была изменена
                        g4: {
                            phrases_added_qty: 1,
                            phrases: {
                                p2: {
                                    phrase: 'Другая фраза',
                                    norm_phrase: 'Другая фраза',
                                    norm_phrase_unquoted: 'Другая фраза',
                                    numword: 2,
                                    price_context: 0.3,
                                    phrase_unglued_suffix: ''
                                }
                            }
                        }
                    })]);

                sandbox.server.respond();
                block.save();

                // проверка ключевых фраз на пересечение с минус словами
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        ok: 1
                    })]);

                sandbox.server.respond();

                // ответ на второе сохранение
                sandbox.server.respondWith('POST', '/registered/main.pl', [200,
                    { 'Content-Type': 'application/json' }, JSON.stringify({
                        // успешно сохраненная группа, фраза не менялась
                        g3: {
                            phrases_added_qty: 1,
                            phrases: {
                                p2: {
                                    phrase: 'Фраза 2',
                                    norm_phrase: 'Фраза 2',
                                    norm_phrase_unquoted: 'Фраза 2',
                                    numword: 2,
                                    price_context: 0.3,
                                    phrase_unglued_suffix: ''
                                }
                            }
                        }
                    })]);

                sandbox.server.respond();

                // На сервере успешно сохранились 3 группы(общее количество фраз - 3)
                // findSourcePhrases - должен вернуть 2 фразы,
                // т.к. фраза группы g4 «Другая фраза» отлична от исходной «Фраза 3»
                expect(block.findSourcePhrases()).to.eql([
                    {
                        srcType: 'ext_phrase',
                        srcPhrase: 'Фраза 1',
                        normPhraseUnquoted: 'Фраза 1',
                        adgroupId: 'g1'
                    },
                    {
                        srcType: 'ext_phrase',
                        srcPhrase: 'Фраза 2',
                        normPhraseUnquoted: 'Фраза 2',
                        adgroupId: 'g3'
                    }
                ]);
            });

        });

        describe('sortGroupsByCampaigns', function() {

            it('Должен вернуть сгруппированные по компаниям номера групп', function() {
                expect(block.sortGroupsByCampaigns({
                    g1: {},
                    g2: {},
                    g3: { copied_from_pid: 'g2' },
                    g4: {}
                })).to.eql({
                    c1: {
                        groups: ['g1', 'g2', 'g3'],
                        copiedFrom: 'g2'
                    },
                    c3: {
                        groups: ['g4'],
                        copiedFrom: undefined
                    }
                });
            });

        });

        describe('getCurrentPhrasesCount', function() {

            it('Должен вернуть количество добавляемых фраз', function() {
                block._removePhrase({
                    cid: 'c1',
                    phraseId: block.getMod(block.elem('phrase-item').eq(0), 'model-id')
                });

                expect(block.getCurrentPhrasesCount()).to.eql(2);
            });

        });

        describe('isChanged', function() {

            it('Должен вернуть false, после построения блока', function() {
                expect(block.isChanged()).to.eql(false);
            });

            it('Должен вернуть true, если в блоке поменялась фраза, ставка, произошло удаление', function() {
                var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                phraseInput
                    .val('фраза 42');

                expect(block.isChanged()).to.eql(true);
            });

            it('Должен вернуть false, если значения вернулись к исходным', function() {
                var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                phraseInput
                    .val('фраза 42')
                    .val('Фраза 1');

                expect(block.isChanged()).to.eql(false);
            });

        });

        describe('isValid', function() {

            it('Должен вернуть true, если нет ошибок во фразах или ставках', function() {
                expect(block.isValid()).to.eql(true);
            });

            it('Должен вернуть false, если есть ошибоки во фразах или ставках', function() {
                var phraseInput = block.findBlockInside(block.elem('phrase').eq(0), 'input');

                phraseInput.val('');

                expect(block.isValid()).to.eql(false);
            });

        });

        describe('hasUnfinishedActions', function() {

            it('Должен вернуть true, если есть открытый попап редактирования ставок', function() {
                var editPhrasePrice = block.findBlockOn(block.elem('price').eq(0), 'b-edit-phrase-price-stat-popup');

                editPhrasePrice.trigger('show');

                expect(block.hasUnfinishedActions()).to.eql(true);
            });

            it('Должен вернуть false, если нет открытого попапа редактирования ставок', function() {
                var editPhrasePrice = block.findBlockOn(block.elem('price').eq(0), 'b-edit-phrase-price-stat-popup');

                editPhrasePrice.trigger('show');
                editPhrasePrice.trigger('hide');

                expect(block.hasUnfinishedActions()).to.eql(false);
            });

        });

    });

});
