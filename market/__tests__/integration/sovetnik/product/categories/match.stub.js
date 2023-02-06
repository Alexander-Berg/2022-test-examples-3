/* eslint-disable max-len */

'use strict';

const deliverySis = expect.objectContaining({
    delivery: {
        active: expect.any(Boolean),
        deliveryText: expect.any(String),
        items: expect.arrayContaining([
            {
                priceText: expect.any(String),
                timeText: expect.any(String),
            },
            {
                priceText: expect.any(String),
                timeText: expect.any(String),
            },
        ]),
        name: expect.any(String),
    },
    pickup: {
        items: expect.arrayContaining([
            {
                priceText: expect.any(String),
                timeText: expect.any(String),
            },
        ]),
        name: expect.any(String),
        pickupText: expect.any(String),
    },
});

module.exports = expect.objectContaining({
    cbirMinOfferPrice: undefined,
    model: {
        name: 'Apple iPhone X 256GB',
        reviewsCount: 21,

        rating: 3.5,
        gradeCount: 88,
        prices: {
            max: 123990,
            min: 80720,
            avg: 85000,
            curCode: 'RUR',
            curName: 'руб.',
        },
        offersCount: 190,
        mainPhoto: {
            url: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/4hq',
            width: 321,
            height: 620,
        },
        isNew: false,
        photo: 'https://avatars.mds.yandex.net/get-mpic/397397/img_id7051974271832358544.png/4hq',
        urls: {
            model: '<wrapped url>',
            modelPicture: '<wrapped url>',
            offers: '<wrapped url>',
            map: '<wrapped url>',
            price: '<wrapped url>',
            reviews: '<wrapped url>',
            reviewsBadge: '<wrapped url>',
        },
        links: {
            constants: {
                allPrices: 'Все цены',
                allPricesMarket: 'Все цены на Яндекс.Маркете',
                closestShops: 'Магазины рядом',
                shopsOnMap: 'Предложения магазинов на карте',
                reviews: 'Отзывы',
                reviewsMarket: 'Отзывы на Яндекс.Маркете',
            },
        },
        info: {
            constants: {
                atMarket: 'на Яндекс.Маркете',
                avgPrice: 'Средняя цена',
                avgPriceGraph: 'График средней цены на',
                ratingTooltipText: 'Рейтинг товара %s из 5',
            },
        },
    },
    feedback: {
        constants: {
            closeButtonText: 'Закрыть',
            error: 'Комментарий не может быть пустым.',
            placeholder:
                'Пожалуйста, расскажите, что именно случилось. Ваш комментарий поможет нам исправить проблему быстрее.',
            submitButtonText: 'Отправить',
            text: 'Ваше сообщение поможет улучшить Советника',
            thanks: 'Спасибо!',
            title: 'Спасибо, мы получили ваш отчёт об ошибке',
        },
    },
    offers: [
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Apple iPhone X 256GB Silver (A1902)',
            price: {
                value: 80990,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'pricebar',
            shopInfo: {
                name: '1CLICK',
                id: 262,
                rating: 5,
                gradeTotal: 5004,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 2,
                daysTo: 2,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig',
            source: 'Market',
            bigPhoto: {
                width: 183,
                height: 350,
                url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig',
            },
            id:
                'yDpJekrrgZHKK5VJLcLldAwa23oMqFsh5yChP7WCs0v5A2-WiSXd2jBBIm1cNSb72YYhPxrX9rMPpp238EFButKDEemH45VDN8hsmkZNLNNumgNNqlARyTsCTHZt4YM8aSLm8WPyLzynjNuSAmZk9Bldw3aUrX3tBRk62PYINIvy8pp5JnhDRHKZx8xPOS2HxT7_dzLEQfPO6QUVzf_fWnTnn2GHceXO0MDEeqTz0pYtMoo-be8H2oIIG7vluF8EUZ5UpvZHjNhVheQ4nbfxkT_Fh8yha2EgRy_vzMggDU3EnpxHHuEm_gsmv4qnNP6G',
            url: '<wrapped url>',
            optOutUrl: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            surfaceUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Смартфон Apple iPhone X 256GB Space Gray (Серый Космос)',
            price: {
                value: 89290,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: 'МТС',
                id: 42315,
                rating: 2,
                gradeTotal: 9378,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 1,
                daysTo: 1,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_gBF2Sp7f2HUJS97fPqbATQ/orig',
            source: 'Market',
            bigPhoto: {
                width: 347,
                height: 691,
                url: 'https://avatars.mds.yandex.net/get-marketpic/223477/market_gBF2Sp7f2HUJS97fPqbATQ/orig',
            },
            id:
                'yDpJekrrgZEIsTnxRU25zIgUIzFhcwM-3kpiloEsV6RnMEaao4omqiw4iZ1wFacBwAcYu0biHumVBTOVZIMw-0h7C_W34R3nX5IduqQzDuPXbm8z5XFBCdoHHdC4q6tjHIqQpOEE4evXvjyR8A2QJkh8mhLlwIf29kgp-z9-goXMPZKVDuSO-3NkH5TPJaJ9ufxjx3bV0aKiveA5bK8dmcOEayPfsMhNkvq9CHTc18qmmFN9Ddn5k3-G7RJ9E7tDvLnPZp5qp1-VX2i2kAQyowXHO-QLQdv7_X6GwUUG7BFd88wbl3MR1lPhqfl2wjfs',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Смартфон Apple iPhone X 256GB Серебристый',
            price: {
                value: 89290,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: 'МТС',
                id: 42315,
                rating: 2,
                gradeTotal: 9378,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 1,
                daysTo: 1,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig',
            source: 'Market',
            bigPhoto: {
                width: 347,
                height: 691,
                url: 'https://avatars.mds.yandex.net/get-marketpic/372231/market_1eH877qRs-ZI3lFT3prRCQ/orig',
            },
            id:
                'yDpJekrrgZHyR2wK6Be0xyMaaINpuXbPz0qehflYoNegBZzyhUisjIsscbwewp80_3kc0jz80rqlLoBNaWc3z6Fm_3gjiyvEg-SdNM9QJEdJk15XGG4IA8hzW88dKzQqjKOIx7XPsKJRl5NFdpLaI2V3K2b4ecTT_VfRnJeJxKyHxoy4ZN8Oz6CRsMRlTaLqoVcXozjBVqvQo3Arq8gPHYppLPRGPsUKvN47fYywbwqlV_6nWr2KtDs8D66Q-AF4_UiC92RSGXG12cg-gB36sjdlZC-p0-NeSZwK4tQ4948',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Apple iPhone X 256 GB (Silver) Серебристый',
            price: {
                value: 86000,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: 'ANT-SHOP.RU',
                id: 111736,
                rating: 5,
                gradeTotal: 592,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: false,
                pickup: true,
                brief: 'в Москву — 400 руб., возможен самовывоз',
                price: {
                    value: '400',
                    currencyCode: 'RUR',
                    currencyName: 'руб.',
                },
                delivery: true,
                daysFrom: 0,
                daysTo: 0,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: expect.any(String),
            source: 'Market',
            bigPhoto: {
                width: 700,
                height: 700,
                url: 'https://avatars.mds.yandex.net/get-marketpic/202387/market_YPWxQKABwFOyu5l2cOEWDQ/orig',
            },
            id: 'yDpJekrrgZF7iu178jPMcRkcA8IZdTZdp9n6gHDH9VdiuDRUi44VeA',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Смартфон Apple iPhone X 256GB (серебристый)',
            price: {
                value: 89290,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: 'CВЯЗНОЙ',
                id: 3828,
                rating: 5,
                gradeTotal: 61583,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 1,
                daysTo: 1,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig',
            source: 'Market',
            bigPhoto: {
                width: 344,
                height: 689,
                url: 'https://avatars.mds.yandex.net/get-marketpic/193095/market_o_PGznWxeOWGjEBik6k5oA/orig',
            },
            id:
                'yDpJekrrgZGOo_gWdvTGiUEgWBEWp4G4_8oXCPR6Ccgw2DK0Id36pBHLbX76M8xVxIC_I7-mUnSayub2xBKosfjnGO2KCBiXdvKs2z_RFtkOEuT2Fyj-WDgJ6_7abHHy2G0VtPaj5emnjxYDfZTCcbZZ05lK-BvxaEFY_0U8EHubPK7nI2kiGDgTFpAb-_Apz0uvGJDfgfNb-A_lYfOQ1kb0im2zy_hkQZg1vglYpK05gfbjXaaknxCN_lHg_Ed3pr0Th4PZ3iWeFseB8yey_PFee72Ei1SCnezwlJCT2AE',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Смартфон Apple iPhone X 256GB (серый космос)',
            price: {
                value: 89290,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: 'CВЯЗНОЙ',
                id: 3828,
                rating: 5,
                gradeTotal: 61583,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 1,
                daysTo: 1,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_55Cyg-zXeW8p_fkC6RIKKg/orig',
            source: 'Market',
            bigPhoto: {
                width: 344,
                height: 689,
                url: 'https://avatars.mds.yandex.net/get-marketpic/231668/market_55Cyg-zXeW8p_fkC6RIKKg/orig',
            },
            id:
                'yDpJekrrgZEZtWYoDuCCCn3uqBW92EhnXvL35RsG78KQj-OT1gDKP1ttbfccIUt1iwLDoQ0oTv0RuuPA2RIxdJUqvnqMy-M6T4VTTiA4X5CJ9PFAVISmuJT5AJJj1kajVgqW8PuH-dJHkMpH6-ErYjKx75o1rVlyAowpILMWVeOW7ZYAM1Vf-zDE5RhUVlKMwOgz3fg8upksQEFF7jVN_lqWoB3smtu3ZV9EeMZJ907cLMRcQXuu4BtHX7nFZ36fEvsqRrYqXTps-qTkvKl5wbem5U8VpED6uaFrhOFon50',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
        {
            offerSurfaceUrl: '<wrapped url>',
            name: 'Apple iPhone X 256GB Silver (A1902)',
            price: {
                value: 80990,
                currencyCode: 'RUR',
                currencyName: 'руб.',
            },
            category: {
                id: 91491,
                name: 'Мобильные телефоны',
                fullName: 'Мобильные телефоны',
                type: 'GURU',
                childCount: 0,
                advertisingModel: 'CPA',
                viewType: 'GRID',
            },
            target: 'price-list',
            shopInfo: {
                name: '1CLICK',
                id: 262,
                rating: 5,
                gradeTotal: 5004,
                url: '<wrapped url>',
            },
            deliverySis,
            delivery: {
                free: true,
                pickup: true,
                brief: 'в Москву — бесплатно, возможен самовывоз',
                delivery: true,
                daysFrom: 2,
                daysTo: 2,
                cityFrom: 'Москвы',
                cityTo: 'Москву',
            },
            photo: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig',
            source: 'Market',
            bigPhoto: {
                width: 183,
                height: 350,
                url: 'https://avatars.mds.yandex.net/get-marketpic/362766/market_C4QeB98Ex-PKPnOV4sBfAg/orig',
            },
            id:
                'yDpJekrrgZHKK5VJLcLldAwa23oMqFsh5yChP7WCs0v5A2-WiSXd2jBBIm1cNSb72YYhPxrX9rMPpp238EFButKDEemH45VDN8hsmkZNLNNumgNNqlARyTsCTHZt4YM8aSLm8WPyLzynjNuSAmZk9Bldw3aUrX3tBRk62PYINIvy8pp5JnhDRHKZx8xPOS2HxT7_dzLEQfPO6QUVzf_fWnTnn2GHceXO0MDEeqTz0pYtMoo-be8H2oIIG7vluF8EUZ5UpvZHjNhVheQ4nbfxkT_Fh8yha2EgRy_vzMggDU3EnpxHHuEm_gsmv4qnNP6G',
            url: '<wrapped url>',
            buttonUrl: '<wrapped url>',
            constants: {
                buttonText: 'В магазин',
                shopInfoTooltip: 'Перейти в магазин',
                shopRatingDefaultText: 'оценки службы контроля качества Яндекс.Маркета',
                shopRatingText: 'Рейтинг %s из 5 на основе',
                freeDelivery: 'Бесплатно',
            },
            warning: undefined,
            warningType: undefined,
        },
    ],
    bucketInfo: {},
    doNotSearchReason: undefined,
    notification: undefined,
    searchInfo: {
        doNotComparePrice: undefined,
        category: undefined,
        originalQuery: 'Мобильный телефон Apple iPhone X 256GB',
        filteredQuery: 'Мобильный телефон Apple iPhone X 256GB',
        convertedPrice: {
            value: 68960,
            currencyCode: 'RUR',
        },
        offersCount: 190,
        urls: {
            shopsInfo: '<wrapped url>',
            market: '<wrapped url>',
            search:
                'https://market.yandex.ru/search.xml?text=%D0%9C%D0%BE%D0%B1%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D0%B9%20%D1%82%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%20Apple%20iPhone%20X%20256GB&clid=2210590&distr_type=4&utm_source=sovetnik&utm_medium=cpc&utm_campaign=offer-title&cvredirect=2&req_id=jf2hull03fq0k85nlqeapxawapdialqk',
            searchButton: '<wrapped url>',
            prices: '<wrapped url>',
            eula: '<wrapped url>',
            feedback: '<wrapped url>',
            help: '<wrapped url>',
            helpPhone: '<wrapped url>',
            helpTablet: '<wrapped url>',
            disable: '<wrapped url>',
            features: '<wrapped url>',
            userHelp: '<wrapped url>',
        },
        categories: [
            {
                id: 91491,
                name: 'Мобильные телефоны',
                rank: 2.076076003606003,
                url: '<wrapped url>',
            },
            {
                id: 10382050,
                name: 'Док-станции',
                rank: -1.7272781972087836,
                url: '<wrapped url>',
            },
            {
                id: 10498025,
                name: 'Умные часы и браслеты',
                rank: -2.835646395095832,
                url: '<wrapped url>',
            },
        ],
    },
    settings: {
        autoShowShopList: true,
        applicationName: 'Яндекс.Советник',
        isMbrApplication: true,
        items: [
            {
                title: 'Показывать предложения из других регионов',
                enabled: true,
            },
            {
                title: 'Показывать список предложений при наведении мыши',
                enabled: true,
            },
        ],
        region: 'Автоматически',
        needShowNotifications: true,
        showProductNotifications: true,
        showAviaNotifications: true,
        showAutoNotifications: true,
        constants: {
            changeRegion: 'Изменить регион',
            changeSettings: 'Изменить настройки',
            disable: 'Выключить на этом сайте',
            yourRegion: 'Ваш регион:',
        },
    },
    tabs: {
        constants: {
            offers: 'Предложения магазинов',
        },
    },
    shopInfo: undefined,
    searchResult: undefined,
    searchResultCaption: undefined,
    optOutInfo: undefined,
    rules: undefined,
    footer: {
        constants: {
            autoDetectedRegion: 'Автоматически',
            changeCurrentRegion: 'Изменить текущий регион',
            changeRegion: 'Изменить регион',
            data: {
                letter: 'Я',
                prefix: 'Данные',
                suffix: 'ндекс.Маркета',
            },
            feedback: 'Сообщить об ошибке',
            gotoMarket: 'Перейти на Яндекс.Маркет',
            infoAboutShop: 'Информация о продавцах',
            infoAboutShopLegal: 'Юридическая информация о продавцах',
            wrongProductDetect: 'Неверно определен товар',
        },
    },
    info: {
        constants: {
            disableText: 'Как отключить Советника',
            featuresText: 'Что ещё умеет Советник',
            feedbackText: 'Обратная связь',
            helpText: 'Помощь',
            licenseText: 'Лицензионное соглашение',
            prefix: 'для ',
            text: 'Это приложение подсказывает вам более выгодные цены на товары, на которые вы смотрите прямо сейчас.',
            upperLine: 'Яндекс.Советник',
            yandexLLC: '© %s ООО «Яндекс.Маркет»',
        },
    },
    pricebar: {
        constants: {
            byPartnerDisclaimer: 'по данным Яндекс.Маркета',
            closeButtonTooltip: 'Закрыть',
            deliveryText: 'бесплатно',
            goButtonText: 'Посмотреть',
            goButtonTooltip: 'Посмотреть',
            inShop: 'в магазине',
            infoButtonTooltip: 'О программе',
            moreButtonText: 'Еще предложения',
            moreButtonTooltip: 'Предложения других магазинов',
            prevText: 'На этой странице самая низкая цена на',
            settingsButtonTooltip: 'Настройки',
        },
    },
    userRegion: expect.any(String),
});