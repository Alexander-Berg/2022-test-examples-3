describe('b-time-targeting', function() {
    var block,
        sandbox,
        defaultData = {
            timeZoneGroups: JSON.parse('[{"name":"Россия","timezones":[{"name":"Калининград (MSK -01:00)","group_nick":"russia","msk_offset":"-01:00","offset_str":"(MSK -01:00)","gmt_offset":"+02:00","timezone":"Europe/Kaliningrad","id":"131","country_id":"225","offset":7200,"timezone_id":"131"}]}]'),
            isExtendModeAvailable: false,
            value: {
                isExtendModeOn: false,
                timeZone: { id: 131, text: 'Калининград' },
                timeTargetCode: '1IJKLMNOPQRST2IJKLMNOPQRST3IJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST',
                isHolidaySettingsEnabled: false,
                isWorkingWeekendEnabled: true,
                holidayShowSettings: {
                    isShowing: true,
                    showingFrom: 8,
                    showingTo: 20,
                    coefficient: 100
                }
            }
        };

    function getBlock(value) {
        return u.getInitedBlock({
            block: 'b-time-targeting',
            params: u.deepExtend(defaultData, value || {})
        });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        block && block.domElem && BEM.DOM.destruct(block.domElem);
        sandbox.restore();
    });

    describe('Реакция на события модели', function() {
        beforeEach(function() {
            block = getBlock();
        });

        it('При событии error на модели должен отрисоваться блок с ошибками', function() {
            block.model.trigger('error', {
                errors: [
                    { text: 'error1' },
                    { text: 'error2' }
                ]
            });
            sandbox.clock.tick(5);
            expect(block.elem('errors').html()).to.be.equal('error1<br>error2');
        });

        it('При изменении поля модели timeTargetTable должно измениться состояние чекбокса с днем недели', function() {
            var scaleBoard = block.findBlockInside('b-time-targeting-scale-board');

            scaleBoard.setCheckbox = sandbox.spy(function() {
                return scaleBoard;
            });
            block.model.get('timeTargetTable').setCell(5, 'H', 100);

            sandbox.clock.tick(5);
            expect(scaleBoard.setCheckbox.getCall(0).args[1]).to.equal('H');
        });

        it('При изменении поля модели timeTargetTable должно измениться состояние чекбокса с часом', function() {
            var scaleBoard = block.findBlockInside('b-time-targeting-scale-board');

            scaleBoard.setCheckbox = sandbox.spy(function() {
                return scaleBoard;
            });
            block.model.get('timeTargetTable').setCell(5, 'H', 100);

            sandbox.clock.tick(5);
            expect(scaleBoard.setCheckbox.getCall(1).args[1]).to.equal(5);
        });

        it('При изменении поля модели totalHours должно очиститься поле с ошибками', function() {
            block.elem('errors').html('1234');
            block.model.set('totalHours', 120);
            sandbox.clock.tick(5);
            expect(block.elem('errors').html()).to.be.equal('');
        });
    });

    describe('Если изначально расширенный режим', function() {
        describe('достпуен', function() {
            describe('и включен', function() {
                beforeEach(function() {
                    block = getBlock({
                        isExtendModeAvailable: true,
                        value: { isExtendModeOn: true }
                    });
                });

                it('То тумблер должен быть виден', function() {
                    sandbox.clock.tick(50);
                    expect(block.findBlockInside('tumbler').domElem.height()).to.not.equal(0);
                });

                it('Расширенный режим  включен', function() {
                    sandbox.clock.tick(50);
                    expect(block.getValue().isExtendModeOn).to.be.true;
                });

                describe('при деактивации расширенного режима', function() {
                    beforeEach(function() {
                        sandbox.clock.tick(5);
                        block.setExtendModeAvailable(false);
                        sandbox.clock.tick(5);
                    });

                    it('тумблер должен быть не виден', function() {
                        expect(block.findBlockInside('tumbler').domElem.height()).to.equal(0);
                    });

                    it('Расширенный режим быть выключен', function() {
                        expect(block.getValue().isExtendModeOn).to.be.false;
                    });
                });
            });

            describe('и выключен', function() {
                beforeEach(function() {
                    block = getBlock({
                        isExtendModeAvailable: true,
                        value: { isExtendModeOn: false }
                    });
                });

                it('То тумблер должен быть виден', function() {
                    sandbox.clock.tick(50);
                    expect(block.findBlockInside('tumbler').domElem.height()).to.not.equal(0);
                });

                it('Расширенный режим выключен', function() {
                    sandbox.clock.tick(50);
                    expect(block.getValue().isExtendModeOn).to.be.false;
                });

            });

        });

        describe('не достпуен', function() {
            describe('и включен', function() {
                beforeEach(function() {
                    block = getBlock({
                        isExtendModeAvailable: false,
                        value: { isExtendModeOn: true }
                    });
                });

                it('То тумблер должен быть не виден', function() {
                    sandbox.clock.tick(50);
                    expect(block.findBlockInside('tumbler').domElem.height()).to.equal(0);
                });

                it('Расширенный режим выключен', function() {
                    sandbox.clock.tick(50);
                    expect(block.getValue().isExtendModeOn).to.be.false;
                });

                describe('при активации расширенного режима', function() {
                    beforeEach(function() {
                        sandbox.clock.tick(5);
                        block.setExtendModeAvailable(true);
                        sandbox.clock.tick(5);
                    });

                    it('тумблер должен быть виден', function() {
                        expect(block.findBlockInside('tumbler').domElem.height()).to.not.equal(0);
                    });

                    it('Расширенный режим быть включен', function() {
                        expect(block.getValue().isExtendModeOn).to.be.true;
                    });
                });
            });

            describe('и выключен', function() {
                beforeEach(function() {
                    block = getBlock({
                        isExtendModeAvailable: true,
                        value: { isExtendModeOn: false }
                    });
                });

                it('То тумблер должен быть не виден', function() {
                    sandbox.clock.tick(50);
                    expect(block.findBlockInside('tumbler').domElem.height()).to.not.equal(0);
                });

                it('Расширенный режим выключен', function() {
                    sandbox.clock.tick(50);
                    expect(block.getValue().isExtendModeOn).to.be.false;
                });

            });

        });

    });

    describe('__prepare-data', function() {
        function prepareSourceData(name, value) {
            var addValue = {},
                defaultValue = defaultData.value,
                sourceData;

            if(name.indexOf('.') > 0) {
                var path = name.split('.'),
                    obj = {};
                for (var i = path.length - 1; i >= 0; i--) {
                    if (i == path.length - 1) {
                        obj[path[i]] = value;
                    } else {
                        var newObj = {};
                        newObj[path[i]] = obj;
                        obj = newObj;
                    }
                }
                addValue = obj;
            } else {
                addValue[name] = value;
            }

            value = u.deepExtend(defaultValue, addValue);
            return u.deepExtend(defaultData, { value: value });
        }

        it('Возвращает правильный набор данных', function() {
            var data = BEMHTML.apply({
                block: 'b-time-targeting',
                elem: 'prepare-data',
                params: defaultData
            }),
                dataKeys = Object.keys(data);

            expect(dataKeys).to.deep.equal(["timeTargetCode","timeTargetMode","timezoneId","timezoneText",
                "timeTargetPreset","isExtendedMode","intoAccountWeekend","intoAccountHolidays",
                "showWorkingWeekendCheckbox","dontShowOnHolidays","holidaysFrom","holidaysTo",
                "holidaysTimeTargetLevel","timezoneRegionId","timeTargetTable","totalHours",
                "dayElems","hoursControls","_timeTargetingModelData","timezoneRegionsData","timezoneData"]);
        });

        [
            {
                sourceName: 'timeTargetCode',
                resultName: 'timeTargetCode',
                realValue: '1IJKLMNOPQRST2IJKLMNOPQRST3IJKLMNOPQRST4IJKLMNOPQRST5IJKLMNOPQRST',
                defaultValue: u['b-time-targeting'].CODE_ALL_TIME
            },
            {
                sourceName: 'preset',
                resultName: 'timeTargetPreset',
                realValue: 'worktime',
                defaultValue: 'other'
            },
            {
                sourceName: 'timeZone.id',
                resultName: 'timezoneId',
                realValue: [0, 104],
                defaultValue: 130
            },
            {
                sourceName: 'timeZone.text',
                resultName: 'timezoneText',
                realValue: 'Эфиопия',
                defaultValue: 'Москва'
            },
            {
                sourceName: 'isWorkingWeekendEnabled',
                resultName: 'intoAccountWeekend',
                realValue: [true, false],
                defaultValue: undefined
            },
            {
                sourceName: 'isHolidaySettingsEnabled',
                resultName: 'intoAccountHolidays',
                realValue: [true, false],
                defaultValue: undefined
            },
            {
                sourceName: 'showWorkingWeekendCheckbox',
                resultName: 'showWorkingWeekendCheckbox',
                realValue: [true, false],
                defaultValue: undefined
            },
            {
                sourceName: 'holidayShowSettings.isShowing',
                resultName: 'dontShowOnHolidays',
                realValue: [true, false],
                resultValue: [false, true],
                defaultValue: true
            },
            {
                sourceName: 'holidayShowSettings.showingFrom',
                resultName: 'holidaysFrom',
                realValue: [undefined, 0, 1, 2, 8, 12],
                resultValue: [8, 0, '1', '2', '8', '12'],
                defaultValue: 8
            },
            {
                sourceName: 'holidayShowSettings.showingTo',
                resultName: 'holidaysTo',
                realValue: [undefined, 0, 1, 2, 8, 22],
                resultValue: [20, 0, '1', '2', '8', '22'],
                defaultValue: 20
            },
            {
                sourceName: 'holidayShowSettings.coefficient',
                resultName: 'holidaysTimeTargetLevel',
                realValue: [undefined, 0, 10, 200],
                resultValue: [100, 100, 10, 200],
                defaultValue: 100
            }

        ].forEach(function(testset) {
            describe(testset.resultName, function() {
                var realValues = testset.realValue;

                if (!u._.isArray(testset.realValue)) {
                    realValues = [testset.realValue];
                }

                realValues.forEach(function(realValue, index) {
                    it('Если в поле ' + testset.sourceName + ' передан ' + realValue + ', то ' +
                        (!!testset.resultValue ? testset.resultValue[index] : realValue) + ' возвращается в поле ' + testset.resultName, function() {
                        var data = BEMHTML.apply({
                            block: 'b-time-targeting',
                            elem: 'prepare-data',
                            params: prepareSourceData(testset.sourceName, realValue)
                        });
                        expect(data[testset.resultName]).to.equal(!!testset.resultValue ? testset.resultValue[index] : realValue);
                    });
                });

                it('Если не передан ' + testset.sourceName + ', то  в поле ' + testset.resultName + ' возвращается дефолтное значение', function() {
                    var data = BEMHTML.apply({
                        block: 'b-time-targeting',
                        elem: 'prepare-data',
                        params: prepareSourceData(testset.sourceName, undefined)
                    });

                    expect(data[testset.resultName]).to.equal(testset.defaultValue);
                })
            })
        });

        [false, true].forEach(function(isExtendModeOn) {
            var addValue = { isExtendModeOn: isExtendModeOn },
                value = u.deepExtend(defaultData.value, addValue),
                sourceData = u.deepExtend(defaultData, { value: value }),
                data = BEMHTML.apply({
                    block: 'b-time-targeting',
                    elem: 'prepare-data',
                    params: sourceData
                });

            it('Если isExtendedModeOn ' + isExtendModeOn + ', то в поле timeTargetMode ' + (isExtendModeOn ? 'extend' : 'simple'), function() {
                expect(data.timeTargetMode).to.equal(isExtendModeOn ? 'extend' : 'simple');
            });

            it('Если isExtendedModeOn ' + isExtendModeOn + ', то в поле isExtendedMode ' + isExtendModeOn, function() {
                expect(data.isExtendedMode).to.equal(isExtendModeOn);
            });
        });
    });
});
