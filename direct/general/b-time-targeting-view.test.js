describe('b-time-targeting-view', function() {
    var timeZoneGroups = {
            "timeZoneGroups": [
                {
                    "name": "Россия",
                    "nick": "russia",
                    "timezones": [
                        {
                            "timezone": "Europe/Kaliningrad",
                            "name": "Калининград (MSK -01:00)",
                            "id": "131",
                            "msk_offset": "-01:00",
                            "gmt_offset": "+02:00",
                            "group_nick": "russia",
                            "offset": 7200,
                            "country_id": "225",
                            "timezone_id": "131",
                            "offset_str": "(MSK -01:00)"
                        },
                        {
                            "gmt_offset": "+03:00",
                            "group_nick": "russia",
                            "msk_offset": "+00:00",
                            "name": "Москва",
                            "timezone": "Europe/Moscow",
                            "id": "130",
                            "country_id": "225",
                            "offset_str": "",
                            "timezone_id": "130",
                            "offset": 10800
                        }
                    ]
                }
            ]
        },
        timeTargetData = u._.extend({
            "isExtendModeAvailable": true
        }, timeZoneGroups),
        utils = u['b-time-targeting-view'],

        timeSettings = [
            {
                value: "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
                text: 'Круглосуточно',
                result: 'Круглосуточно'
            },
            {
                value: "1ABCDEFGHIKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
                text: 'круглосуточно, кроме Пн и Ср',
                result: "Показывать: ПН.: 00:00-09:00, 10:00-24:00, ВТ.: 00:00-24:00, СР.: 00:00-09:00, 10:00-24:00, ЧТ.-ВС.: 00:00-24:00",
                needTimeZone: true
            },
            {
                value: "1ABCDEFGHhIJKLhMNOPQRSTUVWX2ABCDEFGHIJnKLMnNhOnPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
                text: 'круглосуточно с расширенным режимом',
                result: "Круглосуточно",
                extended: true
            }
        ],
        worktimeSettings = [
            /* убрано в DIRECT-61541
            {
                value: true,
                text: 'учитывать рабочие выходные',
                result: "Рабочие выходные: по расписанию перенесенного буднего дня"
            },  */
            {
                value: false,
                text: 'не учитывать рабочие выходные',
                result: null
            }
        ],
        holidaySettings = [
            {
                enabled: true,
                value: {
                    "isShowing": false,
                    "showingFrom": 8,
                    "showingTo": 20,
                    "coefficient": 100
                },
                text: 'не учитывать праздничные дни',
                result: "По праздникам: не показывать"
            },
            {
                enabled: false,
                value: {
                    "isShowing": true,
                    "showingFrom": 8,
                    "showingTo": 20,
                    "coefficient": 100
                },
                text: 'учитывать праздничные дни',
                result: null
            },
            {
                enabled: true,
                value: {
                    "isShowing": true,
                    "showingFrom": 10,
                    "showingTo": 20,
                    "coefficient": 100
                },
                text: 'показывать по праздникам с 10 до 20',
                result: 'По праздникам: показывать с 10:00 до 20:00'
            },
            {
                enabled: true,
                extended: true,
                value: {
                    "isShowing": true,
                    "showingFrom": 10,
                    "showingTo": 20,
                    "coefficient": 80
                },
                text: 'показывать по праздникам с 10 до 20 c ограничениями на ставку',
                result: 'По праздникам: показывать с 10:00 до 20:00 (ограничение ставки на уровне 80%)'
            }
        ],
        timeZoneSettings = [
            {
                id: "131",
                text: "Калининград (MSK -01:00)",
                result: 'Время: Калининград (MSK -01:00)'
            },
            {
                id: "130",
                text: "Москва",
                result: "Время: Москва"
            }
        ],
        extendedModeSettings = [

        ];


    timeSettings.forEach(function(timeValue) {
        timeZoneSettings.forEach(function(timezoneValue) {
            worktimeSettings.forEach(function(worktimeValue) {
                holidaySettings.forEach(function(holidayValue) {
                    it('Выбранные настройки: ' +
                        timeValue.text + ', ' +
                        worktimeValue.text + ', ' +
                        holidayValue.text + ', ' +
                        'часовой пояс: ' + timezoneValue.text, function() {
                            var tData = u._.extend(timeTargetData, {
                                value: {
                                    timeZone: timezoneValue,
                                    timeTargetCode: timeValue.value,
                                    isHolidaySettingsEnabled: holidayValue.enabled,
                                    isWorkingWeekendEnabled: worktimeValue.value,
                                    holidayShowSettings: holidayValue.value,
                                    isExtendModeOn: timeValue.extended || holidayValue.extended,
                                    preset: "worktime"
                                }
                            }),
                                res = [
                                    timeValue.result,
                                    holidayValue.result,
                                    worktimeValue.result,
                                    timeValue.needTimeZone || (holidayValue.enabled && holidayValue.value.isShowing) ?
                                        timezoneValue.result :
                                        null,
                                    timeValue.extended ? "Добавлена корректировка ставки по времени" : null
                                ].filter(function(v) { return v !== null; });

                            expect(utils.getText(tData)).to.deep.equal(res);
                        });
                });
            });
        });
    });
});
