(function() {
    var getTestData = function() {
            return [
                {
                    'улица Льва Толстого, дом 10, строение 1':
                    [
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: '01.08.2018',
                            OutageEnd: '12.08.2018',
                            OutageBeginTime: '14.00.01',
                            Porches: 'все подъезды',
                            uchFullName: 'Участок №152',
                            uchPlace: 'Школа 39',
                            uchPhone: '+7 903 755 7555',
                            uchAddressLink: {
                                caption: 'улица льва толстого 16',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '8:00 - 10:00'
                        }
                    ]
                },
                {
                    'улица Льва Толстого, дом 10, строение 3':
                    [
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: '03.08.2018',
                            OutageEnd: '12.08.2018',
                            OutageBeginTime: '14.00.01',
                            Porches: 'все подъезды',
                            uchFullName: 'Участок №188',
                            uchPlace: 'Поволжский федеральный университет',
                            uchPhone: null,
                            uchAddressLink: {
                                caption: 'улица льва толстого 16',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '8:00 - 10:00'
                        }
                    ]
                },
                {
                    'улица Льва Толстого, дом 14':
                    [
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: '14.08.2018',
                            OutageEnd: '12.08.2018',
                            OutageBeginTime: '14.00.01',
                            Porches: 'Подъезд 1',
                            uchFullName: 'Участок 4',
                            uchPlace: 'Школа 39',
                            uchPhone: '+7 903 755 7555',
                            uchAddressLink: {
                                caption: 'улица льва толстого 16',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '14:00 - 15:00'
                        },
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: '14.08.2018',
                            OutageEnd: '12.08.2018',
                            OutageBeginTime: '14.00.01',
                            Porches: 'Подъезд 2',
                            uchFullName: 'Участок 5',
                            uchPlace: 'Школа 39',
                            uchPhone: '+7 903 755 7555',
                            uchAddressLink: {
                                caption: 'улица льва толстого 16',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '13:00 - 16:00'
                        }
                    ]
                },
                {
                    'улица Льва Толстого, дом 19':
                    [
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: 'без отключения',
                            OutageEnd: 'без отключения',
                            OutageBeginTime: '14.00.01',
                            uchPlace: 'Школа 39 ',
                            Porches: 'все подъезды',
                            uchFullName: 'Участок 2',
                            uchPhone: '+7 903 755 7555',
                            uchAddressLink: {
                                caption: 'улица льва толстого 16 дом 19',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '8:00 - 10:00'
                        }
                    ]
                },
                {
                    'внутригородская территория поселение Внуковское, п. Внуково, ул. Льва Льва, д. 4, стр. 1':
                    [
                        {
                            OutageEndTime: '13.59.01',
                            OutageBegin: 'без отключения',
                            OutageEnd: 'без отключения',
                            OutageBeginTime: '14.00.01',
                            uchPlace: 'Школа 39 ',
                            Porches: 'все подъезды',
                            uchFullName: 'Участок 2',
                            uchPhone: '+7 903 755 7555',
                            uchAddressLink: {
                                caption: 'внутригородская территория поселение Внуковское, п. Внуково, ул. Льва Льва, д. 4, стр. 1',
                                url: 'https://yandex.ru/maps/?text=%D0%A3%D0%BB%D0%B8%D1%86%D0%B0%20%D0%BB%D1%8C%D0%B2%D0%B0%20%D1%82%D0%BE%D0%BB%D1%81%D1%82%D0%BE%D0%B3%D0%BE%2016'
                            },
                            uchWorkTime: '8:00 - 10:00'
                        }
                    ]
                }
            ];
        },
        testInputValues = ['льва', 'фрунзе', 'ленина', 'льва толстого', 'улица Льва Толстого, дом 10, строение 1', 'улица Тимура Фрунзе, дом 1/2, строение 1', 'Кремль, дом Б/Н'];

    BEM.DOM.decl('suggest-subsearch', {
        _processSuggestResponse: function(data) {
            var _this = this;

            if (testInputValues.some(function(val) { return val === _this._input.val() })) {
                data = getTestData();
            } else {
                data = [];
            }

            return this.__base.call(this, data);
        }
    });
})();
