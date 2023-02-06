describe('b-adjustment-rates_type_demography', function() {

    var time,
        block,
        bemJson,
        demographyModel,
        commonModel,
        ratesStub = {
            rates: [
                {
                    sign: 'increment',
                    age: '0-17',
                    gender: 'female',
                    input: 2,
                    pct_max: 400,
                    pct_min: 100,
                    default_multiplier: 100
                },
                {
                    sign: 'decrement',
                    age: '18-24',
                    gender: 'male',
                    input: 12,
                    pct_max: 400,
                    pct_min: 100,
                    default_multiplier: 100
                }
            ]
        };

    /**
     * Создание инстанса блока
     * @param {Object} [extraParams] - доп. параметры которые будут переданы ctx
     * @param {String[]} [extraParams.value] - выбранные пункты
     */
    function createBlock(extraParams) {
        extraParams = extraParams || {};

        bemJson = u._.extend({
            block: 'b-adjustment-rates',
            mods: { type: 'demography' },
            js: { modelId: '123' },
            modelId: '123'
        }, extraParams);

        return BEM.DOM.init(u.getDOMTree(bemJson).appendTo('body')).bem('b-adjustment-rates');
    }

    /**
     * Устанавливает на input фокус, устанавливает значение и снимает фокус
     * @param {BEM} input
     * @param {String} value
     */
    function setInputValue(input, value) {
        input.elem('control').focus();
        input.val(value);
        input.elem('control').focusout();
    }

    /**
     * Создаем экземпляр модели с которой будет взаимодействовать блок
     */
    function createModel() {
        return BEM.MODEL.create({ name: 'm-adjustment-demography-rates', id: '123' }, {
            modelId: '123',
            isEnabled: true,
            canChangeSign: true,
            max_conditions: 10,
            common_control: {
                modelId: 'common-demography123',
                pct_max: 400,
                pct_min: 100,
                default_multiplier: 100
            },
            rates: []
        });
    }

    /**
     * Возвращает список контролов для ввода возраста
     * @returns {Array}
     */
    function getRatesAgeSelects() {
        return block.findBlocksInside(block.findElem('list', 'control', 'type', 'age'), 'select');
    }

    /**
     * Возвращает массив инпутов для процентов корректировки
     * @returns {Array}
     */
    function getRatesInputs() {
        return block.findBlocksInside(block.findElem('list', 'control', 'type', 'input'), 'input');
    }

    /**
     * Возвращает массив селектов для выбора знака
     * @returns {Array}
     */
    function getRatesSignSelects() {
        return block.findBlocksInside(block.findElem('list', 'control', 'type', 'sign'), 'select');
    }

    /**
     * Возвращает список радио-групп для выбора возраста
     * @returns {Array}
     */
    function getRatesGenderRadio() {
        return block.findBlocksInside(block.findElem('list', 'control', 'type', 'gender'), 'radio-button');
    }

    /**
     * Возвращает групповой инпут для ввода ограничений
     * @returns {BEM}
     */
    function getCommonInput() {
        return block.findBlockInside(block.elem('control', 'for', 'common-input'), 'input');
    }

    /**
     * Возвращает групповой селект для выбора знака ограничения
     * @returns {BEM}
     */
    function getCommonSignSelect() {
        return block.findBlockInside(block.elem('control', 'for', 'common-sign'), 'select');
    }

    before(function() {
        time = sinon.useFakeTimers();
        demographyModel = createModel();
        commonModel = demographyModel.get('common_control');
        block = createBlock({ modelData: demographyModel.toJSON() });
    });

    afterEach(function() {
        demographyModel.get('rates').clear();
    });

    after(function() {
        time.restore();
        commonModel.destruct();
        demographyModel.destruct();
        BEM.DOM.destruct(block.domElem);
    });

    describe('Отрисовка одной корректировки', function() {
        var commonInput,
            commonSignSelect;

        beforeEach(function() {
            demographyModel.get('rates').add(ratesStub.rates[0]);

            commonInput = getCommonInput();
            commonSignSelect = getCommonSignSelect();
        });

        it('Групповой инпут должен быть задизейблен', function() {
            expect(commonInput).to.haveMod('disabled');
        });

        it('Групповой селект должен быть задизейблен', function() {
            expect(commonSignSelect).to.haveMod('disabled');
        });

    });

    describe('Отрисовка двух корректировок:', function() {
        var commonControl,
            commonInput,
            commonSignSelect;

        beforeEach(function() {
            demographyModel.update(ratesStub);

            commonControl = block.elem('control', 'for', 'common-input');
            commonInput = getCommonInput();
            commonSignSelect = getCommonSignSelect();
        });

        it('должно быть две корректировки', function() {
            expect(block).to.haveElems('list-item', 2);
        });

        it('Общий инпут не должен быть задизейблен', function() {
            expect(commonInput).to.not.haveMod('disabled');
        });

        it('Общий селект не должен быть задизейблен', function() {
            expect(commonSignSelect).to.not.haveMod('disabled');
        });

        describe('Групповые операции', function() {
            it('должны изменяться корректировки при изменении общего инпута', function() {
                var ratesInputs = getRatesInputs();

                /**
                 * Инпуты в корректировках обновляются после того
                 * как убирается фокус с общего
                 */
                setInputValue(commonInput, '22');
                time.tick(0);

                expect(ratesInputs[0].val()).to.be.equal('22');
                expect(ratesInputs[1].val()).to.be.equal('22');
            });

            it('должны изменяться знаки корректировок при изменении знака общего знака', function() {
                var ratesSignsInputs = getRatesSignSelects();

                commonSignSelect.val('decrement');
                time.tick(0);

                expect(ratesSignsInputs[0].val()).to.be.equal('decrement');
                expect(ratesSignsInputs[1].val()).to.be.equal('decrement');
            });

            it('НЕ должны изменяться корректировки если в групповом инпуте ОШИБКА', function() {
                var ratesInputs = getRatesInputs(),
                    incorrectValue = 99999;

                /**
                 * Инпуты в корректировках обновляются после того
                 * как убирается фокус с общего
                 */
                setInputValue(commonInput, incorrectValue);
                time.tick(0);

                expect(ratesInputs[0].val()).to.be.equal('2');
                expect(ratesInputs[1].val()).to.be.equal('12');
            });

            it('должен очищаться общий инпут при изменении любой корректировки', function() {
                var ratesInputs = getRatesInputs();

                setInputValue(commonInput, '22');
                time.tick(0);

                ratesInputs[0].val('31');
                expect(commonInput.val()).to.be.equal('');
            });
        });

        describe('Ввод невалидных значений в инпуты', function() {
            it('должен быть модификатор error_yes на инпуте если значение невалидное', function() {
                var ratesInputs = getRatesInputs();

                ratesInputs[0].val('3100');
                expect(ratesInputs[0]).to.haveMod('error', 'yes');
            });

            it('должен быть модификатор error_yes на корректировке если значение невалидное', function() {
                var ratesInputs = getRatesInputs(),
                    listItems = block.findElem('list-item');

                ratesInputs[0].val('3100');
                time.tick(0);

                expect(block).to.haveElem('list-item', 'error', 'yes');
            });
        });

        describe('Ввод невалидных значений в групповые инпуты', function() {
            it('Если в групповой инпут ввели не число, он должен очиститься', function() {
                setInputValue(commonInput, 'Невалидное значение');
                time.tick(0);

                expect(commonInput.val()).to.be.equal('');
            });

            ['increment', 'decrement'].forEach(function(sign) {
                it('Если выбрано "ставка - ' + (sign == 'increment' ? 'увеличить' : 'уменьшить') + '", то при вводе в групповой инпут слишком большого числа показываем ошибку ', function() {
                    commonModel.set('sign', sign);

                    setInputValue(commonInput, 99999);

                    time.tick(0);
                    expect(commonInput).to.haveMod('error', 'yes');
                });
            });
        });
    });

    describe('Установка/удаление модификаторов:', function() {
        it('должен отсутствовать модификатор single-rate yes если корректировок больше одной', function() {
            demographyModel.update(ratesStub);

            expect(block).to.not.haveMod('single-rate', 'yes');
        });

        it('должен выставится модификатор single-rate yes если корректировка одна', function() {
            demographyModel.get('rates').add(ratesStub.rates[0]);

            expect(block).to.haveMod('single-rate', 'yes');
        });

        it('должен выставится модификатор status has-rates если есть корректировки', function() {
            demographyModel.update(ratesStub);

            expect(block).to.haveMod('status', 'has-rates');
        });

        it('должен выставится модификатор status empty если нет корректировок', function() {
            expect(block).to.haveMod('status', 'empty');
        });
    });

    describe('Взаимосвязь пол/возраст:', function() {
        beforeEach(function() {
            demographyModel.update(ratesStub);
        });

        it('должен быть задизейблен пол "все" в обеих корректировках при выборе возраста "любой"', function() {
            var ratesAges = getRatesAgeSelects();

            ratesAges[0].val('all');
            time.tick(0);

            expect($('.radio-button__control[value="all"]:disabled').length).to.be.equal(2);
        });

        it('должен раздизейблиться пол "все" в корректировке при удалении корректировки с возрастом "любой"', function() {
            var ratesAges = getRatesAgeSelects();

            ratesAges[0].val('all');
            time.tick(0);
            //используем findElem чтобы не допустить кэширование
            block.findElem('remove-rate').eq(0).trigger('click');
            time.tick(0);

            //раздизейбленно
            expect($('.radio-button__control[value="all"]:disabled').length).to.be.equal(0);
        });

        it('должен быть задизейблен возраст "любой" в обеих корректировках при выборе пол "все"', function() {
            var ratesGender = getRatesGenderRadio();

            ratesGender[0].val('all');

            expect($('.select__option[value="all"]:disabled').length).to.be.equal(2);
        });

        it('должен раздизейблиться возраст "любой" в корректировке при удалении корректировки с полом "все"', function() {
            var ratesGender = getRatesGenderRadio();

            ratesGender[0].val('all');
            time.tick(0);
            //используем findElem чтобы не допустить кэширование
            block.findElem('remove-rate').eq(0).trigger('click');
            time.tick(0);

            //раздизейбленно
            expect($('.select__option[value="all"]:disabled').length).to.be.equal(0);
        });
    });

    describe('Работа переключателя isEnabled:', function() {
        var enabledTumbler,
            controls = {
                gender: getRatesGenderRadio,
                sign: getRatesSignSelects,
                input: getRatesInputs,
                age: getRatesAgeSelects
            },
            common = {
                sign: getCommonSignSelect,
                input: getCommonInput
            };

        before(function() {
            enabledTumbler = block.findBlockInside('tumbler');
        });

        after(function() {
            enabledTumbler.destruct();
        });

        ['', 'yes'].forEach(function(modVal) {
            describe('Тумблер ' + (modVal == 'yes' ? '«вкл»' : '«выкл»'), function() {
                beforeEach(function() {
                    enabledTumbler.setMod('checked', 'yes');
                });

                ['gender', 'age', 'input', 'sign'].forEach(function(type) {
                    it('__control_type_' + type + (modVal == 'yes' ? ' активен' : ' неактивен'), function() {
                        demographyModel.get('rates').add(ratesStub.rates[0]);

                        //чтобы проверить кейсы для включенного тумблера, надо сначала его отключить
                        if (modVal == 'yes') {
                            //переключаем тумблер
                            enabledTumbler.delMod('checked');
                            time.tick(0);
                        }

                        enabledTumbler.setMod('checked', modVal ? 'yes' : '');
                        time.tick(0);

                        if (modVal) {
                            expect(controls[type]()[0]).to.not.haveMod('disabled');
                        } else {
                            expect(controls[type]()[0]).to.haveMod('disabled', 'yes');
                        }
                    });
                });

                ['input', 'sign'].forEach(function(type) {
                    it('C одной корректировкой common ' + type + ' должен быть неактивен', function() {
                        demographyModel.get('rates').add(ratesStub.rates[0]);

                        //чтобы проверить кейсы для включенного тумблера, надо сначала его отключить
                        if (modVal == 'yes') {
                            //переключаем тумблер
                            enabledTumbler.delMod('checked');
                            time.tick(0);
                        }
                        enabledTumbler.setMod('checked', modVal ? 'yes' : '');
                        time.tick(0);

                        expect(common[type]()).to.haveMod('disabled', 'yes');
                    });
                });

                ['input', 'sign'].forEach(function(type) {
                    it('C несколькими корректировками common ' + type + ' должен быть ' + (modVal == 'yes' ? ' активен' : ' неактивен'), function() {
                        demographyModel.update('rates', ratesStub);

                        if (modVal) {
                            expect(common[type]()).to.not.haveMod('disabled');
                        } else {
                            expect(common[type]()).to.haveMod('disabled', 'yes');
                        }

                    });
                });
            });
        });

        it('Кнопка "Все" не должна остаться задизейблена после включения тумблера', function() {
            demographyModel.update('rates', ratesStub);

            //переключаем тумблер
            enabledTumbler.delMod('checked');
            time.tick(0);
            enabledTumbler.setMod('checked', 'yes');
            time.tick(0);
            expect($('.select__option[value="all"]:disabled').length).to.be.equal(0);
        });

        it('Если тумблер включили, но установлен пол "любой", возраст "все" должен остаться задизейбленным', function() {
            demographyModel.get('rates').add(u._.extend(ratesStub.rates[0], { age: 'all' }));

            //переключаем тумблер
            enabledTumbler.delMod('checked');
            time.tick(0);

            enabledTumbler.setMod('checked', 'yes');
            time.tick(0);

            expect($('.radio-button__control[value="all"]:disabled').length).to.be.equal(1);
        });

        it('должен быть задизейблен возраст "любой" при выборе пол "все" и переключении тумблера вкл/выкл', function() {
            demographyModel.get('rates').add(u._.extend(ratesStub.rates[0], { gender: 'all' }));

            //переключаем тумблер
            enabledTumbler.delMod('checked');
            time.tick(0);

            enabledTumbler.setMod('checked', 'yes');
            time.tick(0);

            expect($('.select__option[value="all"]:disabled').length).to.be.equal(1);
        });

    });

    describe('Показ предупреждений об отключении условий', function() {
        it('При установке значения input: 100, sign: "decrement" должно показаться предупреждение об отключении условия', function() {
            demographyModel.get('rates').add(ratesStub.rates[0]);

            demographyModel.get('rates').getByIndex(0).update({ input: 100, sign: 'decrement' });
            time.tick(0);
            expect(block).to.haveElem('list-item', 'warning', 'yes');
        });
    });
});
