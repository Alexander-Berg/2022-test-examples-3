describe('b-time-targeting-decorator', function() {
    var ctx = {
        block: 'b-time-targeting-decorator',
            params: {
                "timeZoneGroups": [
                    {
                        "name": "Россия",
                        "timezones": [
                            {
                                "timezone_id": "131",
                                "id": "131",
                                "name": "Калининград (MSK -01:00)",
                                "offset": 7200,
                                "gmt_offset": "+02:00",
                                "group_nick": "russia",
                                "offset_str": "(MSK -01:00)",
                                "timezone": "Europe/Kaliningrad",
                                "msk_offset": "-01:00",
                                "country_id": "225"
                            },
                            {
                                "id": "130",
                                "name": "Москва",
                                "timezone_id": "130",
                                "country_id": "225",
                                "timezone": "Europe/Moscow",
                                "msk_offset": "+00:00",
                                "offset_str": "",
                                "group_nick": "russia",
                                "gmt_offset": "+03:00",
                                "offset": 10800
                            }
                        ],
                        "nick": "russia"
                    },
                    {
                        "timezones": [
                            {
                                "timezone_id": "166",
                                "id": "166",
                                "name": "Абхазия (MSK +00:00, GMT +03:00)",
                                "group_nick": "cis",
                                "offset": 10800,
                                "gmt_offset": "+03:00",
                                "country_id": "29386",
                                "timezone": "Europe/Moscow",
                                "msk_offset": "+00:00",
                                "offset_str": "(MSK +00:00, GMT +03:00)"
                            }
                        ],
                        "name": "Страны мира"
                    }
                ],
                "isExtendModeAvailable": true,
                "value": {
                    "isExtendModeOn": false,
                    "timeZone": {
                        "id": "130",
                        "text": "Москва"
                    },
                    "timeTargetCode": "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
                    "isHolidaySettingsEnabled": false,
                    "isWorkingWeekendEnabled": true,
                    "holidayShowSettings": {
                        "isShowing": true,
                        "showingFrom": 8,
                        "showingTo": 20,
                        "coefficient": 100
                    },
                    "preset": "worktime"
                }
            }
    },
        block,
        innerBlock,
        sandbox;

    function createBlock() {
        var popupDecorator = BEM.DOM.blocks['b-modal-popup-decorator']
                .create({ 'overflow-x': 'hidden' }, { bodyScroll: false });

        return popupDecorator.setPopupContent(ctx);
    }

    beforeEach(function() {
        block = createBlock();
        innerBlock = block.findBlockInside('b-time-targeting');
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    it('Функция isChanged возвращает promise', function() {
        expect(block.isChanged().hasOwnProperty('promise')).to.be.true;
    });

    describe('Изначальные состояния', function() {
        it('Функция hasChanges возвращает false', function() {
            expect(block.hasChanges()).to.be.false;
        });

        it('getValue возвращает заданные данные', function() {
            expect(block.getValue()).to.deep.equal({
                "timeZone": {
                    "id": "130",
                    "text": "Москва"
                },
                "timeTargetCode": "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
                "isHolidaySettingsEnabled": false,
                "isWorkingWeekendEnabled": true,
                "holidayShowSettings": {
                    "isShowing": true,
                    "showingFrom": 8,
                    "showingTo": 20,
                    "coefficient": 100
                },
                "isExtendModeOn": false,
                "preset": "worktime"
            });
        });
    });

    describe('API пробрасывает функции во внутренний блок', function() {
        [
            { name: 'isChanged', func: 'hasChanges'},
            { name: 'fixChanges', func: 'fixChanges'},
            { name: 'cancel', func: 'cancel'},
            { name: 'setExtendModeAvailable', func: 'setExtendModeAvailable'},
            { name: 'getValue', func: 'getValue'}
        ].forEach(function(test) {
            it('Функция ' + test.name +' вызывает внутреннюю функцию ' + test.func, function() {
                sandbox.spy(innerBlock, test.name);

                block[test.func]();

                expect(innerBlock[test.name].called).to.be.true;
            });
        });
    })
});
