/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/offers/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
            },
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        page: {
            number: 1,
            count: 5,
            total: 11,
        },
        processingOptions: {
            adult: false,
        },
        id: '1571596216754/9aef51e972129fa214e33ac25b950500',
        time: '2019-10-20T21:30:16.888+03:00',
        marketUrl: 'https://market.yandex.ru?pp=484&clid=2210590&distr_type=4',
    },
    offers: [
        {
            id: 'yDpJekrrgZEVznOy6t4UmKwXnVrdZBxHN47KVuWby3eCQg9OiALAGQ',
            wareMd5: '8bQre-P6Cx5xSrFdtsGgdQ',
            skuType: 'market',
            name: 'Жесткий диск Seagate ST2000DM008 2000Gb',
            description: 'HDD 2000 Гб, 3.5 дюймов (SATA-III) • для настольного ПК.',
            price: {
                value: '3545',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlWSXFwW01Igw-yfU3Yu-qYLExT9ZZ6i3DpJX7Jk5fNsYsR8jAP6YH1fwpr_U7SCxawz3yqORNj6i-2I87G8EWr_kPzeLn2NW4XummWU3hwoUOCuUx_XIuPYiZZcJZbdKZfTM8ytc3h-MaUDD0i-i6TzSnFa6HjPJ8ivU-UBPIhE91lEajtMdoWJq6pwLp_DTOw0QXAGhi7IUPNLBDVied-SQw9-sk6WLUjMxcQpou9F_CC7ftFAces7ZCqQIIIxve4Od05gZnmQL8EKjj2bkrkNrTPBV9T2fRyosDneh5x3TQGpsFqWwOcg4n2gT-cGCf597oJPpyS3Xmh-p4bwC1-76tyA7lYcN7NGZvmNzQe78lY4OE06kFB6j1ZzX3BK61rsXMuLPycmIzPhafeJj8-CRGJhvnYjBXAWwDe1VhjWRgAeRiRjlT6OdHPvWHLeKwxvSPp3LXEuLZJxuLqvgfZJ6CVCY1iboeKher6rKQx1XOyKt79DscTNgQFa0v3juBe45Uu7pdvNVcxOk96cv-VhEkd6ZT31QxUzqkEmxkvnbOAvxM5PdktnZW0XHVKK4uZKiNS-JNzqlwZM9vZx0bfuNrRcscVeNKnCqG18n_TFpBFVucl7Kj1KijMpjLh4DNIZzqiU9dlqWqARJarbJLPBhOSQ1of_TBDP9Thz7hxmLCcOPnU2lEh1bZk-M8ElrPcULIK_k6V3f1T8fAPUVVeIv4087F5esqEz1DKpZydqIebZD4D6m2oZX528lMQEHV-TVAcPQyC7wVb1-9m6UyJgdwqemP85PpZ6X09GHUtpRp5y3jzJAIACyU8cETRYke0rMK0qAdTwe1R38L3SykWRAIPDWsJ4gf_5kea41QQrnPXp6aGQ0Kp60Ce7E3JV7beSaSMQof1n30a8lCLZnZ0XvkXwHsCFiA,,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPCevkcbDX_pOUNFAtm0oxdNXb4smtyBUMfUtlk_aKp9bBHP6axSSBkKdz8Fv8iwCCyZxZGKbEhedBFCYrIhm4dnhb9kB_l8KNi3T-r-EQ2cE0QAIIL-6o10vDWX1c3Rza40ctx2ZZyWnaT16ObZmrggIUahcSYJqEF23O35113NRYSg09hk1Ctg6iI7dC4hNJ03R4PsjX2N2RiJfAoeAoWU5DF_-EQSB-jqlLLNQxObWkv_IsSYzVTxtgSSElT4_kcCzW5jMXuse7bXV4kZ535saelgujEuLDmbd4V5O3N-ymV-K4x4tzz1ETcpMxu-FOjqD3xcptM0NABZPikJz7F2ljHnf8HL8YdlFIRVu_dlEhsIynC3pXzS7SmvOXZaU6cvGlI7_mZWf0bj-rmGvZkmCTekpyLTB_mWQ47Pznxt5&b64e=1&sign=9bf78ca4ce9cf868d06ad79bead62881&keyno=1',
            urls: {
                484: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZlWSXFwW01Igw-yfU3Yu-qYLExT9ZZ6i3DpJX7Jk5fNsYsR8jAP6YH1fwpr_U7SCxawz3yqORNj6i-2I87G8EWr_kPzeLn2NW4XummWU3hwoUOCuUx_XIuPYiZZcJZbdKZfTM8ytc3h-MaUDD0i-i6TzSnFa6HjPJ8ivU-UBPIhE91lEajtMdoWJq6pwLp_DTOw0QXAGhi7IUPNLBDVied-SQw9-sk6WLUjMxcQpou9F_CC7ftFAces7ZCqQIIIxve4Od05gZnmQL8EKjj2bkrkNrTPBV9T2fRyosDneh5x3TQGpsFqWwOcg4n2gT-cGCf597oJPpyS3Xmh-p4bwC1-76tyA7lYcN7NGZvmNzQe78lY4OE06kFB6j1ZzX3BK61rsXMuLPycmIzPhafeJj8-CRGJhvnYjBXAWwDe1VhjWRgAeRiRjlT6OdHPvWHLeKwxvSPp3LXEuLZJxuLqvgfZJ6CVCY1iboeKher6rKQx1XOyKt79DscTNgQFa0v3juBe45Uu7pdvNVcxOk96cv-VhEkd6ZT31QxUzqkEmxkvnbOAvxM5PdktnZW0XHVKK4uZKiNS-JNzqlwZM9vZx0bfuNrRcscVeNKnCqG18n_TFpBFVucl7Kj1KijMpjLh4DNIZzqiU9dlqWqARJarbJLPBhOSQ1of_TBDP9Thz7hxmLCcOPnU2lEh1bZk-M8ElrPcULIK_k6V3f1T8fAPUVVeIv4087F5esqEz1DKpZydqIebZD4D6m2oZX528lMQEHV-TVAcPQyC7wVb1-9m6UyJgdwqemP85PpZ6X09GHUtpRp5y3jzJAIACyU8cETRYke0rMK0qAdTwe1R38L3SykWRAIPDWsJ4gf_5kea41QQrnPXp6aGQ0Kp60Ce7E3JV7beSaSMQof1n30a8lCLZnZ0XvkXwHsCFiA,,?data=QVyKqSPyGQwNvdoowNEPjbv5NDGkDw-huUpGHotaEKMse75hWj8mPCevkcbDX_pOUNFAtm0oxdNXb4smtyBUMfUtlk_aKp9bBHP6axSSBkKdz8Fv8iwCCyZxZGKbEhedBFCYrIhm4dnhb9kB_l8KNi3T-r-EQ2cE0QAIIL-6o10vDWX1c3Rza40ctx2ZZyWnaT16ObZmrggIUahcSYJqEF23O35113NRYSg09hk1Ctg6iI7dC4hNJ03R4PsjX2N2RiJfAoeAoWU5DF_-EQSB-jqlLLNQxObWkv_IsSYzVTxtgSSElT4_kcCzW5jMXuse7bXV4kZ535saelgujEuLDmbd4V5O3N-ymV-K4x4tzz1ETcpMxu-FOjqD3xcptM0NABZPikJz7F2ljHnf8HL8YdlFIRVu_dlEhsIynC3pXzS7SmvOXZaU6cvGlI7_mZWf0bj-rmGvZkmCTekpyLTB_mWQ47Pznxt5&b64e=1&sign=9bf78ca4ce9cf868d06ad79bead62881&keyno=1',
            },
            directUrl: 'https://topcomputer.ru/tovary/808903/?r1=yandex&utm_source=market.yandex.ru',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsP8n0LH1Gm8u0gmkwkJsLnaFrG9INTYm5zi7zwly-QIMCTHAKDLZD68XkMPswMEclgvxUQ0FrOm8NlEohAraZWw7FcR1Pc014w6PrVPvfko5eFbfJ_-irB356usz2Cn7xwhjW9r9D5UEmUNe64RDeshGaY6z0F9xqG3-D3YW4xzZFaj-QR5ilpuVxpYSkS1k0K3jPRn7dS9oEsSn4JfMaR829vu-rBdQn8O7ky4KpcEMaUdC8sP4GlqOTgAa47Nm5Gbit8Bxn0iygMOfHrOYUzf3kkvrtAGaLydAv1NDlCcwgzVfNfzgMq1JTkZiuPC9Rv4MsN4MOfWlZ11IJGWvfN8waAkUu7I2TQOKROeYARNxVJO2Y7wLVkQxwhkW4bObI9GsJjtrvVj9_Rkpf-i6koY5vcE-9d1kQhzVEN75BvtJd2RYneSuMILpZNI5wByqVjLIElc81UsL9n0EqmdG2O4rMAfoZBjx4qyKpqMQ8eeG7NYBsN5TTN4tbMOYRQit_oAiW-eN5j3QYpfqXddyw5K52dGQy1XqxAvmowd6hRUtd-sEC_1mIzuIXwa1QRiUe0o59zvLyiOPtWeirk-IPFWeVOZaHrOC6DDnY1JjnYSdjn0wqBo0L93fUcp2ZoHKmSlvVuEBEvg6ofPXKSyDr6Io-NqOJTxOQ71XMIy9jEokJF_HBaj1RlLiuUXWUnTNDOlHzGR517BK2oXioAx_ljhNo_Hwow9oq-ukueRa_fDYcVbAhoVZZU_Tx_vNZ-6A6QUddGoVfpi_D-1A6jwn6dzs-0puXMOwUD-yFW3tSOVLDuIDm57Lz9tlUk3OQX013Cw0RKwuc9ZPo4aO-2KDw0GanYVLXjvDvadJKvjSHo6U7P7XB6Pu-kQM9ImZvdIfdaxTlJtVIDXUWkqUq5D-RUGVkZpVwJQKvX3nf1drXuww,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYZlt_cYH9e6FFZhI2kR1t4EU5VErJw0zkE7Tlv55agrkTEkpv5U2LP_zH107ONft9wm-zFQ6lEi9j1hyQfeOxc,&b64e=1&sign=a00d9e1046160965c47af74a996a1c64&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRsP8n0LH1Gm8u0gmkwkJsLnaFrG9INTYm5zi7zwly-QIMCTHAKDLZD68XkMPswMEclgvxUQ0FrOm8NlEohAraZWw7FcR1Pc014w6PrVPvfko5eFbfJ_-irB356usz2Cn7xwhjW9r9D5UEmUNe64RDeshGaY6z0F9xqG3-D3YW4xzZFaj-QR5ilpuVxpYSkS1k0K3jPRn7dS9oEsSn4JfMaRie6BVLcICSPhmLahlIiKaxMI_FmjdfIj7hVaG3NVdMnive6UYFHkKpVyS2CNv5L9-Qk7MoE0C5OR--RD0qG29XTj6J2K8yciXHhOdv3pC20eSFnEp7Fi3ej8C6YSb0x51AsyzWOKDvGxCL_HjuN3qhqSfZbSRf3XE8Zd5Biy97YBFlrsapyij_Q9j1VoMtoOUhU5EZSOlLz0lKvckCcaOmuOks1E_2u0fu0RBelwWzgk6i9nh_zqU--1aOer3EZDscYogMfE0ezCygHuctZc-DSW9vi0vXoDxu5pYN2M6kPzPBcWA0tTmBZmjP0ZR1ZJJiBXRLtvcF4WxLCXboe7A_B79Vi--wD6paF-5-9fmrPzHudYTGfGLcfw3yLmzrLRJRZ9mvc0AdVxI_oAoM0d-apIbAI4zMVMqCeH58gL3-g4SzJIQBHU7VuQTkNEKnuuKFobvH5JB58lk3BcRLieZPrdIKGbHfOjv2GtjZSUbSl-NF1oy_dOJbFDwUwldypofEW8B3L-jDp3Lh6cr5K5_h6jI4H0mbUz6eKApf_cL6YRmRwrD7pFqeK48gqOfxJufy5MvQQ-KrDiJbwudm5J6HFGvBsXe_v1KyZiZlF4w-prh9PnUfedjypHxdo-RBWkxWIsuQDXRgVeFGFYPP_UH9HoCSWdqvQeldB5F6tSNxqQjnJW8X8vStphC7BSImLSQUoU9hqqTcbZY9bmBOmPGw,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYCCAOx4yDB091gW7KiISaDJNifCJUR91p7fFXwSFDwyIq8yNlLmXL0UdjS4l50fs5Cs2ioGLr-exwDETzcGTU0,&b64e=1&sign=f52dce4a51a1325503a0ddaf3e5199c5&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.4,
                    count: 24205,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1244,
                            percent: 7,
                        },
                        {
                            value: 2,
                            count: 403,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 547,
                            percent: 3,
                        },
                        {
                            value: 4,
                            count: 2004,
                            percent: 11,
                        },
                        {
                            value: 5,
                            count: 14814,
                            percent: 78,
                        },
                    ],
                },
                id: 5205,
                name: 'TopComputer.RU',
                domain: 'topcomputer.ru',
                registered: '2007-08-21',
                type: 'DEFAULT',
                returnDeliveryAddress:
                    'Москва, Гостиничный проезд, дом 4А, строение 1, Белое двухэтажное здание (Медицинский Центр), боковой вход (цокольное помещение), 127106',
                opinionUrl:
                    'https://market.yandex.ru/shop--topcomputer-ru/5205/reviews?pp=484&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 7696024,
            },
            outletCount: 14,
            pickupCount: 14,
            localStoreCount: 0,
            photo: {
                width: 493,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_qXkJNDFwzONBs_NlSgk1Mg/orig',
            },
            delivery: {
                price: {
                    value: '290',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameGenitive: 'России',
                        nameAccusative: 'Россию',
                    },
                    nameGenitive: 'Москвы',
                    nameAccusative: 'Москву',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameGenitive: 'России',
                        nameAccusative: 'Россию',
                    },
                    nameGenitive: 'Москвы',
                    nameAccusative: 'Москву',
                },
                brief: 'в Москву — 290 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 2,
                            daysTo: 4,
                            orderBefore: 24,
                        },
                        brief: '2-4 дня • 14 пунктов магазина',
                        outletCount: 14,
                    },
                ],
                inStock: false,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '290',
                            },
                            daysFrom: 2,
                            daysTo: 4,
                            orderBefore: 23,
                        },
                        brief: '2-4 дня при заказе до 23:00',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 91033,
                name: 'Внутренние жесткие диски',
                fullName: 'Внутренние жесткие диски',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '25066317',
                name: 'TopComputer.RU (м. Алтуфьево)',
                type: 'pickup',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                        },
                    },
                    rating: {
                        value: 4.4,
                        count: 24205,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан',
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1244,
                                percent: 7,
                            },
                            {
                                value: 2,
                                count: 403,
                                percent: 2,
                            },
                            {
                                value: 3,
                                count: 547,
                                percent: 3,
                            },
                            {
                                value: 4,
                                count: 2004,
                                percent: 11,
                            },
                            {
                                value: 5,
                                count: 14814,
                                percent: 78,
                            },
                        ],
                    },
                    id: 5205,
                    name: 'TopComputer.RU',
                    domain: 'topcomputer.ru',
                    registered: '2007-08-21',
                    type: 'DEFAULT',
                    returnDeliveryAddress:
                        'Москва, Гостиничный проезд, дом 4А, строение 1, Белое двухэтажное здание (Медицинский Центр), боковой вход (цокольное помещение), 127106',
                    opinionUrl:
                        'https://market.yandex.ru/shop--topcomputer-ru/5205/reviews?pp=484&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Алтуфьевское шоссе',
                    premiseNumber: '31',
                    fullAddress: 'Москва, Алтуфьевское шоссе, д. 31, стр. 1',
                    wing: '1',
                    note:
                        '3 этаж ТЦ Центр Торговли, по длинному коридору идете до 2-ого пролета справа, поворачиваете и идете до конца и налево.',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.867384,
                            longitude: 37.581523,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (499) 3220317',
                        sanitized: '74993220317',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '21:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/8bQre-P6Cx5xSrFdtsGgdQ?model_id=130084187&hid=91033&pp=484&clid=2210590&distr_type=4&cpc=BIq55zlrpNMLkJwJf6ZHcf-RE0eS2L1Agu_UpXjsVvDrUlvFXLstJObkOqZDJKvegCMERoc3OiQeoo7mu7cYjne7F5FB8UMKT7lfotowAWmLJgrQTFl8C1OY3GtlG7xi2BoCnGWZ1NA%2C&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            benefit: {
                type: 'cheapest',
                description: 'Хорошая цена от надёжного магазина',
                isPrimary: true,
            },
            trace: {
                factors: {
                    CATEG_CLICKS: 7172,
                    SHOP_CTR: 0.004233201034,
                    NUMBER_OFFERS: 57,
                },
                fullFormulaInfo: [
                    {
                        tag: 'CpcBuy',
                        name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                        value: '0.939432',
                    },
                ],
            },
            photos: [
                {
                    width: 493,
                    height: 701,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_qXkJNDFwzONBs_NlSgk1Mg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 175,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/210846/market_qXkJNDFwzONBs_NlSgk1Mg/190x250',
                },
            ],
        },
        {
            id: 'yDpJekrrgZG4U6T_UXiZIqrJd1VmBBGVrmXgXo4YhCsXiZIsLa0f8A',
            wareMd5: 'vc34TS2rUOVDT_ugjqULwQ',
            skuType: 'market',
            name: 'Жесткий диск 2Tb SATA-III Seagate Barracuda (ST2000DM008)',
            description: 'внутренний HDD, 3.5", 2000 Гб, SATA-III, 7200 об/мин, кэш - 256 Мб',
            price: {
                value: '4120',
            },
            cpa: false,
            url:
                'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrzZkohCeaChyF9Z7YhbYWz09j84WI6_TDMo1kABcE8v7Oy1w-75vfIP47com5wy55ViOTVKHOzEBMrjCpkgIlJAL6QybKxThYhVl3h3fIjJdbaA41KvskrSkJ9ncArCKxnONfbABoQryAUyLQJBXN0VFiNFJo3PT6k3DGnBsZbpkPXe0c57_R3aCTwoFuazVI798GCKktWh8RT03xAyru9IvFFvjXSIPJ0TA5FehI1RC0vBiNTR9H7a2GcPuNvxnGbD-iC99GtZ_1mRSaIkT4ZByB7XFyrNFThrweNM100n7_MTSDaxxuSXUZk5QDhcVeE79GxB9pjK0aTx94SUGlYQXf5Va43aB4JCbWXrxAP9z1NnXQTekRAaESMKDH8XyADayojOUn5D2U0eMK-rFf32SH1KeijP-6vuDtWRJVamdQibqDt1nI8g1lyAtPuLO2_4WMDGB5bwAkz-587Zo5dGGJCbtkMDkFmyz_N5HPyY3mcDo314DaRIeoHmBWj-3qrnxnK7h9sq5owG6ZOAK_bt7efbUEkYIlzZ6EIb2_MO6IzTojJqR9-jkD3jQYpNoaQerCPDLdM_2r_F161fgzppSIlgO2zuW23ukvuRVfC5gzZEUH6OvGDmgRRGwpJWpwfcoxG8NNczhvuW4-F6dSt6gKwUfg2mW_2usR8I4T9PSP1wxhPLTu9JiOMhLmRIwbkpLLXiA382a17VsgP_tvpvwE7vj6VcktsC4Bec-GwaoVbLsZU3G9lTcLNCXl37JzV-Vvaiv-YeXWH36vrpJ3Hq6TjejWGdTrYUKjyAnd1sWRlvNJS-6AEhkf-iHwilF-Y-TwMc9ZhJJQ_z4HkTGqbPCBoVTfuBRzbS-FFYmTgIZ9fsxcmD9qCoJkpz4HKbncDggMOIaZnY3w4QykoxCngnSXPFie8V3iA0KU7AOAyD?data=QVyKqSPyGQwNvdoowNEPjUBu8iKId2aDQiyPQ-PTLb8UmW59Y36X01h3WrLCTiANAy5OSVaeOdHyAdW15wMftFKr1Bxgh7cj7VCXpAZ6N8ML3mwUAYC7D2E3iNbWRFxslTqKbOQvU3QikTt-wGs67l1L8m64nAaR&b64e=1&sign=a3f34af18128f80e1d9be3eab1473ea7&keyno=1',
            urls: {
                484: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZrzZkohCeaChyF9Z7YhbYWz09j84WI6_TDMo1kABcE8v7Oy1w-75vfIP47com5wy55ViOTVKHOzEBMrjCpkgIlJAL6QybKxThYhVl3h3fIjJdbaA41KvskrSkJ9ncArCKxnONfbABoQryAUyLQJBXN0VFiNFJo3PT6k3DGnBsZbpkPXe0c57_R3aCTwoFuazVI798GCKktWh8RT03xAyru9IvFFvjXSIPJ0TA5FehI1RC0vBiNTR9H7a2GcPuNvxnGbD-iC99GtZ_1mRSaIkT4ZPcuYeHsfLKw9soJ9c7Q73H5hMzwagzZSRupGO7fAkNOohTbKG0oeXOt7WTfAaFABrGAf-9zeyVwEg6JzTwEA4ROC_BBShd0x3Ed0iiuWqOk1etuodS4jVPbJAg97xmHOD75TVBKm46chpECnd2uzYwOYP-ZF_eQQe7Sj8WRkBlOBqiKjA7kawmFqf0D2aBsjqIcQ-_7ojM2bOtV7SVihExacu21-5DtnbD66aSTgfYqLDT8PKyh8dtocC5wRcnRN1OhkaiqqjeLdJqx6qvK2NJfYp_hZTlwo_683hLVIk_4aiLux64zE7V95wd0nMKztmk4CSZb_4WKfY19w5L7CrV-AOHs7atw20flYKYHx2pc5pCxgw9cslK8OFadVB5OlANvEhkN8lb2ErTdfwbW5cAYrP3MBdDXEIhm5B4TJf7xWWIQe1pndKX3YFJa_OGPya_arVnAgNcQdafxWJxxay0cORIBZCCXCVpOaPcoq3K-0klC53zcVyGQ1ZXdxLOTMAYZbBdBSlpdCLKZ9UhhIFaHc05KoJbHrbvLM6utf3Vt_3dUuTejeM0EksC1skQd6zlde41kPmfaMaI9_oC2jHKRpzWSSabukAE11V1zWrOvtrbhpu6M4356V0gcIg-eq4FlPF7KPyoVMqFxe-EBJt?data=QVyKqSPyGQwNvdoowNEPjUBu8iKId2aDQiyPQ-PTLb8UmW59Y36X01h3WrLCTiANAy5OSVaeOdHyAdW15wMftFKr1Bxgh7cj7VCXpAZ6N8ML3mwUAYC7D2E3iNbWRFxslTqKbOQvU3QikTt-wGs67l1L8m64nAaR&b64e=1&sign=7968fac369032d1ae795b45dc6122921&keyno=1',
            },
            directUrl: 'https://www.regard.ru/catalog/tovar290048.htm',
            outletUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRs-qmWRU5fXDhFOh21Gnm0tI6m7ZVb-VK1ZK7BESUP6tByq8rLxgmR5yA3j0Xb70SDvA1r517Zd41F-QRmiKeJj_CT_Na3EeY8BdRururUaIDPUjup3EowXdcuTa92TWq1CX8awAgBJcFQTjQ1DzNBPalN9lRKdI3u31O29fmtiYXgcuVtQeVnGZxqIZnj-mxRfqtCh1PKve0qu7dNDPlkeZyrzP7VPha_ykTMvRG1LuCnxVYUo0RTMapGoaKWsOFodbfuYhSwfbXu804yW96MktQ0NEHZAKz4AVdUGU2EcxJZ07dN3i7aRUScn79fQz2ATNCBygQm7alA3_1NxJzHx4e-TFhiZSROf8J6aS6A6Bg6PUJP8fOo8UdqEctSC5jpLEnsZtoqgKbUkpK0Y_ET0B5sdzUeICmu3eraPmWSU7vNYggtdaDcF_fzzLB8cwT7P_XNPAQy5C7i8Rq_0M8oIx6D-oOKiZabOdmFG6qBVCeor2LCCkKITFyUBwm-0Um6q6cUvPiYEl3na5REjRDA1sUdXzZH4_2gJQXfRanqO6jeqrDfSRdMMSOSbcLgFKrs-rFWoQFDjFfBNxaOdltwv3whwb2FWqw5vMpkErWntoIBPFQ24qn7ItmoFOWTW1-saPxnLlOO-9ZN7wccUONl7L2SAd_MkSr8H3TZE13rOGBcZwWlOjOuPKpwMrowlN8gmYCQLoK1IeZq6zx65_1k0voZ5vTfrR_-cUyJShHhVm-lsMD7WHyj0P6qAOrxkBpzTXVmwdhjqYoAya1qU6H-GzvL1Hyvi2H6GjuuJ7Y5NxIcooYk8rhMTbGRRkM6xx1OyhZY1qXycdYNbrUuptUkSc9vTmLwsGbbWa60wNMvhulAQ-YBkIQkU2woR5hpVKSFUTvX--dky2kARqjFZsFXbzJqdiI5BuTyzl0QCwHApE08Ky3dwGepR?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI10wbNm7M-3rr2op627_hNRSPVo_5Pr1yuUbnkksbPcI-VxNfjKCT1WPlBgmt90JaDz2msboJ1LZCNcQd9np5_38,&b64e=1&sign=6b56f8ce78c7eeb6c2a6f9f37ee3fe79&keyno=1',
            pickupGeoUrl:
                'https://market-click2.yandex.ru/redir/338FT8NBgRs-qmWRU5fXDhFOh21Gnm0tI6m7ZVb-VK1ZK7BESUP6tByq8rLxgmR5yA3j0Xb70SDvA1r517Zd41F-QRmiKeJj_CT_Na3EeY8BdRururUaIDPUjup3EowXdcuTa92TWq1CX8awAgBJcFQTjQ1DzNBPalN9lRKdI3u31O29fmtiYXgcuVtQeVnGZxqIZnj-mxRfqtCh1PKve0qu7dNDPlkeZyrzP7VPha9nStYIfgZydIoKNmA11Y-tXsT3u7zRamR46vI3r1tyRrf17bjou4x0n30mbkmfeUEY_Feefdt8Bw9XPjlKRiuvxkf-nyCsVGQOzxvbDkCd0wv6rWh-T9QXBAxakrcGqL4QJrVR-5nZG4OvFyr6RfLjOeQTpjV4wqo7sbrYEBYTBFjYMjbPOFHY_7XN95fRAN3L5SdnAvaLuzA4cI1SgFpfkyvmW6sp12x83MljIPyDuYDWrBqrUhPZ1gHhA_kQs6AKMp3OlDvxaBKg8q2gQLG6zLeG-ycyEDICJGgOgsGXSNfvm5f9gj-pt24c5JT0bugDeU7Avw8RNKsu34bRH8AJRTSJcsWkaC7U7WHx5gj1uyEwwe2gTV9ElXZNbcM3FDODYhUQCQRTl74y42Z7fcv1njJJeQcv4edhI3bMnHvBNlJunSfTzZKthzGaYOXaz1CcJEgKY0htSgZWXq69qY89bAmdf3KCfj8CDwv5MHI-eEHz1RorND5e2rvllXdZmBqicP3806ETbqzGvqzK7Y_fXmCRrP2p0EPBa2lqMa1t2cfxPKE2XMdGzq2y2vtaovO2B1rCxv9BSYp524RPmfj2ebStIWv8NCYH8kw1AXEwzU8NmKZxpe4uPHxFpMP305VX8oUW4js7Uw8aHxc74BytnzwHB1Lfmx9s2mD8EJfpTuE67rBgTWgTbrT8pXg9cEXCclzdVZbgkMMGXq-VSxf-?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2YGb3fP5B4xTkNHpG3WolK9O1HuUqKtI1w9NSBQTcQhx0sgVplGnjb7fZPNdR26wTbd4b6DtHFUR_tRNKWBmd80tFQK_ZWCLYTqDlNGNwdp_HFc4hVXkDR8,&b64e=1&sign=ef44750ed2c1a2fef62b034b6cc2e3eb&keyno=1',
            shop: {
                region: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                    },
                },
                rating: {
                    value: 4.6,
                    count: 26650,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 1400,
                            percent: 7,
                        },
                        {
                            value: 2,
                            count: 437,
                            percent: 2,
                        },
                        {
                            value: 3,
                            count: 393,
                            percent: 2,
                        },
                        {
                            value: 4,
                            count: 1564,
                            percent: 8,
                        },
                        {
                            value: 5,
                            count: 16883,
                            percent: 82,
                        },
                    ],
                },
                id: 4398,
                name: 'Регард',
                domain: 'regard.ru',
                registered: '2007-03-02',
                type: 'DEFAULT',
                returnDeliveryAddress:
                    'Москва, Волгоградский проспект, дом 21, Подъезд 9. ПН -ПТ 10 - 18, СБ - 10 - 17, ВС - Выходной, 109316',
                opinionUrl: 'https://market.yandex.ru/shop--regard/4398/reviews?pp=484&clid=2210590&distr_type=4',
                outlets: [],
            },
            model: {
                id: 130084187,
            },
            onStock: true,
            outletCount: 1,
            pickupCount: 1,
            localStoreCount: 0,
            photo: {
                width: 681,
                height: 995,
                url: 'https://avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/orig',
            },
            delivery: {
                price: {
                    value: '290',
                },
                free: false,
                deliveryIncluded: false,
                carried: true,
                pickup: true,
                downloadable: false,
                localStore: false,
                localDelivery: true,
                shopRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameGenitive: 'России',
                        nameAccusative: 'Россию',
                    },
                    nameGenitive: 'Москвы',
                    nameAccusative: 'Москву',
                },
                userRegion: {
                    id: 213,
                    name: 'Москва',
                    type: 'CITY',
                    childCount: 14,
                    country: {
                        id: 225,
                        name: 'Россия',
                        type: 'COUNTRY',
                        childCount: 10,
                        nameGenitive: 'России',
                        nameAccusative: 'Россию',
                    },
                    nameGenitive: 'Москвы',
                    nameAccusative: 'Москву',
                },
                brief: 'в Москву — 290 руб., возможен самовывоз',
                pickupOptions: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '0',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                            orderBefore: 24,
                        },
                        brief: 'завтра • 1 пункт магазина',
                        outletCount: 1,
                    },
                ],
                inStock: true,
                global: false,
                post: false,
                options: [
                    {
                        service: {
                            id: 99,
                            name: 'Собственная служба доставки',
                        },
                        conditions: {
                            price: {
                                value: '290',
                            },
                            daysFrom: 1,
                            daysTo: 1,
                        },
                        brief: 'завтра',
                    },
                ],
                deliveryPartnerTypes: [],
            },
            category: {
                id: 91033,
                name: 'Внутренние жесткие диски',
                fullName: 'Внутренние жесткие диски',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPC',
                viewType: 'LIST',
            },
            warranty: true,
            recommended: false,
            isFulfillment: false,
            outlet: {
                id: '161345',
                name: 'Регард (Волгоградский пр-т)',
                type: 'pickup',
                shop: {
                    region: {
                        id: 213,
                        name: 'Москва',
                        type: 'CITY',
                        childCount: 14,
                        country: {
                            id: 225,
                            name: 'Россия',
                            type: 'COUNTRY',
                            childCount: 10,
                        },
                    },
                    rating: {
                        value: 4.6,
                        count: 26650,
                        status: {
                            id: 'ACTUAL',
                            name: 'Рейтинг нормально рассчитан',
                        },
                        distribution: [
                            {
                                value: 1,
                                count: 1400,
                                percent: 7,
                            },
                            {
                                value: 2,
                                count: 437,
                                percent: 2,
                            },
                            {
                                value: 3,
                                count: 393,
                                percent: 2,
                            },
                            {
                                value: 4,
                                count: 1564,
                                percent: 8,
                            },
                            {
                                value: 5,
                                count: 16883,
                                percent: 82,
                            },
                        ],
                    },
                    id: 4398,
                    name: 'Регард',
                    domain: 'regard.ru',
                    registered: '2007-03-02',
                    type: 'DEFAULT',
                    returnDeliveryAddress:
                        'Москва, Волгоградский проспект, дом 21, Подъезд 9. ПН -ПТ 10 - 18, СБ - 10 - 17, ВС - Выходной, 109316',
                    opinionUrl: 'https://market.yandex.ru/shop--regard/4398/reviews?pp=484&clid=2210590&distr_type=4',
                    outlets: [],
                },
                address: {
                    regionId: 213,
                    locality: 'Москва',
                    thoroughfare: 'Волгоградский проспект',
                    premiseNumber: '21',
                    fullAddress: 'Москва, Волгоградский проспект, д. 21',
                    note: 'подъезд 9',
                    geoPoint: {
                        coordinates: {
                            latitude: 55.72641414,
                            longitude: 37.68936239,
                        },
                    },
                },
                phones: [
                    {
                        number: '+7 (495) 9214158',
                        sanitized: '74959214158',
                    },
                ],
                schedule: [
                    {
                        daysFrom: '1',
                        daysTill: '1',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '2',
                        daysTill: '2',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '3',
                        daysTill: '3',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '4',
                        daysTill: '4',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '5',
                        daysTill: '5',
                        from: '10:00',
                        till: '21:00',
                    },
                    {
                        daysFrom: '6',
                        daysTill: '6',
                        from: '10:00',
                        till: '18:00',
                    },
                ],
            },
            link:
                'https://market.yandex.ru/offer/vc34TS2rUOVDT_ugjqULwQ?model_id=130084187&hid=91033&pp=484&clid=2210590&distr_type=4&cpc=b8yaLcHUcQ60oPn-qzUTqLwno65pjziIm56i659w69jNocNTxGAeJmBTXHoztoZV_NOqjPZbXvPriVEy4vJwwGwCZWmyVy6oc3gW5wJabCLxuKFYiNJZeoeI7yCnXdqBE6zn7p0BH89KyzQZmTpVGw%2C%2C&lr=213',
            paymentOptions: {
                canPayByCard: false,
            },
            isAdult: false,
            restrictedAge18: false,
            isPremium: true,
            benefit: {
                type: 'premium',
                description: 'Хорошая цена от надёжного магазина',
                isPrimary: true,
            },
            trace: {
                factors: {
                    CATEG_CLICKS: 7172,
                    SHOP_CTR: 0.005628393963,
                    NUMBER_OFFERS: 57,
                },
                fullFormulaInfo: [
                    {
                        tag: 'CpaBuy',
                        name: 'MNA_DefaultOffer_2680',
                        value: '0.141278',
                    },
                    {
                        tag: 'CpcClick',
                        name: 'MNA_HybridAuctionCpcCtr2430',
                        value: '0.0487102',
                    },
                ],
            },
            photos: [
                {
                    width: 681,
                    height: 995,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/orig',
                },
            ],
            previewPhotos: [
                {
                    width: 171,
                    height: 250,
                    url: 'https://avatars.mds.yandex.net/get-marketpic/1715186/market_erqXpB_EO3VDcAPDuImlhg/190x250',
                },
            ],
        },
    ],
    filters: [
        {
            id: '-17',
            name: 'Рекомендуется производителем',
            type: 'BOOLEAN',
            values: [],
        },
        {
            id: '-9',
            name: 'Наличие скидки',
            type: 'BOOLEAN',
            values: [],
        },
    ],
    link:
        'https://market.yandex.ru/product--zhestkii-disk-seagate-st2000dm008/130084187/offers?pp=484&clid=2210590&distr_type=4',
    geoLink:
        'https://market.yandex.ru/product--zhestkii-disk-seagate-st2000dm008/130084187/geo?pp=484&clid=2210590&distr_type=4',
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
