describe('b-edit-phrase-price-stat-popup', function() {

    var sandbox,
        block,
        phraseModel,
        createBlock = function(phraseModelData, mods) {
            var newBlock;
            phraseModel = BEM.MODEL.create('m-stat-phrase-bidable', phraseModelData);
            newBlock = u.getInitedBlock({
                block: 'b-edit-phrase-price-stat-popup',
                mods: mods || {},
                js: {
                    currency: 'RUB',
                    campaign: {
                        type: 'text',
                        id: '1'
                    }
                },
                price: phraseModelData['price'],
                price_context: phraseModelData['price_context']
            });
            newBlock.initModels(phraseModel);

            return newBlock;
        },
        createBlockGlobal = function(phraseModelData, mods) {
            block = createBlock(phraseModelData, mods);
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
        block.destruct();
        sandbox.restore();
    });

    describe('Содержание блока в зависимости от входных данных', function() {

        it('Должен отобразить две цены: 100.00 / 100.00', function() {
            createBlockGlobal({ price: '100', price_context: '100' });

            expect(block.findBlockInside('switcher', 'button').elem('text').text()).to.be.eq('100.00 / 100.00');
        });

        it('Должен отобразить две цены, но не длинее 16 символов: XXXXX.XX / XXXXXX.XX -> XXXXX.XX / XXXXX…', function() {
            createBlockGlobal({ price: '10000.99', price_context: '100000.99' });

            expect(block.findBlockInside('switcher', 'button').elem('text').text()).to.be.eq('10000.99 / ...');
        });

        it('Должен отобразить кнопку с текстом Выставить, если мод multiedit_yes', function() {
            createBlockGlobal({ price: '10000.99', price_context: '100000.99' }, { multiedit: 'yes' });

            expect(block.findBlockInside('switcher', 'button').elem('text').text()).to.be.eq('Выставить');
        });

    });

    describe('Поведение блока без модификатора multiedit', function() {

        var defaultPhraseModelData;

        describe('Общее поведение', function() {

            beforeEach(function() {
                defaultPhraseModelData = {
                    "adgroup_id": "522216570",
                    "cid": "10925739",
                    "norm_phrase": "!в напряжение стабилизатор",
                    "norm_phrase_unquoted": "!в напряжение стабилизатор",
                    "phrase": "!в напряжение стабилизатор",
                    "src_phrase": "!в напряжение стабилизатор",
                    "src_type": "ext_phrase",
                    "currency": "RUB",
                    "has_price": true,
                    "has_price_context": true,
                    "status": "",
                    "price": 224.9,
                    "price_context": 0.3
                };

                createBlockGlobal(defaultPhraseModelData);
            });

            it('Должен открыть попап при нажатии на кнопку', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect(BEM.blocks['b-edit-phrase-price-stat-popup']._popup.isShown()).to.be.true;
            });

            it('Должен стриггерить событие show при открытии попапа', function() {

                expect(block).to.triggerEvent('show', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);
                });
            });

            it('Кнопка Применить должна быть disabled при открытии попапа', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect(block._saveButton).to.haveMod('disabled', 'yes');
            });

            it('Кнопка Применить должна быть активна, если модель изменилась и цена корректная', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);
                phraseModel.set('price', 150);

                expect(block._saveButton).to.not.haveMod('disabled', 'yes');
            });

            it('Кнопка Применить должна быть не активна, если модель изменилась и цена некорректная', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);
                phraseModel.set('price', 0);

                expect(block._saveButton).to.haveMod('disabled', 'yes');
            });

            it('Должен закрыть попап при нажатии на Отменить', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);
                block._cancelButton.domElem.click();
                sandbox.clock.tick(500);

                expect(BEM.blocks['b-edit-phrase-price-stat-popup']._popup.isShown()).to.be.false;
            });

            it('Должен стригерить событие hide при нажатии на Отменить', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect(block).to.triggerEvent('hide', function() {
                    block._cancelButton.domElem.click();
                    sandbox.clock.tick(500);
                });
            });

            it('Должен обновить содержимое кнопки', function() {
                block.updateButtonText(1, 1);

                expect(block.findBlockInside('switcher', 'button').elem('text').text()).to.be.eq('1.00 / 1.00');
            });

            it('Должен обновить содержимое кнопки c учетом ограничения', function() {
                block.updateButtonText(10000.99, 100000.99);

                expect(block.findBlockInside('switcher', 'button').elem('text').text()).to.be.eq('10000.99 / ...');
            });

            it('Должен вернуть кнопку', function() {
                var switcher = block.getSwitcher();

                expect(switcher.domElem.length).to.be.eq(1);
            });

            it('Два блока должны использовать один и тот же popup', function() {
                var block2 = createBlock(defaultPhraseModelData),
                    popup1,
                    popup2;

                block.elem('switcher').click();
                sandbox.clock.tick(500);
                popup1 = block.__self._getPopup();

                block2.elem('switcher').click();
                sandbox.clock.tick(500);
                popup2 = block2.__self._getPopup();

                expect(popup1 == popup2).to.be.true;
            });

        });

        describe('С выставлением цены только на поиске', function() {

            beforeEach(function() {
                defaultPhraseModelData = {
                    "adgroup_id": "522216570",
                    "cid": "10925739",
                    "norm_phrase": "!в напряжение стабилизатор",
                    "norm_phrase_unquoted": "!в напряжение стабилизатор",
                    "phrase": "!в напряжение стабилизатор",
                    "src_phrase": "!в напряжение стабилизатор",
                    "src_type": "ext_phrase",
                    "currency": "RUB",
                    "has_price": true,
                    "has_price_context": false,
                    "status": "",
                    "price": 224.9
                };
            });

            describe('Содержимое попапа', function() {

                beforeEach(function() {
                    createBlockGlobal(defaultPhraseModelData);
                });

                it('Попап содержит один элемент для настройки цены', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__price-wrap').length).to.be.eq(1);
                });

                it('Попап содержит текст "На поиске"', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__platform').text()).to.be.eq('На поиске');
                });

                it('Попап содержит блок b-edit-phrase-price_control-type_search', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price_control-type_search').length).to.be.eq(1);
                });

            });

            it('Не содержит модификатор highlight-border_red если цена валидна', function() {
                defaultPhraseModelData['price'] = 100;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.not.haveMod('highlight-border');

            });

            it('Содержит модификатор highlight-border_red если цена невалидна', function() {
                defaultPhraseModelData['price'] = 0;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'red');
            });

            it('Содержит модификатор highlight-border_pink если цена высока', function() {
                defaultPhraseModelData['price'] = 1000;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'pink');
            });

        });

        describe('С выставлением цены только в сетях', function() {

            beforeEach(function() {
                defaultPhraseModelData = {
                    "adgroup_id": "522216570",
                    "cid": "10925739",
                    "norm_phrase": "!в напряжение стабилизатор",
                    "norm_phrase_unquoted": "!в напряжение стабилизатор",
                    "phrase": "!в напряжение стабилизатор",
                    "src_phrase": "!в напряжение стабилизатор",
                    "src_type": "ext_phrase",
                    "currency": "RUB",
                    "has_price": false,
                    "has_price_context": true,
                    "status": "",
                    "price_context": 0.3
                };
            });

            describe('Содержимое попапа', function() {

                beforeEach(function() {
                    createBlockGlobal(defaultPhraseModelData);
                });

                it('Попап содержит один элемент для настройки цены', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__price-wrap').length).to.be.eq(1);
                });

                it('Попап содержит текст "В сетях"', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__platform').text()).to.be.eq('В сетях');
                });

                it('Попап содержит блок b-edit-phrase-price_control-type_context', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price_control-type_context').length).to.be.eq(1);
                });

            });

            it('Не содержит модификатор highlight-border_red если цена валидна', function() {
                defaultPhraseModelData['price_context'] = 100;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.not.haveMod('highlight-border');
            });

            it('Содержит модификатор highlight-border_red если цена невалидна', function() {
                defaultPhraseModelData['price_context'] = 0;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'red');
            });

            it('Содержит модификатор highlight-border_pink если цена высока', function() {
                defaultPhraseModelData['price_context'] = 1000;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'pink');
            });

        });

        describe('С выставлением цены в сетях и на поиске', function() {

            beforeEach(function() {
                defaultPhraseModelData = {
                    "adgroup_id": "522216570",
                    "cid": "10925739",
                    "norm_phrase": "!в напряжение стабилизатор",
                    "norm_phrase_unquoted": "!в напряжение стабилизатор",
                    "phrase": "!в напряжение стабилизатор",
                    "src_phrase": "!в напряжение стабилизатор",
                    "src_type": "ext_phrase",
                    "currency": "RUB",
                    "has_price": true,
                    "has_price_context": true,
                    "status": "",
                    "price": 224.9,
                    "price_context": 0.3
                };
            });

            describe('Содержимое попапа', function() {

                beforeEach(function() {
                    createBlockGlobal(defaultPhraseModelData);
                });

                it('Попап содержит два элемента для настройки цены', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__price-wrap').length).to.be.eq(2);
                });

                it('Попап содержит текст "На поиске"', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__platform').eq(0).text()).to.eq('На поиске');
                });

                it('Попап содержит текст "В сетях"', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price-stat-popup__platform').eq(1).text()).to.eq('В сетях');
                });

                it('Попап содержит блок b-edit-phrase-price_control-type_search', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price_control-type_search').length).to.be.eq(1);
                });

                it('Попап содержит блок b-edit-phrase-price_control-type_context', function() {
                    block.elem('switcher').click();
                    sandbox.clock.tick(500);

                    expect($('.b-edit-phrase-price_control-type_context').length).to.be.eq(1);
                });

            });

            it('Не содержит модификатор highlight-border_red если цена валидна', function() {
                defaultPhraseModelData['price_context'] = 100;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.not.haveMod('highlight-border');
            });

            it('Содержит модификатор highlight-border_red если цена невалидна', function() {
                defaultPhraseModelData['price_context'] = 0;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'red');
            });

            it('Содержит модификатор highlight-border_pink если цена высока', function() {
                defaultPhraseModelData['price'] = 1000;
                createBlockGlobal(defaultPhraseModelData);
                block.validate();

                expect(block.findBlockInside('switcher', 'button')).to.haveMod('highlight-border', 'pink');
            });

        });

    });

    describe('Поведение блока с модификатором multiedit_yes', function() {

        var defaultPhraseModelData;

        describe('Содержимое попапа', function() {

            beforeEach(function() {
                defaultPhraseModelData = {
                    "adgroup_id": "522216570",
                    "cid": "10925739",
                    "norm_phrase": "!в напряжение стабилизатор",
                    "norm_phrase_unquoted": "!в напряжение стабилизатор",
                    "phrase": "!в напряжение стабилизатор",
                    "src_phrase": "!в напряжение стабилизатор",
                    "src_type": "ext_phrase",
                    "currency": "RUB",
                    "has_price": true,
                    "has_price_context": true,
                    "status": "",
                    "price": 224.9,
                    "price_context": 0.3
                };
                createBlockGlobal(defaultPhraseModelData, { multiedit: 'yes' });
            });

            it('Попап содержит один элемент для настройки цены', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect($('.b-edit-phrase-price-stat-popup__price-wrap').length).to.be.eq(1);
            });

            it('Попап содержит текст "Единая ставка"', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect($('.b-edit-phrase-price-stat-popup__platform').text()).to.be.eq('Единая ставка');
            });

            it('Попап содержит блок b-edit-phrase-price_multiedit_yes', function() {
                block.elem('switcher').click();
                sandbox.clock.tick(500);

                expect($('.b-edit-phrase-price_multiedit_yes').length).to.be.eq(1);
            });

        });
    });
});
