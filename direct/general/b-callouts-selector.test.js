describe('b-callouts-selector', function() {
    var block,
        sandbox,
        constsStub,
        bChooser;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });
        block = u.createBlock(
            { block: 'b-callouts-selector' }
        );
        // блок надо положить в DOM, чтобы правильно отработала live-инициализация на liveBindTo
        $('body').append(block.domElem);
        constsStub = sandbox.stub(u, 'consts');

        constsStub.withArgs('MAX_CALLOUT_LENGTH_ON_BANNER').returns(25);
        constsStub.withArgs('MAX_CALLOUT_LENGTH').returns(25);
    });

    afterEach(function() {
        block.destruct();
        // убираем из DOM все лишнее
        $('.b-callouts-selector').remove();
        sandbox.restore();
    });

    function getCalloutById(id) {
        return block._model.get('list').getById(id);
    }

    function addNewCallout(text) {
        block._inputSearchAdd.val(text);
        block._buttonAddNew.trigger('click');
    }

    it('При отсутствии текста в строке поиска кнопка ДОБАВИТЬ задизейблена', function() {
        block._inputSearchAdd.val('');
        expect(block._buttonAddNew).to.haveMod('disabled');
    });

    describe('Метод val', function() {
        beforeEach(function() {
            sandbox.server.respondWith("GET",
            "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
            [200, {"Content-Type":"application/json"}, JSON.stringify(
                {
                    "requestId": 1,
                    success: true,
                    callouts: [
                        {
                            "callout_text": "Цена",
                            "additions_item_id": "81408",
                            "status_moderate": "Yes"
                        },
                        {
                            "status_moderate": "Yes",
                            "additions_item_id": "81409",
                            "callout_text": "Описание"
                        },
                        {
                            "status_moderate": "Yes",
                            "additions_item_id": "81415",
                            "callout_text": "Доставка"
                        }
                    ]
                })
            ]);
        });

        it('При наличии выбранных уточнений возвращает список уточнений', function() {
            block.reset({ callouts: [
                {
                    "callout_text": "Цена",
                    "additions_item_id": "81408",
                    "status_moderate": "Yes"
                }
            ]});

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block.val().callouts).to.deep.equal([{
                additions_item_id: "81408",
                callout_text: "Цена",
                status_moderate: "Yes"
            }]);
        });

        it('При отсутствии выбранных уточнений возвращает пустой список', function() {
            block.reset({ callouts: []});

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block.val().callouts.length).to.equal(0);
        })
    });

    describe('Метод reset', function() {
        beforeEach(function() {
            sandbox.server.respondWith("GET",
                "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                [200, {"Content-Type":"application/json"}, JSON.stringify(
                    {
                        "requestId": 1,
                        success: true,
                        callouts: [
                            {
                                "callout_text": "Цена",
                                "additions_item_id": "81408",
                                "status_moderate": "Yes"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81409",
                                "callout_text": "Описание"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81415",
                                "callout_text": "Доставка"
                            }
                        ]
                    })
                ]);

            sandbox.clock.tick(100);

            block.reset({ callouts: [
                    {
                        "callout_text": "Наличие",
                        "status_moderate": "Yes",
                        "additions_item_id": "81405"
                    },
                    {
                        "additions_item_id": "81407",
                        "status_moderate": "Yes",
                        "callout_text": "Фото"
                    }
                ]
            });
        });

        it('При вызове метода reset дергается ajax-запрос getBannersAdditions', function() {
            expect(sandbox.server.requests[0].url.indexOf('cmd=getBannersAdditions')).not.to.equal(-1);
        });

        it('Во время работы запроса на блоке есть модификатор loading', function() {
            expect(block).to.haveMod('loading');
        });

        it('После прихода результата снимается модификатор loading', function() {
            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);

            expect(block).not.to.haveMod('loading');
        })
    });

    describe('Отображение действий пользователя в VM', function() {
        beforeEach(function() {
            bChooser = block.findBlockOn('available-items', 'b-chooser');
            sandbox.server.respondWith("GET",
                "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                [200, {"Content-Type":"application/json"}, JSON.stringify(
                    {
                        "requestId": 1,
                        success: true,
                        callouts: [
                            {
                                "callout_text": "Цена",
                                "additions_item_id": "81408",
                                "status_moderate": "Yes"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81409",
                                "callout_text": "Описание"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81415",
                                "callout_text": "Доставка"
                            }
                        ]
                    })
                ]);
            block.reset({ callouts: [
                {
                    "callout_text": "Цена",
                    "additions_item_id": "81408",
                    "status_moderate": "Yes"
                }
            ]});

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);
        });

        it('При отмечании уточнения в списке доступных он получает selected:true в модели', function() {
            var idToSelect = '81409';

            bChooser.check(idToSelect);

            expect(getCalloutById(idToSelect).get('selected')).to.equal(true);
        });

        it('При отмечании уточнения в списке доступных обновляется счетчик длины текста выбранных', function() {
            var idToSelect = '81409',
                selectedLength = 12;
            bChooser.check(idToSelect);
            expect(block._model.get('selectedTextLength')).to.equal(selectedLength);
        });

        it('При снятии отметки с уточнения в списке доступных он получает selected:false в модели', function() {
            var idToUnselect = '81408';

            bChooser.uncheck(idToUnselect);

            expect(getCalloutById(idToUnselect).get('selected')).to.equal(false);
        });

        it('При удалении из списка выбранных уточнение получает selected:false', function() {
            var idToUnselect = '81408';

            block.findBlockInside({ block: 'b-callouts-selector-item', modName: 'type', modVal: 'selected' })
                .findElem('delete-selected-item').click();

            sandbox.clock.tick(100);

            expect(getCalloutById(idToUnselect).get('selected')).to.equal(false);
        });

        it('При удалении из списка доступных всплывает confirm', function() {
            sandbox.spy(BEM.blocks['b-confirm'], 'open');

            block.findBlockInside({ block: 'b-callouts-selector-item', modName: 'type', modVal: 'available' })
                .findElem('delete-selected-item').click();

            sandbox.clock.tick(100);

            expect(BEM.blocks['b-confirm'].open.called).to.be.equal(true);
            BEM.blocks['b-confirm']._buttonNo.trigger('click');

            BEM.blocks['b-confirm'].open.restore();
        });

        it('При удалении из списка доступных при подтверждении уточнение удаляется из списка в модели', function() {
            sandbox.stub(BEM.blocks['b-confirm'], 'open').callsFake(function(options) {
                options.onYes.apply(block);
            });
            sandbox.server.respondWith("POST",
                "/registered/main.pl",
                [200, {"Content-Type":"application/json"}, JSON.stringify(
                    {
                        "requestId": 1,
                        success: true
                    })
                ]);

            block.findBlockInside({ block: 'b-callouts-selector-item', modName: 'type', modVal: 'available' })
                .findElem('delete-selected-item').click();

            sandbox.clock.tick(100);
            sandbox.server.respond();

            expect(getCalloutById('81408')).to.equal(undefined);

            BEM.blocks['b-confirm'].open.restore();
        });

        it('При нажатии на ссылку "Очистить все" все модели становятся невыбранными', function() {
            block.findBlockOn('clear-all', 'link').trigger('click');

            sandbox.clock.tick(100);

            var isAllUnselected = block.val().callouts.length == 0;

            expect(isAllUnselected).to.equal(true);
        });

        describe('При добавлении нового уточнения', function() {
            it('Если такое уточнение уже есть, оно становится выбранным', function() {
                addNewCallout('Описание');

                sandbox.clock.tick(100);

                expect(getCalloutById('81409').get('selected')).to.equal(true);
            });

            it ('Если проверка успешно пройдена, уточенение добавляется в модель', function() {
                sandbox.server.respondWith("POST",
                    "/registered/main.pl",
                    [200, {"Content-Type":"application/json"}, JSON.stringify(
                        {
                            "requestId": 1,
                            success: true,
                            callouts: [{
                                "status_moderate": "Yes",
                                "additions_item_id": "99999",
                                "callout_text": "Чупакабра"
                            }]
                        })
                    ]);

                addNewCallout('Чупакабра');
                sandbox.server.respond();

                sandbox.clock.tick(100);

                expect(block._model.getCalloutByText('Чупакабра')).not.to.equal(undefined);
            });

            it ('Если проверка не пройдена, уточенение не добавляется в модель', function() {
                sandbox.server.respondWith("POST",
                    "/registered/main.pl",
                    [405, {"Content-Type":"application/json"}, JSON.stringify(
                        {
                            "requestId": 1,
                            success: false
                        })
                    ]);

                sandbox.spy(BEM.blocks['b-confirm'], 'open');
                addNewCallout('Кукарача');
                sandbox.server.respond();

                sandbox.clock.tick(100);
                expect(BEM.blocks['b-confirm'].open.called).to.be.equal(true);
                BEM.blocks['b-confirm']._buttonNo.trigger('click');
                BEM.blocks['b-confirm'].open.restore();
            });
        });
    });

    describe('Отображение модели в HTML-представление', function() {
        beforeEach(function() {
            bChooser = block.findBlockOn('available-items', 'b-chooser');
            sandbox.server.respondWith("GET",
                "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                [200, {"Content-Type":"application/json"}, JSON.stringify(
                    {
                        "requestId": 1,
                        success: true,
                        callouts: [
                            {
                                "callout_text": "Цена",
                                "additions_item_id": "81408",
                                "status_moderate": "Yes"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81409",
                                "callout_text": "Описание"
                            },
                            {
                                "status_moderate": "Yes",
                                "additions_item_id": "81415",
                                "callout_text": "Доставка"
                            }
                        ]
                    })
                ]);
            block.reset({ callouts: [
                {
                    "callout_text": "Цена",
                    "additions_item_id": "81408",
                    "status_moderate": "Yes"
                }
            ]});

            sandbox.clock.tick(100);
            sandbox.server.respond();
            sandbox.clock.tick(100);
        });

        ['available', 'selected'].forEach(function(list) {
            it('Все элементы списка list (' + list + ') находятся в ' + (list == 'available' ? 'правом' : 'левом') + ' списке и только они', function() {
                var ids = list == 'available' ? ['81408', '81409', '81415'] : ['81408'],
                    allIdsInList = true,
                    onlyIdsInList = true;

                ids.forEach(function(id) {
                    allIdsInList = allIdsInList && (!!block._getCalloutItemBlock(list + '-items-list', id));
                });

                block.findBlocksInside({ block: 'b-callouts-selector-item', modName: 'type', modVal: list }).forEach(function(elem) {
                    var id = elem.getMod('id');
                    onlyIdsInList = onlyIdsInList && ids.indexOf(id) >= 0;
                });

                expect(allIdsInList).to.equal(true);
                expect(onlyIdsInList).to.equal(true);
            });
        });

        describe('События при выборе/снятии выбора уточнений', function() {
            var calloutId = '81409';

            it('При выборе уточнения отмечается элемент в списке доступных', function() {
                block._model.setCalloutSelection(true, calloutId);

                var elem = bChooser.elem('item', 'name', calloutId);
                expect(bChooser.getMod(elem, 'selected')).to.equal('yes');
            });

            it('При выборе уточнения элемент добавляется в список выбранных', function() {
                block._model.setCalloutSelection(true, calloutId);

                expect(block._getCalloutItemBlock('selected-items-list', calloutId)).not.to.equal(null);
            });

            it('При снятии выбора уточнения разотмечается элемент в списке доступных', function() {
                block._model.setCalloutSelection(false, calloutId);

                var elem = bChooser.elem('item', 'name', calloutId);
                expect(bChooser.getMod(elem, 'selected')).to.equal('');
            });

            it('При снятии выбора уточнения элемент убирается элемент из списка выбранных', function() {
                block._model.setCalloutSelection(false, calloutId);

                expect(block._getCalloutItemBlock('selected-items-list', calloutId)).to.equal(null);
            });
        });

        describe('События на добавление нового уточнения', function() {
            var calloutId = '99099';
            beforeEach(function() {
                block._inputSearchAdd.val("Страус Эму");
                block._model.addNewCallout({
                    "status_moderate": "Yes",
                    "additions_item_id": calloutId,
                    "callout_text": "Страус Эму"
                });
            });

            it('При добавлении нового уточнения он появляется в списке доступных', function() {
                expect(bChooser.elem('item', 'name', calloutId).length).not.to.equal(0);
            });

            it('При добавлении нового уточнения он появляется в списке добавленных с галочкой', function() {
                var elem = bChooser.elem('item', 'name', calloutId);
                expect(bChooser.getMod(elem, 'selected')).to.equal('yes');
            });

            it('При добавлении нового уточнения он появляется в списке выбранных', function() {
                expect(block._getCalloutItemBlock('selected-items-list', calloutId)).not.to.equal(null);
            });

            it('При добавлении нового уточнения, если их меньше максимума, кнопка "добавить" не дизейблится', function() {
                expect(block._buttonAddNew).not.to.haveMod('disabled');
            });

            it('При добавлении нового уточнения, если их больше максимума, кнопка "добавить" дизейблится', function() {
                var longStr = (new Array(u.consts('MAX_CALLOUT_LENGTH')+2)).join('x');
                block._inputSearchAdd.val(longStr);
                block._model.addNewCallout({
                    "status_moderate": "Yes",
                    "additions_item_id": "99899",
                    "callout_text": longStr
                });
                expect(block._buttonAddNew).to.haveMod('disabled', 'yes');
            });
        });

        describe('События на удаление уточнения', function() {
            var calloutId = '81415';
            it('При удалении уточнения из модели оно удаляется из списка доступных в интерфейсе', function() {
                var length = bChooser.elem('item').length;
                block._model.deleteCalloutById(calloutId);

                sandbox.clock.tick(100);

                expect(bChooser.elem('item').length).to.equal(length-1);
            });

            it('При удалении уточнения из модели оно удаляется из списка выбранных в интерфейсе', function() {
                getCalloutById(calloutId).set('selected', true);

                block._model.deleteCalloutById(calloutId);

                expect(block._getCalloutItemBlock('selected-items-list', calloutId)).to.equal(null);
            });

            it('При удалении уточнения из модели, если нет текста в строке поиска, кнпока "Добавить" дизейблится', function() {
                block._model.deleteCalloutById(calloutId);

                expect(block._buttonAddNew).to.haveMod('disabled', 'yes');
            });

            it('При удалении уточнения из модели, если есть текст в строке поиска и он короче максимально допустимого, кнпока "Добавить" энейблится', function() {
                var shortStr = (new Array(u.consts('MAX_CALLOUT_LENGTH')-5)).join('x');

                block._inputSearchAdd.val(shortStr);
                block._model.deleteCalloutById(calloutId);

                expect(block._buttonAddNew).not.to.haveMod('disabled');
            });

            it('При удалении уточнения из модели, если есть текст в строке поиска и он длиннее максимально допустимого, кнпока "Добавить" дизейблится', function() {
                var longStr = (new Array(u.consts('MAX_CALLOUT_LENGTH')+5)).join('x');

                block._inputSearchAdd.val(longStr);
                block._model.deleteCalloutById(calloutId);

                expect(block._buttonAddNew).to.haveMod('disabled', 'yes');
            });
        });

        describe('Обновление списка досупных и выбранных уточнений', function() {
            it('При отсутствии доступных уточнений появляется сообщиение "Нет ни одного уточнения"', function() {
                sandbox.server.respondWith("GET",
                    "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                    [200, {"Content-Type":"application/json"}, JSON.stringify(
                        {
                            "requestId": 1,
                            success: true,
                            callouts: []
                        })
                    ]);
                block.reset({ callouts: []});

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                sandbox.clock.tick(100);

                expect(block.elem('selection-status').text()).to.equal('Нет ни одного уточнения');
            });

            it('При наличии доступных уточнений и отсутствии выбранных появляется сообщиение "Ничего не выбрано"', function() {
                sandbox.server.respondWith("GET",
                    "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                    [200, {"Content-Type":"application/json"}, JSON.stringify(
                        {
                            "requestId": 1,
                            success: true,
                            callouts: [
                                {
                                    "callout_text": "Цена",
                                    "additions_item_id": "81408",
                                    "status_moderate": "Yes"
                                },
                                {
                                    "status_moderate": "Yes",
                                    "additions_item_id": "81409",
                                    "callout_text": "Описание"
                                },
                                {
                                    "status_moderate": "Yes",
                                    "additions_item_id": "81415",
                                    "callout_text": "Доставка"
                                }
                            ]
                        })
                    ]);
                block.reset({ callouts: []});

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                expect(block.elem('selection-status').text()).to.equal('Ничего не выбрано');
            });

            it('При наличии доступных уточнений и наличии выбранных не появляется сообщиение', function() {
                sandbox.server.respondWith("GET",
                    "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                    [200, {"Content-Type":"application/json"}, JSON.stringify(
                        {
                            "requestId": 1,
                            success: true,
                            callouts: [
                                {
                                    "callout_text": "Цена",
                                    "additions_item_id": "81408",
                                    "status_moderate": "Yes"
                                },
                                {
                                    "status_moderate": "Yes",
                                    "additions_item_id": "81409",
                                    "callout_text": "Описание"
                                },
                                {
                                    "status_moderate": "Yes",
                                    "additions_item_id": "81415",
                                    "callout_text": "Доставка"
                                }
                            ]
                        })
                    ]);
                block.reset({ callouts: [
                    {
                        "callout_text": "Цена",
                        "additions_item_id": "81408",
                        "status_moderate": "Yes"
                    }
                ]});

                sandbox.clock.tick(100);
                sandbox.server.respond();
                sandbox.clock.tick(100);

                expect(block.elem('selection-status').text()).to.equal('');
            });

            describe('Для мультиредактирования:', function() {
                beforeEach(function() {
                    block = u.createBlock(
                        {
                            block: 'b-callouts-selector',
                            mods: { mode: 'multi' }
                        }
                    );

                    sandbox.server.respondWith("GET",
                        "/registered/main.pl?cmd=getBannersAdditions&additions_type=callout&limit=10000&offset=0",
                        [200, {"Content-Type":"application/json"}, JSON.stringify(
                            {
                                "requestId": 1,
                                success: true,
                                callouts: [
                                    {
                                        "callout_text": "Цена",
                                        "additions_item_id": "81408",
                                        "status_moderate": "Yes"
                                    },
                                    {
                                        "status_moderate": "Yes",
                                        "additions_item_id": "81409",
                                        "callout_text": "Описание"
                                    },
                                    {
                                        "status_moderate": "Yes",
                                        "additions_item_id": "81415",
                                        "callout_text": "Доставка"
                                    }
                                ]
                            })
                        ]);

                    block.reset({ calloutsKits: [
                        [{
                            "status_moderate": "Yes",
                            "additions_item_id": "81415",
                            "callout_text": "Доставка"
                        }]
                    ] });

                    sandbox.clock.tick(100);
                    sandbox.server.respond();
                    sandbox.clock.tick(100);
                });

                [
                    {
                        count: 1,
                        text: 'Ещё 1 уточнение не совпадает'
                    },
                    {
                        count: 2,
                        text: 'Ещё 2 уточнения не совпадает'
                    },
                    {
                        count: 6,
                        text: 'Ещё 6 уточнений не совпадает'
                    }
                ].forEach(function(test) {
                    it('Если в моделях отличаются ' + test.count + ' уточнений, и есть выбранные, появляется соответствующее сообщение', function() {
                        block._model.set('diffCount', test.count);

                        sandbox.clock.tick(100);

                        expect(block.elem('selection-status').text()).to.equal(test.text);
                    })
                });

                it('Если в моделях отличаются уточнения, и нет выбранных, появляется сообщение, что все уточнения разные', function() {
                    block._model.set('diffCount', 5);
                    block._model.set('haveSelected', false);

                    sandbox.clock.tick(100);

                    expect(block.elem('selection-status').text()).to.equal('В выбранных объявлениях разные уточнения');
                });
            });
        });

        it('При обновлении счетчика длины текста выбранных обновляется текст в соответствующем поле', function() {
            var length = 15;

            block._model.set('selectedTextLength', length);

            expect(+block.elem('selected-items-total-length').text()).to.equal(u.consts('MAX_CALLOUT_LENGTH_ON_BANNER') - length);
        });

    });

    describe('Валидация модели', function() {
        beforeEach(function() {
            constsStub.withArgs('MAX_CALLOUTS_COUNT_ON_BANNER').returns(50);
            block._model
                .set('list', block._getAvaliableItems([{
                        "callout_text": "Цена",
                        "additions_item_id": "81408",
                        "status_moderate": "Yes"
                    },
                    {
                        "status_moderate": "Yes",
                        "additions_item_id": "81409",
                        "callout_text": "Описание"
                    },
                    {
                        "status_moderate": "Yes",
                        "additions_item_id": "81415",
                        "callout_text": "Доставка"
                    }], ['81408']))
                .set('inited', true)
                .fix();
        });

        it('Если уточнений меньше ' + u.consts('MAX_CALLOUTS_COUNT_ON_BANNER') + ', валидация проходит', function() {
            expect(block._model.validate('selectedIds').valid).to.equal(true);
        });

        it('Если уточнений больше ' + u.consts('MAX_CALLOUTS_COUNT_ON_BANNER') + ', валидация не проходит', function() {
            for (var i = 0; i < u.consts('MAX_CALLOUTS_COUNT_ON_BANNER'); i++) {
                var id = '914' + i;
                block._model.addNewCallout({
                    "status_moderate": "Yes",
                    "additions_item_id": id,
                    "callout_text": i
                });
            }

            var res = block._model.validate('selectedIds');
            expect(res.errors && res.errors.length > 0).to.equal(true);
        });
    });
});
