before(function() {

    window.mock['message-body'] = [
        {
            'params': {ids: '1'},
            'data': {
                info: {
                    stid: '102.66466005.241046198820547232969001058'
                },
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: ''
                    }
                ]
            }
        },
        {
            'params': {ids: '1.1'},
            'data': {
                info: {
                    stid: ''
                },
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: 'some text here<div>some div here</div>'
                    }
                ]
            }
        },
        //письмо без тела
        {
            'params': {ids: '1.2'},
            'data': {
                info: {
                    stid: ''
                },
                body: []
            }
        },
        {
            'params': {ids: '4'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><a href="mailto:doochik@ya.ru">link</a></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '5'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><a href="http://download.com/virus.exe">get fresh porno</a></div>'
                    }
                ],
                info: {
                    'delivered-to': ''
                }
            }
        },
        {
            'params': {ids: '5', is_spam: false, raw: false, draft: true},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><a href="http://download.com/virus.exe">get fresh porno</a></div>'
                    }
                ],
                info: {
                    'delivered-to': ''
                }
            }
        },
        {
            'params': {ids: '6'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><a href="http://download.com/virus.exe">get fresh porno</a></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '61'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<img src="/client/build/fake.gif" width="1" height="2"/>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '62'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<img src="/client/build/fake.gif" \n' +
                        'width="1" height="2"/>'
                    }
                ],
                info: {}
            }
        },
        // img without src
        {
            'params': {ids: '63'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<img width="1" height="2"/>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '7'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><a href="http://yandex.ru">Yandex</a></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '8'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><object/></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '9'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><embed/></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '10'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<div><form action="" formaction=""/></div>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '11'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<span class="wmi-video-link" params="a=1&b=2">videolink</span>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '12'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<blockquote>quote</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '12.1'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<blockquote style="font-size:1000%;color:red;">quote</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '13'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<blockquote>quote1 <blockquote>quote2</blockquote></blockquote>'
                    }
                ],
                info: {}
            }
        },
        // письмо с цитатами от Хабра
        {
            'params': {ids: '13.1'},
            'data': {
                body: [{
                    type: 'text',
                    subtype: 'html',
                    content: '<blockquote>quote1 <blockquote>quote2</blockquote></blockquote>'
                }],
                info: {}
            }
        },
        {
            'params': {ids: '14'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<blockquote>quote1</blockquote><blockquote>quote2</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '15'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html'
                    },
                    {
                        type: 'text',
                        subtype: 'plain'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '16'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'plain',
                        content: 'plain body1',
                        name: ""
                    },
                    {
                        type: 'text',
                        subtype: 'html',
                        content: 'html body2',
                        name: ""
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '17'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: 'html body1',
                        name: ""
                    },
                    {
                        type: 'text',
                        subtype: 'plain',
                        content: 'plain body2',
                        name: ""
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '18'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '<img alt="" border="0" src="/client/build/wow/static/i/yand-add-b.png\n   ">'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '19.1'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '21 декабря 2012, 06:66 The User    &lt;<a href="mailto:mail@mail.mail">mail@mail.mail</a>&gt;:<br><blockquote>quote</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '19.2'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '21 декабря 2012, 06:66 \n   &lt;<a href="mailto:mail@mail.mail">mail@mail.mail</a>&gt;:<br><blockquote>quote</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '19.3'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: 'On 21.12.2012 06:66, The User wrote:<br><blockquote>quote</blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '19.4'},
            'data': {
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: '21 декабря 2012, 06:66 The User &lt;<a href="mailto:mail@mail.mail">mail@mail.mail</a>&gt;:<br><blockquote>quote1 21 декабря 2012, 16:66 The Second User &lt;<a href="mailto:oooooooooooooooooooooooooo@mail2.mail2">oooooooooooooooooooooooooo@mail2.mail2</a>&gt;:<br><blockquote>quote2</blockquote></blockquote>'
                    }
                ],
                info: {}
            }
        },
        {
            'params': {ids: '20'},
            'data': {
                body: [{
                    type: 'text',
                    subtype: 'html',
                    content: '<img src="/client/build/images/foo.png"/>'
                }],
                info: {}
            }
        },
        {
            'params': {ids: '21'},
            'data': {
                body: [{
                    type: 'text',
                    subtype: 'html',
                    content: '<a href="http://ya.ru"/></a>'
                }],
                info: {}
            }
        },
        {
            'params': {ids: '22'},
            'data': {
                body: [{
                    type: 'text',
                    subtype: 'plain',
                    content: '<p>attach file "приложил",</p>'
                }],
                info: {}
            }
        },
        {
            'params': {ids: '23'},
            'data': {
                body: [{
                    type: 'text',
                    subtype: 'plain',
                    content: '<p>Зааттачил файлик</p>'
                }],
                info: {}
            }
        },
        // getRecipients
        {
            'params': {ids: 'r1'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'test1@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r2'},
            'data': {
                info: {
                    field: [
                        {type: 'from', email: 'test2@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r3'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'my@ya.ru'},
                        {type: 'from', email: 'test3@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r4'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'my@ya.ru'},
                        {type: 'from', email: 'my@ya.ru'},
                        {type: 'to', email: 'test4@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r5'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'test5@ya.ru'},
                        {type: 'from', email: 'test5@ya.ru'},
                        {type: 'to', email: 'test6@ya.ru'},
                        {type: 'to', email: 'test7@ya.ru'},
                        {type: 'to', email: 'my@ya.ru'},
                        {type: 'cc', email: 'test8@ya.ru'},
                        {type: 'cc', email: 'test9@ya.ru'},
                        {type: 'cc', email: 'my@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r6'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'my@ya.ru'},
                        {type: 'from', email: 'my@ya.ru'},
                        {type: 'to', email: 'my@ya.ru'},
                        {type: 'to', email: 'test6@ya.ru'},
                        {type: 'to', email: 'test7@ya.ru'},
                        {type: 'cc', email: 'test8@ya.ru'},
                        {type: 'cc', email: 'test9@ya.ru'},
                    ]
                }
            }
        },
        {
            'params': {ids: 'r7'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'my2@ya.ru'},
                        {type: 'from', email: 'my2@ya.ru'},
                        {type: 'to', email: 'my@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r8'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'some@ya.ru'},
                        {type: 'from', email: 'some@ya.ru'},
                        {type: 'to', email: 'my.dash@ya.ru'},
                        {type: 'to', email: 'my-dot@ya.ru'},
                        {type: 'to', email: 'my-dot.dash@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'r9'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'some@ya.ru'},
                        {type: 'from', email: 'some@ya.ru'},
                        {type: 'to', email: 'mYcameLcasE@yA.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'withCA'},
            'data': {
                info: {
                    field: [
                        {type: 'from', email: 'some@ya.ru'},
                        {type: 'to', email: '+79999999@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'withCAandCC'},
            'data': {
                info: {
                    field: [
                        {type: 'from', email: 'some@ya.ru'},
                        {type: 'to', email: '+799999999@ya.ru'},
                        {type: 'to', email: 'test6@ya.ru'},
                        {type: 'to', email: 'test7@ya.ru'},
                        {type: 'cc', email: 'test8@ya.ru'},
                        {type: 'cc', email: 'test9@ya.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'withCorpRecipients'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'bobuk@yandex-team.ru'},
                        {type: 'from', email: 'bobuk@yandex-team.ru'},
                        {type: 'to', email: 'kukutz@yandex-team.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'withCorpAndNotCorpRecipients'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'bobuk@yandex-team.ru'},
                        {type: 'from', email: 'bobuk@yandex-team.ru'},
                        {type: 'to', email: 'kukutz@yandex-team.ru'},
                        {type: 'to', email: 'foo@yandex.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'withCorpRecipientsAndCC'},
            'data': {
                info: {
                    field: [
                        {type: 'reply-to', email: 'bobuk@yandex-team.ru'},
                        {type: 'from', email: 'bobuk@yandex-team.ru'},
                        {type: 'to', email: 'kukutz@yandex-team.ru'},
                        {type: 'cc', email: 'bar@yandex-team.ru'}
                    ]
                }
            }
        },
        {
            'params': {ids: 'eticket1'},
            'data': {
                "signatures": {},
                "calendars": {},
                "pkpasses": {},
                "info": {
                    "stid": "11772.775691647.1447850221156688869939525324241",
                    "references": "",
                    "in-reply-to": "",
                    "message-id": "hjcodkcovvflxvhxpemo@mail-widget-work01f.mail.yandex.net",
                    "filter-id": "",
                    "personal-spam": "",
                    "delivered-to": "",
                    "list-unsubscribe": "",
                    "eticket-flight-direction": "forward",
                    "date": {
                        "timestamp": "1397807655000",
                        "user_timestamp": "1397807655000",
                        "chunks": {
                            "year": 2014,
                            "month": 3,
                            "date": 18,
                            "hours": 11,
                            "minutes": 54
                        },
                        "full": "18 апр. в 11:54"
                    },
                    "field": [
                        {
                            "name": "Яндекс.Почта",
                            "email": "hello@yandex-team.ru",
                            "type": "from",
                            "ref": "0a2356a42b64cd3859d25ae8963a3a0a",
                            "is_service": true,
                            "is_free": true
                        },
                        {
                            "name": "yt.user28@yandex.ru",
                            "email": "yt.user28@yandex.ru",
                            "type": "to",
                            "ref": "21f2fdcdee502ce2c7bc7962d0343eb6",
                            "is_service": false,
                            "is_free": true
                        }
                    ]
                },
                "dkim-verify": {
                    "status": "OK",
                    "domain": "yandex.ru"
                },
                "attachment": [],
                "body": [
                    {
                        "hid": "1.1",
                        "main": "true",
                        "type": "text",
                        "subtype": "html",
                        "disposition_value": "",
                        "content_id": "",
                        "content_location": "",
                        "disposition_filename": "",
                        "name": "",
                        "class": "doc",
                        "length": "861",
                        "lang": "1",
                        "content": "Здравствуйте!<br /><br />У вас запланирован полёт: Москва - Малага<br />Рейс: UN349, 2 ноября в 10:25<br /><br />Аэропорт: Домодедово (<a href=\"https://mail.yandex.ru/re.jsx?h=a,5M9QM9VhXGb379UUR6crfQ&amp;l=aHR0cHM6Ly93d3cuYWVyb2V4cHJlc3MucnUv\" target=\"_blank\">купить билет на аэроэкспресс</a>)<br /><br />До вылета осталось меньше суток.<br /><br />Не забудьте пройти <a href=\"https://mail.yandex.ru/re.jsx?h=a,DOnmOVbMfJXjKD3TOJuZEA&amp;l=aHR0cDovL3d3dy50cmFuc2Flcm8ucnUvcnUvd2ViY2hlY2tpbg\" target=\"_blank\">регистрацию на рейс</a><span>, а также распечатать <a href=\"https://mail.yandex.ru/re.jsx?h=a,3w_SovUIaFRw7RPgXHxSuQ&amp;l=aHR0cHM6Ly9tYWlsLnlhbmRleC5ydS9tZXNzYWdlP2lkcz0yMzAwMDAwMDA0MDEwOTkxODM2\" target=\"_blank\">информацию о полёте</a></span>.<br /><br />Удачного путешествия!"
                    }
                ],
                "facts": {
                    "ticket": {
                        "flight": [
                            {
                                "direction": "forward",
                                "airport_dep": "Домодедово",
                                "city_dep": "Москва",
                                "city_arr": "Малага",
                                "trip": [
                                    {
                                        "flight_number": "UN349",
                                        "iata": "UN",
                                        "checkin": "http://www.transaero.ru/ru/webcheckin",
                                        "phone": "",
                                        "city_arr_geoid": 10437,
                                        "departure": 1414909500000,
                                        "city_dep_geoid": 213
                                    }
                                ],
                                "departure": 1414909500000
                            }
                        ],
                        "hash": "8ba61bfc2bb15e9aeb45e228bde5e55a"
                    }
                },
                "is-spam": false,
                "is-support": false
            }
        },
        {
            'params': {ids: 'hotels1'},
            'data': {
                "signatures": {},
                "calendars": {},
                "pkpasses": {},
                "info": {
                    "stid": "15892.809508090.428612773414079547528211042063",
                    "references": "",
                    "in-reply-to": "",
                    "message-id": "<2086685599.7.1402398545273.JavaMail.aero@qa-aero-agent03d>",
                    "filter-id": "",
                    "personal-spam": "",
                    "delivered-to": "",
                    "list-unsubscribe": "",
                    "date": {
                        "timestamp": "1390904133000",
                        "user_timestamp": "1390904133000",
                        "chunks": {
                            "year": 2014,
                            "month": 0,
                            "date": 28,
                            "hours": 14,
                            "minutes": 15
                        },
                        "full": "28 янв. в 14:15"
                    },
                    "field": [
                        {
                            "name": "Hotels.com Российская Федерация",
                            "email": "confirmation@mail.hotels.com",
                            "type": "from",
                            "ref": "b01729fcf53a72f0353fd85d49fa47e1",
                            "is_service": true,
                            "is_free": false
                        },
                        {
                            "name": "chakxx10@yandex.ru",
                            "email": "chakxx10@yandex.ru",
                            "type": "to",
                            "ref": "abe4f2d20af1304c8c6cba81827ebe61",
                            "is_service": false,
                            "is_free": true
                        },
                        {
                            "name": "Hotels.com ГђВ ГђВѕГ‘ВЃГ‘ВЃГђВёГђВ№Г‘ВЃГђВєГђВ°Г‘ВЏ ГђВ¤ГђВµГђВґГђВµГ‘в‚¬ГђВ°Г‘вЂ ГђВёГ‘ВЏ",
                            "email": "reply-fe63167076620678771c-3502318_HTML-1508022569-198875-3944@reply.mail.hotels.com",
                            "type": "reply-to",
                            "ref": "bae8e5c2ff54379b28e7fc016be58700",
                            "is_service": false,
                            "is_free": false
                        }
                    ]
                },
                "dkim-verify": {
                    "status": "BADSIG"
                },
                "attachment": [],
                "body": [
                    {
                        "hid": "1.2",
                        "main": "true",
                        "type": "text",
                        "subtype": "html",
                        "disposition_value": "",
                        "content_id": "",
                        "content_location": "",
                        "disposition_filename": "",
                        "name": "",
                        "class": "doc",
                        "length": "97991",
                        "lang": "1",
                        "facts": {
                            "addr": [
                                {
                                    "geo": {
                                        "country": "россия"
                                    },
                                    "geo_addr": "Российская Федерация"
                                }
                            ],
                            "hotels": [
                                {
                                    "people": "2",
                                    "check-out_date": "2014-07-18 00:00:00",
                                    "hotel": "Aparton",
                                    "reservation_number": "10062014001",
                                    "city_geoid": "213",
                                    "city": "Москва",
                                    "country": "Россия",
                                    "cancellation_info": "30.7.2014 0:0:0",
                                    "check-inn_date": "2014-07-15 00:00:00",
                                    "price": "$70.00",
                                    "address": "Nezavisimosti Prospekt 31, office 26 Moscow 220000 Russia +375291159000",
                                    "link": "https://ssl-ru.hotels.com/customer_care/booking_details.html?pos=HCOM_RU&lo…intlid=Body_ViewRes_H839.008.001_H1099.000.000_H1121.001.003_H1237.001.000",
                                    "country_geoid": "225",
                                    "cancellation_date": "2014-07-30 00:00:00",
                                    "uniq_id": "f8df3713ca3a79f01bdbefe6abbb860f"
                                }
                            ],
                            "events": null,
                            "unsubscribe": null
                        },
                        "content": "message content"
                    }
                ],
                "facts": {
                    "addr": [
                        {
                            "geo": {
                                "country": "россия"
                            },
                            "geo_addr": "Российская Федерация"
                        }
                    ],
                    "hotels": [
                        {
                            "people": "2",
                            "check-out_date": "2014-07-18 00:00:00",
                            "hotel": "Aparton",
                            "reservation_number": "10062014001",
                            "city_geoid": "213",
                            "city": "Москва",
                            "country": "Россия",
                            "cancellation_info": "30.7.2014 0:0:0",
                            "check-inn_date": "2014-07-15 00:00:00",
                            "price": "$70.00",
                            "address": "Nezavisimosti Prospekt 31, office 26 Moscow 220000 Russia +375291159000",
                            "link": "https://ssl-ru.hotels.com/customer_care/booking_details.html?pos=HCOM_RU&lo…intlid=Body_ViewRes_H839.008.001_H1099.000.000_H1121.001.003_H1237.001.000",
                            "country_geoid": "225",
                            "cancellation_date": "2014-07-30 00:00:00",
                            "uniq_id": "f8df3713ca3a79f01bdbefe6abbb860f"
                        }
                    ],
                    "events": null,
                    "unsubscribe": null
                },
                "is-spam": false,
                "is-support": false,
                "has-links": true,
                "has-img": true
            }
        },
        {
            params: {ids: 'calendar1'},
            data: {
                "signatures": {},
                "calendars": {"calendar": [{"type": "text", "subtype": "calendar", "hid": "1.1.3", "length": "1731"}]},
                "pkpasses": {},
                "info": {
                    "stid": "49096.1120000000007162.1363643127213511294960595668560",
                    "references": "",
                    "in-reply-to": "",
                    "message-id": "njtjjtvjbouyiebqqswb@calcorp-back1h.cmail.yandex.net",
                    "filter-id": "",
                    "personal-spam": "",
                    "spam": "",
                    "delivered-to": "",
                    "list-unsubscribe": "",
                    "date": {
                        "timestamp": "1429268809000",
                        "user_timestamp": "1429268809000",
                        "chunks": {"year": 2015, "month": 3, "date": 17, "hours": 14, "minutes": 6}
                    },
                    "field": [{
                        "name": "Артур Будько",
                        "email": "info@calendar.yandex-team.ru",
                        "type": "from",
                        "ref": "544ba7d241b0deb79e212d6021af2398",
                        "is_service": true,
                        "is_free": false
                    }, {
                        "name": "aandrosov@yandex-team.ru",
                        "email": "aandrosov@yandex-team.ru",
                        "type": "to",
                        "ref": "aa097a0e2c6e2e7718424cd31cd87773",
                        "is_service": false,
                        "is_free": false
                    }, {
                        "name": "Артур Будько",
                        "email": "b-arteg@yandex-team.ru",
                        "type": "reply-to",
                        "ref": "73286070ff421ec1c449e7bb5f3bbb17",
                        "is_service": false,
                        "is_free": false
                    }]
                },
                "dkim-verify": {"status": "OK", "domain": "yandex-team.rugollum.yandex.ru"},
                "attachment": [{
                    "hid": "1.1.3",
                    "subtype": "calendar",
                    "type": "text",
                    "disposition_value": "",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "",
                    "name": "",
                    "class": "general",
                    "length": "1731",
                    "can-view-file": false
                }, {
                    "hid": "1.2",
                    "subtype": "calendar",
                    "type": "text",
                    "disposition_value": "attachment",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "event.ics",
                    "name": "event.ics",
                    "name-uri-encoded": "event.ics",
                    "filename": "event",
                    "fileext": ".ics",
                    "class": "general",
                    "length": "1351",
                    "can-view-file": false
                }],
                "body": [{
                    "hid": "1.1.2",
                    "main": "true",
                    "type": "text",
                    "subtype": "html",
                    "disposition_value": "",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "",
                    "name": "",
                    "class": "doc",
                    "length": "4212",
                    "lang": "1",
                    "content": "\u003cdiv link=\"#1952ce\" alink=\"red\" style=\"line-height:1.4;\"\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd\u003e\u003ctable cellspacing=\"0\" cellpadding=\"5\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd style=\"padding:0 7px 1px;\"\u003e\u003cfont size=\"4\" face=\"Arial,sans-serif\"\u003e\u003cb\u003eАртур Будько изменил информацию о встрече «\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,26uZ2V9x_VpedYDELEI5AA&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9ldmVudC8_dWlkPTExMjAwMDAwMDAwMDAzNzEmZXZlbnRfaWQ9MTk5NDY4MiZzaG93X2RhdGU9MjAxNS0wNC0xNyZ2aWV3X3R5cGU9d2Vlaw\"\u003eПитер. Задачи на неделю\u003c/a\u003e»\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003ctr\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eСегодня, 17-го апреля с 14:00 до 15:00  (Europe/Moscow, GMT+03:00)\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd valign=\"top\"\u003e\u003cfont color=\"#a2a2a2\" face=\"Arial,sans-serif\" size=\"3\"\u003e\u003cb\u003eОрганизатор\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003ctd width=\"5\"\u003e\u003c/td\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eВиталий Мещанинов\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003ctr\u003e\u003ctd valign=\"top\"\u003e\u003cfont color=\"#a2a2a2\" face=\"Arial,sans-serif\" size=\"3\"\u003e\u003cb\u003eПриглашены\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003ctd width=\"5\"\u003e\u003c/td\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eГеоргий Беседин\u003cbr /\u003eАртур Будько\u003cbr /\u003eАлексей Андросов\u003cbr /\u003e\u003c/font\u003e\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,LRJ00cQlGvTreaaHRo1mfA&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9ldmVudC8_dWlkPTExMjAwMDAwMDAwMDAzNzEmcHJpdmF0ZV90b2tlbj0xMjYwMzZhMTIxYjczOTgxZjJiMTQyZTZiZjc2Y2ViOTQ0ZDZkYWVmJmV2ZW50X2lkPTE5OTQ2ODImc2hvd19kYXRlPTIwMTUtMDQtMTc\"\u003e\u003cfont face=\"Verdana,sans-serif\" size=\"1\"\u003eПосмотреть, кто пойдёт\u003c/font\u003e\u003c/a\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd bgcolor=\"#e3eafd\"\u003e\u003ctable cellspacing=\"0\" cellpadding=\"5\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd style=\"padding:0 7px 1px;\"\u003e\u003cfont size=\"3\" face=\"Arial,sans-serif\"\u003e\u003cb\u003e\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,MFdkjRGI6b0ULmlKZ6ZpLQ&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9hcGkvYWRkLWV2ZW50LWludml0YXRpb24_dWlkPTExMjAwMDAwMDAwMDAzNzEmcHJpdmF0ZV90b2tlbj0xMjYwMzZhMTIxYjczOTgxZjJiMTQyZTZiZjc2Y2ViOTQ0ZDZkYWVmJmV2ZW50X2lkPTE5OTQ2ODImZGVjaXNpb249bm8\"\u003eНе пойду\u003c/a\u003e\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd\u003e\u003chr size=\"1\" noshade=\"noshade\" color=\"#cccccc\" style=\"border:0 hidden;border-top:1px solid #cccccc;height:0;margin-top:30px;\" /\u003e\u003cfont size=\"1\" face=\"Verdana, sans-serif\" color=\"#9a9a9a\"\u003eУведомление отправлено через \u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,GgX3HjFjEobI4ffOeSejfw&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydT91aWQ9MTEyMDAwMDAwMDAwMDM3MQ\"\u003eЯндекс.Календарь\u003c/a\u003e. Если вы не хотите получать такие письма, \u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,yhVCJ6rgKpF0-L3KLMVlQg&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS90dW5lP3VpZD0xMTIwMDAwMDAwMDAwMzcx\"\u003eоткажитесь от них\u003c/a\u003e.  © 2007—2015 «\u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,uNkm2uzDbNsdtfGA9hXOgQ&amp;l=aHR0cDovL3d3dy55YW5kZXgucnUv\"\u003eЯндекс\u003c/a\u003e»\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/div\u003e"
                }],
                "facts": {}
            }
        },
        {
            params: {ids: 'calendar2'},
            data: {
                "signatures": {},
                "calendars": {
                    "calendar": [{
                        "type": "text",
                        "subtype": "calendar",
                        "hid": "1.1.3",
                        "length": "1731"
                    }, {"type": "text", "subtype": "calendar", "hid": "1.2", "length": "1351"}]
                },
                "pkpasses": {},
                "info": {
                    "stid": "49096.1120000000007162.1363643127213511294960595668560",
                    "references": "",
                    "in-reply-to": "",
                    "message-id": "njtjjtvjbouyiebqqswb@calcorp-back1h.cmail.yandex.net",
                    "filter-id": "",
                    "personal-spam": "",
                    "spam": "",
                    "delivered-to": "",
                    "list-unsubscribe": "",
                    "date": {
                        "timestamp": "1429268809000",
                        "user_timestamp": "1429268809000",
                        "chunks": {"year": 2015, "month": 3, "date": 17, "hours": 14, "minutes": 6}
                    },
                    "field": [{
                        "name": "Артур Будько",
                        "email": "info@calendar.yandex-team.ru",
                        "type": "from",
                        "ref": "544ba7d241b0deb79e212d6021af2398",
                        "is_service": true,
                        "is_free": false
                    }, {
                        "name": "aandrosov@yandex-team.ru",
                        "email": "aandrosov@yandex-team.ru",
                        "type": "to",
                        "ref": "aa097a0e2c6e2e7718424cd31cd87773",
                        "is_service": false,
                        "is_free": false
                    }, {
                        "name": "Артур Будько",
                        "email": "b-arteg@yandex-team.ru",
                        "type": "reply-to",
                        "ref": "73286070ff421ec1c449e7bb5f3bbb17",
                        "is_service": false,
                        "is_free": false
                    }]
                },
                "dkim-verify": {"status": "OK", "domain": "yandex-team.rugollum.yandex.ru"},
                "attachment": [{
                    "hid": "1.1.3",
                    "subtype": "calendar",
                    "type": "text",
                    "disposition_value": "",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "",
                    "name": "",
                    "class": "general",
                    "length": "1731",
                    "can-view-file": false
                }, {
                    "hid": "1.2",
                    "subtype": "calendar",
                    "type": "text",
                    "disposition_value": "attachment",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "event.ics",
                    "name": "event.ics",
                    "name-uri-encoded": "event.ics",
                    "filename": "event",
                    "fileext": ".ics",
                    "class": "general",
                    "length": "1351",
                    "can-view-file": false
                }],
                "body": [{
                    "hid": "1.1.2",
                    "main": "true",
                    "type": "text",
                    "subtype": "html",
                    "disposition_value": "",
                    "content_id": "",
                    "content_location": "",
                    "disposition_filename": "",
                    "name": "",
                    "class": "doc",
                    "length": "4212",
                    "lang": "1",
                    "content": "\u003cdiv link=\"#1952ce\" alink=\"red\" style=\"line-height:1.4;\"\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd\u003e\u003ctable cellspacing=\"0\" cellpadding=\"5\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd style=\"padding:0 7px 1px;\"\u003e\u003cfont size=\"4\" face=\"Arial,sans-serif\"\u003e\u003cb\u003eАртур Будько изменил информацию о встрече «\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,26uZ2V9x_VpedYDELEI5AA&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9ldmVudC8_dWlkPTExMjAwMDAwMDAwMDAzNzEmZXZlbnRfaWQ9MTk5NDY4MiZzaG93X2RhdGU9MjAxNS0wNC0xNyZ2aWV3X3R5cGU9d2Vlaw\"\u003eПитер. Задачи на неделю\u003c/a\u003e»\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003ctr\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eСегодня, 17-го апреля с 14:00 до 15:00  (Europe/Moscow, GMT+03:00)\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd valign=\"top\"\u003e\u003cfont color=\"#a2a2a2\" face=\"Arial,sans-serif\" size=\"3\"\u003e\u003cb\u003eОрганизатор\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003ctd width=\"5\"\u003e\u003c/td\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eВиталий Мещанинов\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003ctr\u003e\u003ctd valign=\"top\"\u003e\u003cfont color=\"#a2a2a2\" face=\"Arial,sans-serif\" size=\"3\"\u003e\u003cb\u003eПриглашены\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003ctd width=\"5\"\u003e\u003c/td\u003e\u003ctd\u003e\u003cfont face=\"Arial,sans-serif\" size=\"3\"\u003eГеоргий Беседин\u003cbr /\u003eАртур Будько\u003cbr /\u003eАлексей Андросов\u003cbr /\u003e\u003c/font\u003e\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,LRJ00cQlGvTreaaHRo1mfA&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9ldmVudC8_dWlkPTExMjAwMDAwMDAwMDAzNzEmcHJpdmF0ZV90b2tlbj0xMjYwMzZhMTIxYjczOTgxZjJiMTQyZTZiZjc2Y2ViOTQ0ZDZkYWVmJmV2ZW50X2lkPTE5OTQ2ODImc2hvd19kYXRlPTIwMTUtMDQtMTc\"\u003e\u003cfont face=\"Verdana,sans-serif\" size=\"1\"\u003eПосмотреть, кто пойдёт\u003c/font\u003e\u003c/a\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd bgcolor=\"#e3eafd\"\u003e\u003ctable cellspacing=\"0\" cellpadding=\"5\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd style=\"padding:0 7px 1px;\"\u003e\u003cfont size=\"3\" face=\"Arial,sans-serif\"\u003e\u003cb\u003e\u003ca href=\"http://mail.yandex-team.ru/re.jsx?h=a,MFdkjRGI6b0ULmlKZ6ZpLQ&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS9hcGkvYWRkLWV2ZW50LWludml0YXRpb24_dWlkPTExMjAwMDAwMDAwMDAzNzEmcHJpdmF0ZV90b2tlbj0xMjYwMzZhMTIxYjczOTgxZjJiMTQyZTZiZjc2Y2ViOTQ0ZDZkYWVmJmV2ZW50X2lkPTE5OTQ2ODImZGVjaXNpb249bm8\"\u003eНе пойду\u003c/a\u003e\u003c/b\u003e\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003ctable cellpadding=\"4\" width=\"100%\"\u003e\u003ctbody\u003e\u003ctr\u003e\u003ctd\u003e\u003chr size=\"1\" noshade=\"noshade\" color=\"#cccccc\" style=\"border:0 hidden;border-top:1px solid #cccccc;height:0;margin-top:30px;\" /\u003e\u003cfont size=\"1\" face=\"Verdana, sans-serif\" color=\"#9a9a9a\"\u003eУведомление отправлено через \u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,GgX3HjFjEobI4ffOeSejfw&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydT91aWQ9MTEyMDAwMDAwMDAwMDM3MQ\"\u003eЯндекс.Календарь\u003c/a\u003e. Если вы не хотите получать такие письма, \u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,yhVCJ6rgKpF0-L3KLMVlQg&amp;l=aHR0cHM6Ly9jYWxlbmRhci55YW5kZXgtdGVhbS5ydS90dW5lP3VpZD0xMTIwMDAwMDAwMDAwMzcx\"\u003eоткажитесь от них\u003c/a\u003e.  © 2007—2015 «\u003ca style=\"color:#9a9a9a;\" href=\"http://mail.yandex-team.ru/re.jsx?h=a,uNkm2uzDbNsdtfGA9hXOgQ&amp;l=aHR0cDovL3d3dy55YW5kZXgucnUv\"\u003eЯндекс\u003c/a\u003e»\u003c/font\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/tbody\u003e\u003c/table\u003e\u003c/div\u003e"
                }],
                "facts": {}
            }
        }
    ];


    [2, 3, 121, 122, '2190000000624510036', '2190000000624510037', 'insertMessage1-1', 'insertMessage1-2', 'insertMessage1-3'].forEach(function (ids) {
        mock['message-body'].push({
            'params': {ids: ids, is_spam: false, raw: false, draft: false},
            'data': {
                info: {
                    stid: '102.66466005.241046198820547232969001058'
                },
                body: [
                    {
                        type: 'text',
                        subtype: 'html',
                        content: ''
                    }
                ]
            }
        });
    });

});
