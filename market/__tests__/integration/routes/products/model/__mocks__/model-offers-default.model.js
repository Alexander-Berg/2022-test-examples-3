/* eslint-disable max-len */

const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/[0-9]+\/offers\/default/;

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
        id: '1572131633816/b3365dfe2faa046ba1e4926bd8950500',
        time: '2019-10-27T02:13:53.88+03:00',
        marketUrl: 'https://market.yandex.ru?pp=483',
    },
    offer: {
        id: 'yDpJekrrgZEbICgC7ODNaRLi6S5UC_kXwvwo-_DGWOqkB9SuzFAWmQ',
        wareMd5: 'AmNSpxXIdeXEFdMutOlYSA',
        sku: '100307940935',
        skuType: 'market',
        name: 'Яндекс.Станция - умная колонка для умного дома, фиолетовая',
        modelAwareTitle: 'Яндекс.Станция - умная колонка для умного дома, фиолетовая',
        description: '',
        price: {
            value: '10990',
        },
        promocode: true,
        cpa: false,
        url:
            'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZhrE8Jd6czUMh69UmHqOqi1h0yuYmzpjL94jYca6HwSoR18t3_Z8BcoEyrDCdfc-f_hXqJHTDA-o_pAh21tWT9Q3Cl1UAbA09AEvhV9vasTdx6UH0db7KQF0K6aqNQZGYL3PTFazvOYoodp5U8N0JjQYda1qU5relJNBbKDJs4HQw9zsSeA5rEsRLxJiMSiDwUMmuKiwvm7hQyrfXKMXjqJBGmB8jf2bIQrKo6fiPnNpc9emeMXvzIYUtgJZAVN1HKOCbTTbHgZuQH_mYGwagvdiSZ0luJfD0iWVXxjxmxI_XuKN-i3ivAqitf049E3vd80q2KQotjrapO4JCTNF8FN_fADiXf4lYY72lc5jM4K812kCxLbxnZ0h6OU4VGAdz4PzSiTzWFiN9yEjlvuXOKL68Mg1B_Y7JX2FIO_uVV3xAXUqVKHxfejVeF8JctkpHa-DHHFPDj62yqkacxtolo9MU75jAuP7w4TVgo6Zvhojidjaiiz37rppyYhYxeSeaF6vjviRbXntnmVQB2RogkCJprukozrXJL_-m3ifnvXVH_QPopXOtohKneUXx0bUubdcRdgJtD9gKHD_PPxbIFXX9W--QLcTVABCH5oWMSWcL8Q1NcA-ie3gwQPaBWnwZ79DgyExsFhVucB3IB8S0dOP5xTIztzf9hG6dbV4nwFXNQYfeQZRqa0xve-OzCIAx9Gpf4yIqJyOWbvcPGvXS1lJKx4z3EJU_a24CkZzJz7CGDo3ycvCbPO6Hf0Hf3vIJVgxVstb9XGXwMeuwGVTrg2_4L6u_CQk8OwTiXNLTHrMK_wGCwx29XMkQTkfnXdXtCbF2aCEzC45CV22twRptafDMrKMZBaZ3_-WI9TaHo1gXXQ6wsi4CK6zFS8cBXPjooJcZ_76mz90aNLEKYbBg8beaBzFjtH6vsrZIFPkbGdwU7BUowISWAGZX1XEelpkVQ,,?data=QVyKqSPyGQwNvdoowNEPjTmsCE1SehLrFONw22P-t5Qwt9LO3xjAA6yuf1vMaOj1Ilu_e5YBL6O0Ub5b9nkTckwoQS8YWjXb52Cz7fc2Nc_tVOpczw85xbuDAarbk8xVZzTL80wFKUgSUrgbfzgQeS6spgqTpdQ25RVmBenvB6CD24oqHQb40SPN3-u-v5THxoY5VbZQxFhlzeyKUxMvTaHQqcn_eSUKOq978-Fh46LsiGFO3KfBGqXtxfmnvMj_1ouYqUCdKrvHH1I05Fu8H8DQjRI2kU41_lh73DAlWq9UarCmODjKVWgi4DALqt5ISa1rBPKchYlRunJ4L7r4vQ,,&b64e=1&sign=28307e7b7a85f1382f968b5b233817f2&keyno=1',
        urls: {
            483: 'https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZhrE8Jd6czUMh69UmHqOqi1h0yuYmzpjL94jYca6HwSoR18t3_Z8BcoEyrDCdfc-f_hXqJHTDA-o_pAh21tWT9Q3Cl1UAbA09AEvhV9vasTdx6UH0db7KQF0K6aqNQZGYL3PTFazvOYoodp5U8N0JjQYda1qU5relJNBbKDJs4HQw9zsSeA5rEsRLxJiMSiDwUMmuKiwvm7hQyrfXKMXjqJBGmB8jf2bIQrKo6fiPnNpc9emeMXvzIYUtgJZAVN1HKOCbTTbHgZuQH_mYGwagvdiSZ0luJfD0iWVXxjxmxI_XuKN-i3ivAqitf049E3vd80q2KQotjrapO4JCTNF8FN_fADiXf4lYY72lc5jM4K812kCxLbxnZ0h6OU4VGAdz4PzSiTzWFiN9yEjlvuXOKL68Mg1B_Y7JX2FIO_uVV3xAXUqVKHxfejVeF8JctkpHa-DHHFPDj62yqkacxtolo9MU75jAuP7w4TVgo6Zvhojidjaiiz37rppyYhYxeSeaF6vjviRbXntnmVQB2RogkCJprukozrXJL_-m3ifnvXVH_QPopXOtohKneUXx0bUubdcRdgJtD9gKHD_PPxbIFXX9W--QLcTVABCH5oWMSWcL8Q1NcA-ie3gwQPaBWnwZ79DgyExsFhVucB3IB8S0dOP5xTIztzf9hG6dbV4nwFXNQYfeQZRqa0xve-OzCIAx9Gpf4yIqJyOWbvcPGvXS1lJKx4z3EJU_a24CkZzJz7CGDo3ycvCbPO6Hf0Hf3vIJVgxVstb9XGXwMeuwGVTrg2_4L6u_CQk8OwTiXNLTHrMK_wGCwx29XMkQTkfnXdXtCbF2aCEzC45CV22twRptafDMrKMZBaZ3_-WI9TaHo1gXXQ6wsi4CK6zFS8cBXPjooJcZ_76mz90aNLEKYbBg8beaBzFjtH6vsrZIFPkbGdwU7BUowISWAGZX1XEelpkVQ,,?data=QVyKqSPyGQwNvdoowNEPjTmsCE1SehLrFONw22P-t5Qwt9LO3xjAA6yuf1vMaOj1Ilu_e5YBL6O0Ub5b9nkTckwoQS8YWjXb52Cz7fc2Nc_tVOpczw85xbuDAarbk8xVZzTL80wFKUgSUrgbfzgQeS6spgqTpdQ25RVmBenvB6CD24oqHQb40SPN3-u-v5THxoY5VbZQxFhlzeyKUxMvTaHQqcn_eSUKOq978-Fh46LsiGFO3KfBGqXtxfmnvMj_1ouYqUCdKrvHH1I05Fu8H8DQjRI2kU41_lh73DAlWq9UarCmODjKVWgi4DALqt5ISa1rBPKchYlRunJ4L7r4vQ,,&b64e=1&sign=28307e7b7a85f1382f968b5b233817f2&keyno=1',
        },
        directUrl:
            'https://beru.ru/product/100307940935?offerid=AmNSpxXIdeXEFdMutOlYSA&utm_source=market&utm_medium=cpc&utm_term=510689.YNDX-0001P&utm_content=15553892&clid=910',
        outletUrl:
            'https://market-click2.yandex.ru/redir/338FT8NBgRtpJBUQNahCONWYY04-O1H08lY4avmBJRfKrKEGd8iC7lS1aUs2lU2HST_WpvwdGe9pi1mvBjcebpmrZJUcULN8mCjvP_3tp2oJbhvfr2BGoS9vLIptk68JI-7r-Q6GB_FK1a1M_XzZXQBRF93y6A7alGuDoaxauWlOlkZaK7pQ1Pk-_Vyp-QG-kl8jK0wCbjfkfO1yAoLcQIFfDjuuH7JFwuWpUnrXCfoDejTpYUkUrPjDs1g9-qvuElGvFFi2zB_4lNBPsWc7if7aIc6GFwJgWroTCOrH2FWatGW3YUUwLpx8rwa6yInxEXd9psorCLW0fXnMagEXqokvlTsOXiEfB1_USj6_t2cgN_Z9c02oA6NV5ImOQxgsumHH1yglzzlENMO8OjPQp0_DgxBl7usOw7hFNQAV0i_rPTZ-Gg4JFviB0mB6ezHmWH4nzsgyb6VMMjg-9ia5CYLVefkN4QyRsnGRhmhljRe37sWH4b-yrzY2JPKllYZZDrqyrPj8RGYULmDdkWI_usjVP-kKDFPCSQ6qLa53eJJVxU6M8aryMSulEH-uL3mOzwzBkru1iWQJzT0k1bG68UMqvOf3_unmh_TfRJq4RFKtTPo-52Z84X2Zj7DyqVPK9Ch11KrddkGASoELNIzPAjrbmOvZHGT_yeERkiOqtlwHqSROCdVM0fYvFLn8M-Bi82Zqd48vsK7Rp_PMqlGqXsBReGaOxzzzTjiUIvkKSBJNZAdurMmnNZZnDAyg38oy89xs3C1qFz0RRPM48OFeKSdZSuxjU1L4bh9nytL6QX7glIdFQOuUFBBJfErNFIQe_e7oCzn_eVB076MsDlXD-EYBFNobQPqnJKbbrqpccIyKG8MejfoyvaTguq7T6-c3rlEPV8VddTvxZbAzrLqrtjIvQaET4Pgs6xlhrpq7wKVAiiT5Iey4H5xUfkZZDA9tdn83PyESEWBmxrewjA1QbA,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QjauuqqjgWXmB1g-c1jURP7nmKxN44uZnh3ZZ0vwCrUFc_OoqqSiJ0OtowimPf98SZPAhEVWHa69QjbZ8u8-dEbVVVpfJiB7LS99tTMoy51G5qzRawUYwo,&b64e=1&sign=a80445ff4e96eb7742e33add74f4536a&keyno=1',
        pickupGeoUrl:
            'https://market-click2.yandex.ru/redir/338FT8NBgRtpJBUQNahCONWYY04-O1H08lY4avmBJRfKrKEGd8iC7lS1aUs2lU2HST_WpvwdGe9pi1mvBjcebpmrZJUcULN8mCjvP_3tp2oJbhvfr2BGoS9vLIptk68JI-7r-Q6GB_FK1a1M_XzZXXTxvy4YnVQCUv0vT5JW7ynkeE9W0_0oKnZYAiDhl7Owd2p4_iyO0NcoJkpkKNHl4mddPo0arKmSW9nBJW1NV8Z267uWIl8XmcOa9rddxvUhoOxmt55XQhuTO3qrUsUpUFhcrx8hZkO1r2wZEKCu2HJfjTP6FFA6ggLBmVckw40WBQpKhJdobdvta7oLgt8laj-2k5VqWoDXv1Ga3mvSLtrF9sp1BQNGqzT9Qgx21ayDl81djKmOVf9Uz5A3N9HTLGFqjtT097IZ0wahuxqnn7KURoeBvhrBRuC-y4aYBPlbtXifZtDeoSQ9cr9eUBQiBZ82YnZPnqNx_WHr9bLkHQiGFZCg7YevRQZ2pu7XuXIDdfyZrWDGIH6Tply-Cok_Hl1959n99-P4RYqBzApPcXxSRSEx4jhFBnX0kH15vBeHJUnA6uj3Lrd9vnmRB3ModvBq4q9WPX19vFCrnYBVoEAVlXcRxmjoTC-DVZeRVVgaC1oYJJb1OScr6KhKSKtHDro-TIh8BIOp2wP-cJfTw4XFr8m34Z-McHwEt0MpuqSxuoTogdjeOpMvi-fM4uBQUCeOC6cM6J5DCCvrL-y_u8GECMpCWYdDiNYi2QmL0DexMA4UAUBh-87i-_lrAZvkKqqL_hGJSTJGbyVWgCv8Cyy3qIjbzcmQZwBvbqi6I_a4dw8D7gT7Bj5twwuiDZeeuWZOKLXa_fBwPUStxE-6ZVJGhShPjUjy53QN6FeGNbtVHAQqzp5gyvkNG5b0FP8pZndaylIcc8waOsMkRIc2_iRPIBTSQ14QTEd0WbwQNtJzf83iqNAJv0Ntp3Q54_9LpQ,,?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2QjauuqqjgWXmB1g-c1jURP7nmKxN44uZi9UJ5jg0RDQDCNS-UEi_sulopRiredlO5XD_4jkb3QrfUg8dMqQnKbvL6PIU2GHmDgGLM0VzUUEHD-0p0cViuk,&b64e=1&sign=d4625633feb676ec480a25bc45636c90&keyno=1',
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
                value: 4.1,
                count: 53192,
                status: {
                    id: 'ACTUAL',
                    name: 'Рейтинг нормально рассчитан',
                },
                distribution: [
                    {
                        value: 1,
                        count: 4150,
                        percent: 18,
                    },
                    {
                        value: 2,
                        count: 1281,
                        percent: 5,
                    },
                    {
                        value: 3,
                        count: 1581,
                        percent: 7,
                    },
                    {
                        value: 4,
                        count: 2870,
                        percent: 12,
                    },
                    {
                        value: 5,
                        count: 13634,
                        percent: 58,
                    },
                ],
            },
            id: 431782,
            name: 'БЕРУ',
            domain: 'beru.ru',
            registered: '2017-08-14',
            type: 'DEFAULT',
            returnDeliveryAddress: '140961, АСЦ Подольска, А/Я 171',
            opinionUrl: 'https://market.yandex.ru/shop--beru/431782/reviews?pp=483',
            outlets: [],
        },
        model: {
            id: 1971204201,
        },
        onStock: true,
        outletCount: 757,
        pickupCount: 757,
        localStoreCount: 0,
        photo: {
            width: 478,
            height: 701,
            url: 'https://avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/orig',
        },
        delivery: {
            price: {
                value: '0',
                base: '249',
                discountType: 'THRESHOLD',
            },
            free: true,
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
                    nameAccusative: 'Россию',
                    nameGenitive: 'России',
                },
                nameAccusative: 'Москву',
                nameGenitive: 'Москвы',
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
                    nameAccusative: 'Россию',
                    nameGenitive: 'России',
                },
                nameAccusative: 'Москву',
                nameGenitive: 'Москвы',
            },
            brief: 'в Москву — бесплатно, возможен самовывоз',
            pickupOptions: [
                {
                    service: {
                        id: 198,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 1,
                        daysTo: 1,
                        orderBefore: 20,
                    },
                    brief: 'завтра при заказе до 20:00 • 1 пункт',
                    outletCount: 1,
                },
                {
                    service: {
                        id: 162,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 1,
                        daysTo: 1,
                        orderBefore: 20,
                    },
                    brief: 'завтра при заказе до 20:00 • 30 пунктов',
                    outletCount: 30,
                },
                {
                    service: {
                        id: 106,
                        name: 'Boxberry',
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 1,
                        daysTo: 2,
                        orderBefore: 18,
                    },
                    brief: '1-2 дня при заказе до 18:00 • 354 пункта, BOXBERRY',
                    outletCount: 354,
                },
                {
                    service: {
                        id: 1003937,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 2,
                        daysTo: 2,
                        orderBefore: 20,
                    },
                    brief: '2&nbsp;дня при заказе до 20:00 • 213 пунктов',
                    outletCount: 213,
                },
                {
                    service: {
                        id: 51,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 3,
                        daysTo: 3,
                        orderBefore: 21,
                    },
                    brief: '3&nbsp;дня при заказе до 21:00 • 156 пунктов',
                    outletCount: 156,
                },
                {
                    service: {
                        id: 1003937,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '99',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 3,
                        daysTo: 3,
                        orderBefore: 20,
                    },
                    brief: '3&nbsp;дня при заказе до 20:00 • 3 пункта',
                    outletCount: 3,
                },
            ],
            inStock: true,
            global: false,
            post: true,
            postStats: {
                minDays: 5,
                maxDays: 7,
                minDate: '2019-11-01',
                maxDate: '2019-11-03',
                minPrice: {
                    value: '0',
                },
                maxPrice: {
                    value: '0',
                },
            },
            options: [
                {
                    service: {
                        id: 179,
                    },
                    conditions: {
                        price: {
                            value: '0',
                            base: '249',
                            discountType: 'THRESHOLD',
                        },
                        daysFrom: 1,
                        daysTo: 1,
                        orderBefore: 18,
                    },
                    brief: 'завтра при заказе до 18:00',
                },
            ],
            deliveryPartnerTypes: ['YANDEX_MARKET'],
        },
        category: {
            id: 15553892,
            name: 'Умные колонки',
            fullName: 'Умные колонки',
            type: 'GURU',
            link: 'https://market.yandex.ru/catalog--umnye-kolonki/15553892/list?hid=15553892&onstock=1&pp=483',
            childCount: 0,
            advertisingModel: 'CPC',
            viewType: 'GRID',
        },
        warranty: false,
        recommended: false,
        isFulfillment: true,
        outlet: {
            id: '90321475',
            name: 'Фирменные постаматы Беру',
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
                    value: 4.1,
                    count: 53192,
                    status: {
                        id: 'ACTUAL',
                        name: 'Рейтинг нормально рассчитан',
                    },
                    distribution: [
                        {
                            value: 1,
                            count: 4150,
                            percent: 18,
                        },
                        {
                            value: 2,
                            count: 1281,
                            percent: 5,
                        },
                        {
                            value: 3,
                            count: 1581,
                            percent: 7,
                        },
                        {
                            value: 4,
                            count: 2870,
                            percent: 12,
                        },
                        {
                            value: 5,
                            count: 13634,
                            percent: 58,
                        },
                    ],
                },
                id: 431782,
                name: 'БЕРУ',
                domain: 'beru.ru',
                registered: '2017-08-14',
                type: 'DEFAULT',
                returnDeliveryAddress: '140961, АСЦ Подольска, А/Я 171',
                opinionUrl: 'https://market.yandex.ru/shop--beru/431782/reviews?pp=483',
                outlets: [],
            },
            address: {
                regionId: 213,
                locality: 'Москва',
                thoroughfare: 'Зелёный проспект',
                premiseNumber: '54А',
                fullAddress: 'Москва, Зелёный проспект, д. 54А',
                note: 'Постамат расположен в отделении Сбербанка, в зоне ожидания для клиентов',
                geoPoint: {
                    coordinates: {
                        latitude: 55.75201,
                        longitude: 37.81275,
                    },
                },
            },
            phones: [
                {
                    number: '+7 (800) 2342712',
                    sanitized: '78002342712',
                },
            ],
            schedule: [
                {
                    daysFrom: '1',
                    daysTill: '1',
                    from: '08:30',
                    till: '19:30',
                },
                {
                    daysFrom: '2',
                    daysTill: '2',
                    from: '08:30',
                    till: '19:30',
                },
                {
                    daysFrom: '3',
                    daysTill: '3',
                    from: '08:30',
                    till: '19:30',
                },
                {
                    daysFrom: '4',
                    daysTill: '4',
                    from: '08:30',
                    till: '19:30',
                },
                {
                    daysFrom: '5',
                    daysTill: '5',
                    from: '08:30',
                    till: '19:30',
                },
                {
                    daysFrom: '6',
                    daysTill: '6',
                    from: '09:30',
                    till: '18:00',
                },
            ],
        },
        link:
            'https://beru.ru/product/100307940935?offerid=AmNSpxXIdeXEFdMutOlYSA&hid=15553892&pp=483&cpc=e3Yw5eplQuXHNiIaqhfThlsauYmc11hApumJ3cMT89Tj1SH-t0VFLIaU94Nfhdvi44od_211XPWkNUddwRvbUnHcuWJ3ruG3fqt0Zsb4TiOQ3LFdWWmnl0kL8-Lo0Vu3i3uZ2_yU5WMugTwjrKt-6w%2C%2C&lr=213',
        paymentOptions: {
            canPayByCard: false,
        },
        isAdult: false,
        restrictedAge18: false,
        benefit: {
            type: 'default',
            description: 'Хорошая цена от надёжного магазина',
            isPrimary: true,
        },
        trace: {
            fullFormulaInfo: [
                {
                    tag: 'CpcBuy',
                    name: 'MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter',
                    value: '0.858633',
                },
            ],
        },
        spasibo: {
            receive: {
                points: 109,
                percent: 1,
            },
        },
        photos: [
            {
                width: 478,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/orig',
            },
            {
                width: 701,
                height: 607,
                url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id4110796139450667876.jpeg/orig',
            },
            {
                width: 701,
                height: 644,
                url: 'https://avatars.mds.yandex.net/get-mpic/1554397/img_id865969275187870419.jpeg/orig',
            },
            {
                width: 701,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1525215/img_id3941069550074879601.jpeg/orig',
            },
            {
                width: 701,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1545401/img_id4755552011311305408.jpeg/orig',
            },
            {
                width: 594,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1715213/img_id1475683773834324617.jpeg/orig',
            },
            {
                width: 701,
                height: 652,
                url: 'https://avatars.mds.yandex.net/get-mpic/1865974/img_id4148613522769632485.jpeg/orig',
            },
            {
                width: 579,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1767083/img_id8085967701331104764.jpeg/orig',
            },
            {
                width: 654,
                height: 701,
                url: 'https://avatars.mds.yandex.net/get-mpic/1603927/img_id7399494570172033254.jpeg/orig',
            },
        ],
        manufactCountries: [
            {
                id: 134,
                name: 'Китай',
                type: 'COUNTRY',
                childCount: 33,
                country: {
                    id: 134,
                    name: 'Китай',
                    type: 'COUNTRY',
                    childCount: 33,
                    nameAccusative: 'Китай',
                    nameGenitive: 'Китая',
                },
                nameAccusative: 'Китай',
                nameGenitive: 'Китая',
            },
        ],
        cargoTypes: [40, 200],
    },
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
