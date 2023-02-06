module.exports = {
    type: 'wizard',
    request_text: '691 миллион долларов',
    exp_flags: 'new_currency_converter',
    data_stub: [
        {
            type: 'units_converter',
            wizplace: 'upper',
            data: {
                srcName: 'ЦБ РФ',
                data: {
                    uv: [
                        {
                            i: '200',
                            sn1: 'RUB',
                            n1: 'российский рубль',
                            n2: 'российских рубля',
                            n5: 'российских рублей',
                            nd: 'российского рубля',
                            sn2: 'RUB',
                            sn5: 'RUB',
                            snd: 'RUB'
                        },
                        {
                            i: '204',
                            sn1: 'USD',
                            n1: 'американский доллар',
                            n2: 'американских доллара',
                            n5: 'американских долларов',
                            nd: 'американского доллара',
                            sn2: 'USD',
                            sn5: 'USD',
                            snd: 'USD'
                        },
                        {
                            i: '205',
                            sn1: 'EUR',
                            n1: 'евро',
                            n2: 'евро',
                            n5: 'евро',
                            nd: 'евро',
                            sn2: 'EUR',
                            sn5: 'EUR',
                            snd: 'EUR'
                        },
                        {
                            i: '216',
                            sn1: 'CHF',
                            n1: 'швейцарский франк',
                            n2: 'швейцарских франка',
                            n5: 'швейцарских франков',
                            nd: 'швейцарского франка',
                            sn2: 'CHF',
                            sn5: 'CHF',
                            snd: 'CHF'
                        },
                        {
                            i: '214',
                            sn1: 'GBP',
                            n1: 'британский фунт',
                            n2: 'британских фунта',
                            n5: 'британских фунтов',
                            nd: 'британского фунта',
                            sn2: 'GBP',
                            sn5: 'GBP',
                            snd: 'GBP'
                        },
                        {
                            i: '217',
                            sn1: 'JPY',
                            n1: 'японская иена',
                            n2: 'японских иены',
                            n5: 'японских иен',
                            nd: 'японской иены',
                            sn2: 'JPY',
                            sn5: 'JPY',
                            snd: 'JPY'
                        },
                        {
                            i: '213',
                            sn1: 'UAH',
                            n1: 'украинская гривна',
                            n2: 'украинских гривны',
                            n5: 'украинских гривен',
                            nd: 'украинской гривны',
                            sn2: 'UAH',
                            sn5: 'UAH',
                            snd: 'UAH'
                        },
                        {
                            i: '207',
                            sn1: 'KZT',
                            n1: 'казахстанский тенге',
                            n2: 'казахстанских тенге',
                            n5: 'казахстанских тенге',
                            nd: 'казахстанского тенге',
                            sn2: 'KZT',
                            sn5: 'KZT',
                            snd: 'KZT'
                        },
                        {
                            i: '202',
                            sn1: 'BYN',
                            n1: 'белорусский рубль',
                            n2: 'белорусских рубля',
                            n5: 'белорусских рублей',
                            nd: 'белорусского рубля',
                            sn2: 'BYN',
                            sn5: 'BYN',
                            snd: 'BYN'
                        }
                    ],
                    rel: {}
                },
                fromVal: '691000000',
                toVal: '24702766300',
                toName: '200',
                fromName: '204',
                date: '10.04.2014',
                historyUrl: 'http://news.yandex.ru/quotes/1.html',
                shortFromReadableName: 'USD'
            },
            counter_prefix: '/wiz/converter/currencies/',
            types: {
                kind: 'wizard'
            }
        }
    ]
};
