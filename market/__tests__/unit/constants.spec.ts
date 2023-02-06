/* eslint-disable no-template-curly-in-string */
const constantsBuilder = require('../../src/middlewares/constants/builder-middleware');
const constantsInjector = require('../../src/middlewares/constants/injector-middleware');
const constantsEnricher = require('../../src/middlewares/constants/enricher-middleware');
const langMixer = require('../../utils/lang-mixer');

type TestCase = {
    name: string;
    input: {
        result: { [item: string]: any };
        searchData: { [item: string]: any };
    };
    expected: {
        propName: string;
        builder: { [item: string]: any };
        injector: { [item: string]: any };
        enricher?: { [item: string]: any };
    };
};
describe('Test Builder, Injector constants', () => {
    const testTable = [
        {
            name: 'test pricebar constants',
            input: {
                result: {
                    offers: [{}],
                },
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'pricebar',
                builder: {
                    constants: [
                        'infoButtonTooltip',
                        'settingsButtonTooltip',
                        'closeButtonTooltip',
                        'prevText',
                        'inShopText',
                        'goButtonText',
                        'goButtonTooltip',
                        'priceFromCategory',
                        'onMarket',
                        'moreButtonText',
                        'moreButtonTooltip',
                        'similarModels.similarProducts',
                        'similarModels.comparePrices',
                    ],
                },
                injector: {
                    constants: {
                        infoButtonTooltip: 'О программе',
                        settingsButtonTooltip: 'Настройки',
                        closeButtonTooltip: 'Закрыть',
                        prevText: 'Цена на Яндекс.Маркете на',
                        inShopText: 'в магазине',
                        goButtonText: 'Посмотреть',
                        goButtonTooltip: 'Посмотреть',
                        priceFromCategory: 'Цена на товары из категории',
                        onMarket: 'на Яндекс.Маркете',
                        moreButtonText: 'Еще предложения',
                        moreButtonTooltip: 'Предложения других магазинов',
                        comparePrices: 'Сравнить цены',
                        similarProducts: 'Похожие товары',
                    },
                },
            },
        },
        {
            name: 'test model constants',
            input: {
                result: {
                    model: {
                        name: 'Смартфон Apple iPhone 8 64GB',
                        reviewsCount: 124,
                        rating: 4.5,
                        gradeCount: 858,
                        prices: {
                            max: 53990,
                            min: 31580,
                            avg: 35500,
                            curCode: 'RUR',
                            curName: 'руб.',
                        },
                        offersCount: 139,
                        mainPhoto: {
                            width: 332,
                            height: 620,
                            url: 'https://avatars.mds.yandex.net/get-mpic/932277/img_id8577152390218330050.jpeg/orig',
                            criteria: [
                                {
                                    id: '13887626',
                                    value: '13891866',
                                },
                                {
                                    id: '14871214',
                                    value: '14898056',
                                },
                            ],
                        },
                        photo: 'https://avatars.mds.yandex.net/get-mpic/932277/img_id8577152390218330050.jpeg/orig',
                        isNew: false,
                        outletsCount: 1238,
                        reasonsToBuy: ['качество фотографий', 'мощный процессор'],
                        urls: {
                            model:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dmodel-title%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=model-title&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            modelPicture:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dmodel-picture%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=model-picture&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            offers:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Foffers%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dall-offers%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=all-offers&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            map:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Fgeo%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26modelid%3D1732171388%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Doffers-on-map%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d%26lr%3D2&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=offers-on-map&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            price:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Fprices%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dmiddle-price%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=middle-price&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            reviews:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Freviews%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dmodel-reviews%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=model-reviews&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            reviewsBadge:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Freviews%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dmodel-reviews-badge%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=model-reviews-badge&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            footerAveragePrice:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Fprices%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dfooter-average-price%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=footer-average-price&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                            allOpinions:
                                'https://sovetnik.market.yandex.ru/redir?url=https%3A%2F%2Fmarket.yandex.ru%2Fproduct--smartfon-apple-iphone-8-64gb%2F1732171388%2Freviews%3Fhid%3D91491%26pp%3D1002%26clid%3D2210393%26distr_type%3D4%26utm_source%3Dsovetnik%26utm_medium%3Dcpc%26utm_campaign%3Dopinions-tab-all-button%26lr%3D2%26req_id%3Dunknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&v=201912041225&ref=https%3A%2F%2Fwww.eldorado.ru%2Fcat%2Fdetail%2Fsmartfon-apple-iphone-8-64gb-silver-mq6h2ru-a%2F&clid=2210393&aff_id=1048&client_id=65-1234567890&type=market&transaction_id=unknown-k489pwfe98fnf8w13wtsluec4fb1gq7d&type_sovetnik=browser&target=opinions-tab-all-button&click_type=market&ab=%7B%22ab_spp_after_market%22%3A%22after_market%22%2C%22ab_bconvsa%22%3A%22enabled%22%2C%22ab%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C%5C%22after_market%5C%22%5D%2C%5B%5C%22bconvsa%5C%22%2C%5C%22enabled%5C%22%5D%5D%22%2C%22ab_index%22%3A%22%5B%5B%5C%22spp_after_market%5C%22%2C2%5D%2C%5B%5C%22bconvsa%5C%22%2C2%5D%5D%22%7D&page_price=39990&page_rating=&page_grade_count=&model_id=1732171388',
                        },
                    },
                },
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'model',
                builder: {
                    constants: ['new_model_popup.with_reviews.subText'],
                    info: {
                        constants: [
                            'info.atMarket',
                            'info.scrollTooltipLabel',
                            'info.avgPriceGraph',
                            'info.ratingTooltipText',
                            'info.avgPrice',
                        ],
                    },
                    links: {
                        constants: [
                            'links.allPricesMarket',
                            'links.closestShops',
                            'links.shopsOnMap',
                            'links.reviewsMarket',
                            'links.allPrices',
                            'links.reviews',
                        ],
                    },
                },
                injector: expect.objectContaining({
                    constants: {
                        subText: 'Покупателям нравится',
                    },
                }),
            },
        },
        {
            name: 'test opinions constants',
            input: {
                result: {
                    opinions: [
                        {
                            id: 72975586,
                            date: '18 октября 2017',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 151,
                            disagreeCount: 35,
                            text:
                                'Сейчас на руках черный 8 на 64. До этого пользовался 6s и SE. До них был redmi note 3 pro.\n\nПосле того, как пересел с китайца, очень долго матерился на батарею, пришлось купить внешний аккумулятор. В 8 проблема остаться без связи осталась) По крайней мере, если иду куда-то на ночь, с собой беру и зарядку.\n\nКамера просто потрясная. Примеры ниже)\n\nБыл удивлен качеством стекла, когда уронил его на плитку. Упал на рамку под небольшим углом к стеклу. Остались небольшие следы на рамке и все. Был бы в легком чехле - царапки бы не было.\n\nПриятно, что Siri может подобрать музыку под настроение. И подобрать то, что действительно слушать приятно. (Сравниваю с ВК)\n\nВывод мой таков - если есть время на тонкую настройку девайса и желание искренне наслаждаться кастомной красотой - точно андроид. Если нужна звонилка с качественными сервисами и шикарной камерой - смотрите в сторону iphone.\n\nP.s. После того, как пересел с RMN 3 pro на 6s, было легкое чувство того, что где-то меня нае.. обманули. Разница в цене не соответсвует разнице в качестве, подумал я. Но со временем я обратил внимание на качество сервисов Apple. Очень качественно и продуманно. С моей колокольни - в ios продуман интерфейс и рабочая среда значительно лучше.\n\n"Но андроид кастомизируется в сотни раз лучше" - возразите вы. Здесь я соглашусь. Но, к сожалению или счастью, то время, когда я занимался сменой цветовых схем и тем, как ярлыки расположены на экране - осталось в школе или универе, точно не вспомню.\n\np.s. 2. Решил проблему с наушниками, купив Bluetooth гарнитуру Xiaomi Mi Sport Bluetooth. Цена вопроса - 1800 рублей. Работает с несколькими девайсами параллельно.',
                            pros:
                                'Шикарная камера.\nTouchID работает заметно быстрее, чем на 6s.\nКрепкое стекло. (испытано)\nПриятная OS.',
                            cons:
                                'Пару раз было, что при попытке разблокировать телефон, он вибрирует, но экран не загорается. Нужно жмакать на кнопку блокировки\nОдин раз просто выключился, когда было около 80% зарядки. \nВышеописанное - скорее всего недостатки 11 версии iOS. \n\nРеальные недостатки: \nОтсутствие быстрой зарядки в комплекте. Сильно разочаровало.\nНу и боль - наушники iphone 8 несовместимы с macbook pro 13 2017. Здесь просто комментировать отказываюсь.',
                            author: {
                                visibility: 'ANONYMOUS',
                            },
                            region: {
                                id: 39,
                                name: 'Ростов-на-Дону',
                                type: 'CITY',
                                childCount: 8,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_WEEKS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '151 пользователь счёл',
                            disagreeCountText: '35 пользователей сочли',
                            constants: {
                                label: 'Oтличнaя мoдeль',
                                time: 'неcкoлько недeль',
                                timeLabel: 'Oпыт иcпользoвaния:',
                                pros: 'Дocтoинствa:',
                                cons: 'Нeдоcтaтки:',
                                text: 'Коммeнтарий:',
                                gradeTooltip: 'Oцeнкa пользoвaтeля: 5 из 5',
                                useful: '151 пoльзoвaтeль счёл этoт oтзыв полезным',
                                useless: '35 пользовaтeлeй coчли этот oтзыв беcпoлeзным',
                                anonymous: 'Пoльзoвaтель скрыл cвoи дaнныe',
                            },
                        },
                        {
                            id: 83348006,
                            date: '21 октября 2018',
                            grade: 4,
                            state: 'UNMODERATED',
                            agreeCount: 101,
                            disagreeCount: 26,
                            text:
                                'Аппарат приобретался на замену iPhone SE. Не сказать что по сравнению с ним восьмерка стала революционно новым устройством, но ряд улучшений определенно есть. Среди приятных плюшек данной модели также можно отметить Bluetoch 5.0, поддержку (не из коробки) беспроводной и быстрой зарядки.\n\nЕще немного про фото. Если снимать на приложения поддерживающие сохранение в Raw- формате (DNG), можно получать в процессе обработки очень приличного качества фото. Однако далеко не каждому захочется заморачиваться с мобильным фото, хотя повторюсь, результат может быть гораздо лучше стокового jpeg. \nФронтальная камера конечно ощутимо выросла в мегапикселях по сравнению с SE, однако сильно на качество фото это не повлияло, разве что повысилась детализация. \n\nВ целом достойный аппарат, еще хоть как-то оправдывающий свою цену, про модели с "монобровью" такого уже сказать не могу.\nЭто наверняка последний айфон в классическом виде с кнопкой, адекватным соотношением сторон экрана 16:9 и IPS-матрицей, а также приятным внешним видом с красивой задней панелью (в моем случае расцветка "серый космос"). "Болшие рамки" меня не сильно заботят, недостатком даже считать не стал, так как удобство использования и внутренние характеристики устройства намного приоритетней.',
                            pros:
                                '- Производительность. Она на данный момент избыточна, с большим запасом на несколько лет. Но в этом плане мне и iPhone SE более чем хватало.\n\n- Камера. Больше достоинств у видеосъемки. Параметры и качество видео до сих пор недостижимы ни для одного конкурента- честные 4k 60 fps с оптической и программной стабилизацией (программная стабилизация конечно лучше себя проявляет в Full HD). Фото данный аппарат способен делать на очень хорошем уровне, но только в Raw-формате с последующей обработкой. \nВажным нововведением модели является вспышка Slow Sync, благодаря которой в темное время суток можно получать качественное фото без засветов объекта съемки и равномерном освещении заднего плана.\n\n- Taptic Engine. Очень полезный виброотклик. Новые ощущения от использования устройства. При выбровызове после классической вибрации SE, мало того, что звонок в кармане тяжелее пропустить, так еще и во время вибрации, когда телефон лежит на столе нет противного жужжания, звука вибрации вообще почти нет.\n\n- 3D Touch. Очень удобная и недооцененная самой компанией технология, ибо с появления в 2015 году, под нее до сих пор мало что заточено. А по слухам, вскоре Apple вообще планирует от нее отказаться.\n\n- Сенсорная "кнопка", как результат взаимодействия предыдущих двух пунктов, с Touch ID второго поколения. Сенсорная кнопка оказалась даже удобнее физической, а почти моментальная разблокировка касанием после iPhone SE также дарит приятные впечатления от использования.\n\n- Относительная эргономика и компактность. Не сравнимо с удобством SE, но в целом еще возможно пользоваться устройством одной рукой и носить его в кармане.\n\n- Одна из лучших IPS матриц с уместным количеством DPI и соответственно разумным разрешением без маркетинговых больших чисел в параметрах разрешения экрана, благодаря чему меньшая нагрузка на "железо" и аккумулятор.\nTrue Tone - сначала воспринял функцию подстраивания баланса белого под освещение негативно, но как только отключил ее в комнате, понял насколько она хороша.',
                            cons:
                                '- Главный и единственный серьезный недостаток - качество камерного jpeg. В первую очередь бросается в глаза агрессивный шумодав, превращающий потенциально хорошее фото в жуткую акварель. Также имеется завышенная контурная резкость, частые засветы, провалы в тенях, выпяченная контрастность. Особенно отвратительно благодаря этому "винегрету" обработки получаются лица. \nПричем матрица в смартфоне вполне топового уровня, о чем говорит и качество видео, и особенно качество фотографий, сделанных в Raw-формате. Но обработка стокового приложения "камера" и любого другого, сохраняющего фото в jpeg (программные алгоритмы обработки не зависят от приложения), просто убивает фотографию. Конечно любой смартфон имеет те или иные косяки в обработке - как правило это прежде всего "замыливание" шумов, но такой кошмар как тут, надо еще поискать.\nПоэтому любой айфон из модельного ряда 2017 года категорически не подходит тем, кто хочет приобрести фотофлагман с целью "навел-снял-получил результат" - то есть значительному большинству людей, приобретающих смартфон как мобильную фотокамеру. Если же большего, чем фото для соцсетей, или просмотра фото на экране смартфона не требуется, то данного качества может быть достаточно.\n\n- Объем ОЗУ. Не сказать что 2 Гб на ios устройстве сегодня где-либо не хватает, но неизвестно сколько времени, а точнее до какой версии ios продержится такая ситуация. Я считаю, что производитель мог бы установить и 3 Гб в модель 2017 года с таким-то уровнем производительности.\n\n- Отсутствие разъема Mini Jack.  Apple задала отвратительную тенденцию. На сегодняшний день нет беспроводных наушников с достойным качеством звучания. а аудиотракт айфонов с данным разъемом был вполне неплохим, особенно с использованием стороннего софтового плеера.\n\n- Поддержка записи видео на фронтальную камеру с частотой только 30 кадров/сек. Производитель мог бы добавить возможность записи видео Full HD с частотой 60 кадров/сек, но реализовал это только в моделях 2018 года.',
                            author: {
                                name: 'Иван Г.',
                                grades: 3,
                                visibility: 'NAME',
                            },
                            region: {
                                id: 15,
                                name: 'Тула',
                                type: 'CITY',
                                childCount: 14,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '101 пользователь счёл',
                            disagreeCountText: '26 пользователей сочли',
                            constants: {
                                label: 'Xоpoшaя мoдeль',
                                time: 'несколькo мeсяцeв',
                                timeLabel: 'Oпыт иcпoльзовaния:',
                                pros: 'Дocтоинствa:',
                                cons: 'Нeдocтaтки:',
                                text: 'Кoмментaрий:',
                                gradeTooltip: 'Oценкa пользoвaтeля: 4 из 5',
                                useful: '101 пoльзoватeль cчёл этoт oтзыв пoлeзным',
                                useless: '26 пользовaтелeй coчли этoт oтзыв беcполезным',
                            },
                        },
                        {
                            id: 76935521,
                            date: '11 марта 2018',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 203,
                            disagreeCount: 59,
                            text:
                                'Смартфоны от Apple мне не нравились до 5s из-за своей "кирпичной" формы.  Но когда появилась "шестерка" то я сразу же хотел её приобрести. Но когда узнал про "начинку", существенно не отличающуюся от предыдущих моделей, то передумал. Производитель выпустил 6s, потом 7, а я знакомился с отзывами и все больше понимал, что это не моё. С презентацией "восьмерки" у меня опять появился интерес к смартфонам от Apple. Ознакомившись со многими отзывами, тестами, статьями и т.д., я понял, что этот смартфон является действительно совершенным. Но стоял перед выбором: iPhone 8 или Google Pixel 2. Купил бы второй, если бы он официально продавался в нашей стране. Раньше был некоторый стереотип по поводу "яблочников") Все-таки iPhone сейчас можно увидеть у каждого 3-го. С Pixel 2 хотелось бы выделиться, наверное. Повторюсь, что не пожалел, что купил iPhone 8. Нынче, когда ряд флагманов тесно приблизились друг другу по тех. характеристикам, упор делается именно на дизайн. Я не буду писать, сколько попугаев в бенчмарках набирают флагманы, как оценивают камеры телефонов всякие программы и т.д. Нельзя выделить телефон, который на голову превосходит все остальные, как лет 7 назад. Теперь мы делаем выбор, опираясь на дизайн или личные убеждения. \n*"Серыми" называют товары, незаконно ввезенные на территорию страны. Список официальных ритейлеров в вашем городе можно найти на сайте Apple. Впрочем, "серый" телефон не является подделкой. Отличить сертифицированный iPhone от "серого" очень просто: на коробке на русском языке написано, что входит в комплект, указан поставщик "Эппл Рус", модель обычно A1905(для iPhone 8), номер партии, в к-ом есть две буквы RU, знак EAC; проверяйте IMEI. Если вы решили покупать "серую" модель, покупайте европейскую версию. Потому что с американской могут возникнуть проблемы со связью, а на японской и вовсе нету NFC и нет возможности отключить звук затвора камеры. Кроме того, вы не получите официальной гарантии в своей стране. Надеюсь, мой отзыв в чем-то поможет)',
                            pros:
                                'По сути, это идеальный современный телефон.\n1) Дизайн. Покупал телефон 2 недели назад, но до сих пор не могу налюбоваться внешним видом) У меня белого цвета. Для такого телефона лучше подобрать тонкий силиконовый чехол. Пожалуй, самый красивый смартфон, который я когда-либо видел.\n2) Экран. Потрясающий экран. Мне больше нравится IPS, чем Amoled. К тому же это уже не тот IPS, который был в более ранних моделях. Очень обрадовала технология True Tone: баланс белого подстраивается под дневной свет, ночь или освещение в помещении. Телефоном удобно пользоваться. По-моему, это существенное дополнение для последних iPhone. \n3) Быстродействие и производительность. Можно сказать, что все летает) Если соединение по Wi-Fi хорошее, то страницы загружаются за долю секунды. Собственно, новый процессор для iPhone 8 и X, как известно, самый мощный в мире. Об этом почитайте в интернете. Бенчмарки данный факт, конечно, подтвердили. Ходят слухи, что до 1-го обновления есть какие-то глюки. Я их не заметил.\n4) Удобный интерфейс. Но это субъективно. Если вам нравится менять значки на рабочем столе, то это явно не к iPhone. Но я не вижу в этом необходимости. В iPhone 8 очень простой и удобный интерфейс. Не приходится ломать голову и искать будильник, как во многих смартфонах на андроиде) Лично я перешел на iPhone 8 после шестилетнего пользования древней моделью Samsung Wave 723 на базе ОС Bada (может, кто-то ещё помнит эту ОС), а к новому телефону тем не менее привык очень быстро.\n5) Камера. Так и сразу и не скажешь, чем фото, сделанные на iPhone 8, отличаются от фото, созданных фотоаппаратом. Поищите статью в интернете о том, как один профессиональный фотограф сделал 2000 фото на "восьмерке" и 8 Plus, а также сравнение с камерой iPhone 7)\n6) Качество звука. Тоже на высоте.\nВ целом, замечательный смартфон) Не знаю, что нас ждет в будущем, но iPhone 8 превзошел все мои ожидания. Может, из-за того, что я долго ходил с динозавром, но все же сложно игнорировать достоинства этого смартфона)\n',
                            cons:
                                'Не выявил существенных недостатков, но ознакомился с теми, которые обычно выделяют другие пользователи. Всё-таки в основном эти недостатки чисто субъективны либо связаны с тем, как я заметил, что людям в руки попалась "серая" модель (об этом в комментарии). Перечислим некоторые:\n1) Батарея. Если прочитать характеристики, то можно удивиться, как на таком флагмане всего 1,8К mAh. Однако производитель оптимизировал работу процессора. У меня батарея держится 1,5-2 суток. Но я не играю в игры, но не сказал бы, что редко беру телефон в руки: браузер, соц. сети, мессенджеры, камера - для всего это вроде норм. Но хотелось бы больше, конечно. Однако эта проблема актуальна для многих топовых смартфонов. Мой совет: отключите службы геолокации, так автономность заметно увеличится. \n2) Телефон "скользкий". Ну, чего можно ещё ожидать от телефона, корпус которого сделан из стекла.. Очень сомневаюсь, что у вас появится желание носить этот телефон без чехла. \n3) В комплекте нету беспроводной зарядки. Но представьте, каким был бы объем коробки с телефоном, если бы туда положили и беспроводную зарядку. Думаю, эту проблему можно легко решить, купив качественную и доступную беспроводную зарядку. \n4) Цена. По-моему, глупо сейчас критиковать Apple за высокую цену. Посмотрите на других производителей: даже цены на топовые "китайцы" такие же, как и на последние iPhone, что уж тут говорить о смартфонах от Samsung, LG, HTC и др. Просто не покупайте смартфон сразу на старте продаж. Через месяц-два он все равно существенно подешевеет, даже если компания не презентовала новые гаджеты. Смысл торопиться) Хотя по поводу цены на iPhone X соглашусь, но "восьмерка", по сути, такая же, только отличается экраном, способом защиты данных (если вас не беспокоит, следит ли за вами Пентагон или нет, то какая разница, пользуетесь вы Touch или Face ID. Кому как удобно, что касается и экрана). \nДругие "недостатки" или совсем субъективны, или связаны с покупкой "серой" модели. ',
                            author: {
                                visibility: 'ANONYMOUS',
                            },
                            region: {
                                id: 35,
                                name: 'Краснодар',
                                type: 'CITY',
                                childCount: 4,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_WEEKS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '203 пользователя сочли',
                            disagreeCountText: '59 пользователей сочли',
                            constants: {
                                label: 'Oтличнaя мoдeль',
                                time: 'несколькo нeдeль',
                                timeLabel: 'Oпыт иcпользoвaния:',
                                pros: 'Доcтоинcтвa:',
                                cons: 'Нeдocтaтки:',
                                text: 'Комментapий:',
                                gradeTooltip: 'Oцeнкa пoльзоватeля: 5 из 5',
                                useful: '203 пользoватeля coчли этoт отзыв полезным',
                                useless: '59 пoльзoватeлeй coчли этoт oтзыв бecпoлeзным',
                                anonymous: 'Пользoвaтeль cкрыл cвoи данныe',
                            },
                        },
                        {
                            id: 74892220,
                            date: '30 декабря 2017',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 135,
                            disagreeCount: 43,
                            text:
                                'Подержав в руках одновременно X и 8 появились сомнения, стоит ли переплачивать 30т.р. Позже прочитав, что у iPhone X подсветка экрана с ШИМ (экран мерцает на высокой частоте), понял, что однозначно стоит взять 8 и ни разу не пожалел:)\nКупил девайс для Британского рынка EC(евротест), у него максимальная громкость звука в наушниках на 15-20% меньше чем у девайсов для России, благо на громкость Bluetooth такая же.\nДля тех у кого быстро разряжается iPhone:\nИдем в Настройки->Конфиденциальность-> Службы геолокации -> Системные службы отключаем все лишнее (я оставил только калибровку движения, найти iPhone, и поделиться геолокацией). Это дает ощутимый прирост автономного времени работы.',
                            pros:
                                'Качество сборки в лучших традициях Apple, все идеально.\nСкорость работы прямо огонь! Все летает с iPhone 7 разница конечно не колоссальная, а вот с 6ками уже вполне ощутима.\nАвтономность хватает на 12 часов использования, то есть 1.5-2 дня.',
                            cons:
                                'Пожалуй единственный реальный недостаток - сильно царапается задняя крышка, через пару дней аккуратного использования уже появились коцки, пришлось докупить чехол-накладку.\nНебольшие засветы по краям экрана на белом фоне.\nTrueTone надо было назвать YellowTone) благо отключаем.\nНет "ВАУ" эффекта, как от Х.\n\n',
                            author: {
                                name: 'Иван К.',
                                grades: 11,
                                visibility: 'NAME',
                            },
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
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '135 пользователей сочли',
                            disagreeCountText: '43 пользователя сочли',
                            constants: {
                                label: 'Oтличнaя модeль',
                                time: 'нecкoлькo мecяцeв',
                                timeLabel: 'Oпыт использовaния:',
                                pros: 'Дocтoинства:',
                                cons: 'Нeдocтатки:',
                                text: 'Кoммeнтaрий:',
                                gradeTooltip: 'Оценкa пользoвaтeля: 5 из 5',
                                useful: '135 пoльзовaтeлeй coчли этoт oтзыв пoлeзным',
                                useless: '43 пользовaтеля cочли этот oтзыв беcпoлезным',
                            },
                        },
                        {
                            id: 77849645,
                            date: '8 апреля 2018',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 57,
                            disagreeCount: 19,
                            text:
                                'Телефон приобрёл перед Новым годом, Первые вчечатлиния были двоякими (перешёл с 6го айфона) так как все тот же дизайн! Да он быстрее но нет того эффекта как раньше от покупки нового флагмана! За эти деньги хочется большего, но увы! Зато это все тот же надежный друг и товарищ, который всегда с тобой! В принципе покупкой доволен, не как слон конечно))))) ',
                            pros:
                                'Телефон быстрый, производительность с запасом на 3-5 лет! Беспроводная зарядка! Хороший экран!',
                            cons: 'Старый дизайн! Толстые рамки! Нет разъёма 3.5!',
                            author: {
                                name: 'Владимир Соловьев',
                                avatarUrl:
                                    'https://avatars.mds.yandex.net/get-yapic/26311/enc-0a42771e002d07ff58799a16577e384d8e105f4875a31ed3eaf9254808e7b76f/islands-middle',
                                grades: 1,
                                visibility: 'NAME',
                            },
                            region: {
                                id: 54,
                                name: 'Екатеринбург',
                                type: 'CITY',
                                childCount: 7,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '57 пользователей сочли',
                            disagreeCountText: '19 пользователей сочли',
                            constants: {
                                label: 'Отличнaя модeль',
                                time: 'нeскoлькo мeсяцeв',
                                timeLabel: 'Oпыт испoльзoвания:',
                                pros: 'Дocтоинcтвa:',
                                cons: 'Недocтaтки:',
                                text: 'Кoммeнтаpий:',
                                gradeTooltip: 'Oцeнкa пользовaтеля: 5 из 5',
                                useful: '57 пoльзoватeлей сoчли этoт отзыв полeзным',
                                useless: '19 пoльзoватeлeй coчли этoт отзыв бecполeзным',
                            },
                        },
                        {
                            id: 80309820,
                            date: '7 июля 2018',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 17,
                            disagreeCount: 6,
                            text:
                                'Сразу после анонса «восьмерки» заняли первые строчки рейтингов бенчмарков с большим отрывом. Без тормозов работает любая программа из App Store, интерфейс системы идеально плавный. Телефон с большим резервом по производительности.',
                            pros:
                                'Качество звука и камеры. Хорошая оптическая стабилизация камеры. Вычислительная система, которая обеспечивает нормальное время работы батареи, Программное обеспечение на базе ARKit. True Tone дисплей, как у iPad Pro.\n',
                            cons: '- устаревший дизайн\n- защита от воды по устаревшему стандарту',
                            author: {
                                name: 'Wolf',
                                avatarUrl:
                                    'https://avatars.mds.yandex.net/get-yapic/21493/enc-b6590cbd1757a14e498eb50dc8aa18e9/islands-middle',
                                grades: 33,
                                visibility: 'NAME',
                            },
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
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '17 пользователей сочли',
                            disagreeCountText: '6 пользователей сочли',
                            constants: {
                                label: 'Oтличная модeль',
                                time: 'неcколько меcяцeв',
                                timeLabel: 'Oпыт иcпoльзования:',
                                pros: 'Достоинcтва:',
                                cons: 'Недоcтaтки:',
                                text: 'Комментaрий:',
                                gradeTooltip: 'Oценкa пoльзoвaтеля: 5 из 5',
                                useful: '17 пoльзoвaтелей сoчли этoт oтзыв пoлeзным',
                                useless: '6 пользовaтeлeй сoчли этот oтзыв бeсполезным',
                            },
                        },
                        {
                            id: 74329016,
                            date: '11 декабря 2017',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 45,
                            disagreeCount: 18,
                            text:
                                'Когда ещё только-только выходили на рынок новые модели айфонов, ждал, как и многие, выхода Х, недоумевая, зачем Apple выпустила в этом году очередную эволюцию поколения 6? Но с выходом флагмана начал задаваться другим вопросом-нахрена Apple было торопиться с выпуском сырого Х? В восьмёрочке эволюцией всё отточено практически до совершенства: Touch ID второго поколения быстр, как никогда-не успеешь поднести палец к сканеру, как смартфон уже разблокирован. Функция raise to wake, когда при подъёме смартфона экран автоматически загорается очень приятная плюшечка после 6-ки то. Размер экрана-самое оно. Не понимаю, почему Apple решила, что будущее принадлежит лопатофонам?? Это стало самым весомым аргументов в пользу 8-ки. Беспроводная зарядка-в яблочко! Почему в Apple так долго оттягивали внедрение этой инновации?? Дизайн отточили до совершенства: теперь чёрный смартфон чёрный везде, по всем краям. Стеклянный корпус очень приятно держать в руках. Олеофобное покрытие практически не оставляет следов на телефоне. Аккумулятора уверенно хватает на полный день, но, конечно, могли бы сделать его поболее. Качество камеры специально не испытывал-не фотограф, но уверен, что лучше, чем у 6-ки. Скорость работы потрясает-всё просто летает. Список сетей при ручном поиске выдаёт практически моментально. Блютуз пятого поколения цепляется за беспроводные гаджеты гораздо цепче-заметил по свои airpods и apple watch, дальность уверенного контакта с телефоном стала выше. Динамики также стали громче и звук-объёмней что ли. За ос11 команда яблоразработчиков во главе с яблогенералом Куком будут, конечно, гореть в аду, но на 8-ке она работает практически безотказно (версия 11.2). В общем, любителям яблоклассики-советую!',
                            pros: 'Быстрота отклика, стеклянный корпус, Touch ID второго поколения',
                            cons: '11-я ось!',
                            author: {
                                name: 'Михаил Б.',
                                grades: 12,
                                visibility: 'NAME',
                            },
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
                            recommend: true,
                            usageTime: 'FEW_WEEKS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '45 пользователей сочли',
                            disagreeCountText: '18 пользователей сочли',
                            constants: {
                                label: 'Отличная мoдель',
                                time: 'нecкoлькo нeдель',
                                timeLabel: 'Oпыт иcпoльзoвaния:',
                                pros: 'Дocтoинства:',
                                cons: 'Нeдocтатки:',
                                text: 'Комментapий:',
                                gradeTooltip: 'Оцeнкa пoльзоватeля: 5 из 5',
                                useful: '45 пoльзoвaтeлeй coчли этот oтзыв полeзным',
                                useless: '18 пользoватeлей сoчли этот отзыв бecпoлeзным',
                            },
                        },
                        {
                            id: 73919286,
                            date: '25 ноября 2017',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 87,
                            disagreeCount: 36,
                            text:
                                'В пользовании 1,5 месяца. До этого были 4, 5s. \nБезусловно, разница с пятеркой конечно очень значительная. Во всем. \nТут даже описывать нечего. \nБрал за 45 тысяч европейца, модель 1905. О чем нисколько не жалею. Везде тогда стоил 5657 тысяч. Это конечно совершенна не та цена за которую надо покупать. \nСразу же заказал беспроводную зарядку 1300 рублей + усиленый блок питания 700 рублей. Пришло все быстро. Беспроводная зарядка это вещь. После месяца использования привыкаешь настолько, что даже задумываешься над тем как ты жил раньше без этого девайса. \nКамера вполне приличная. С зеркалкой конечно не сравнить. Физику не обманешь (в моей зеркалке одна оптика только чего стоит), но снимает на удивление очень и очень не плохо. Для селфи, соцсетей и инстаграмма идеально. А большего большинству и не надо.\nЗаряд батарея держит вполне достойно. На сутки хватает полностью. Даже если не выпускать телефон из рук. \nОчень шустрый. Никаких тормозов. Один раз глюкнул, наотрез отказался выходить в интернет. Притом что сеть видел и обычный разговор поддерживал. Помогла простая перезагрузка. \nПри разговоре никакого треска динамика нет. Слышимость прекрасная. Звонит громко. Звук качественный. \nОбъема памяти 64 гигабайта лично мне хватает. Для просмотра фильмов и сериалов есть айпад. Для телефона этого достаточно. \nКак то меня спросили, почему именно айфон? Для меня, в первую очередь, это безопасность. Закрытость iOS. Телефон это и записная книжка, и хранилище личных данных, и (самое главное) мобильный бумажник. Он постоянно при мне. Я им пользуюсь не по разу ежедневно. Семь дней в неделю. Безопасность платежей, сохранность данных, Touch ID, стабильность системы. Вот что привлекает. А вовсе не факт, самого обладания дорогой вещью. Да. Не дешёвый. Зато надёжный. Это мой помощник. А он не может быть плохим. \n',
                            pros:
                                'Привлекательный внешне\nПриятный на ощупь\nНадежность и безопасность закрытой системы iOS \nПостоянная поддержка и совершенствование iOS \nОтсутствие вирусов, распространённых на Android\nApple Pay\nПрактически никаких глюков и тормозов\nДостойная камера\nПриемлемый объём памяти 64 Гб\nДолго держит заряд батареи \nБеспроводная зарядка',
                            cons: 'Нет стандартного выхода на наушники',
                            author: {
                                visibility: 'ANONYMOUS',
                            },
                            region: {
                                id: 973,
                                name: 'Сургут',
                                type: 'CITY',
                                childCount: 7,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '87 пользователей сочли',
                            disagreeCountText: '36 пользователей сочли',
                            constants: {
                                label: 'Oтличнaя мoдель',
                                time: 'несколько меcяцeв',
                                timeLabel: 'Опыт иcпoльзoвaния:',
                                pros: 'Дocтoинствa:',
                                cons: 'Нeдocтaтки:',
                                text: 'Комментаpий:',
                                gradeTooltip: 'Oцeнкa пoльзoвaтeля: 5 из 5',
                                useful: '87 пoльзoвaтeлей сoчли этот отзыв полезным',
                                useless: '36 пользoватeлей cочли этoт oтзыв бecпoлезным',
                                anonymous: 'Пoльзовaтeль скpыл cвoи данныe',
                            },
                        },
                        {
                            id: 84845254,
                            date: '3 декабря 2018',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 10,
                            disagreeCount: 4,
                            text:
                                'Долго думал о переходе на iOs (до этого всегда были смартфоны и телефоны только от Sony) и как только это свершилось, ни разу не пожалел. К новой системе привык за пару дней, тут всё просто и понятно. \nПосле полугода пользования ни разу не было никаких проблем, зависаний, тормозов или чего либо ещё - всё работает быстро и стабильно.',
                            pros:
                                '1. Качество сборки, iOs, производительность - все приложения работают стабильно и быстро. Аппарат приятно держать в руках, им удобно пользоваться.\n2. Цветопередача, яркий экран. \n3. Звук.\n4. Камера. Перед покупкой этой модели долго просматривал различные обзоры, тесты на качество фото и видео, сравнивал разные смартфоны в магазинах. В итоге, работа камеры и ПО больше всего понравилась именно на афоне - особенно это видно на видео, которые всегда получаются гладкими с отличным звуком и цветопередачей.',
                            cons:
                                '1. Возможно, не для всех это недостаток, но 64Гб физической памяти маловато. Если вы собираетесь много фотографировать и снимать видео (и хотите их хранить на аппарате), берите модель с большим объемом памяти.\n2. Заряда хватает максимум на сутки. Но заряжается он довольно быстро.',
                            author: {
                                name: 'Виктор К.',
                                avatarUrl:
                                    'https://avatars.mds.yandex.net/get-yapic/43473/Gwnl3n4563W0sTK5lM83sYCbY-1568502259/islands-middle',
                                grades: 3,
                                visibility: 'NAME',
                            },
                            region: {
                                id: 47,
                                name: 'Нижний Новгород',
                                type: 'CITY',
                                childCount: 8,
                                country: {
                                    id: 225,
                                    name: 'Россия',
                                    type: 'COUNTRY',
                                    childCount: 10,
                                },
                            },
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '10 пользователей сочли',
                            disagreeCountText: '4 пользователя сочли',
                            constants: {
                                label: 'Oтличная мoдeль',
                                time: 'нeскoлькo мeсяцeв',
                                timeLabel: 'Oпыт иcпoльзoвaния:',
                                pros: 'Дocтoинствa:',
                                cons: 'Недocтaтки:',
                                text: 'Коммeнтapий:',
                                gradeTooltip: 'Oценкa пользoвателя: 5 из 5',
                                useful: '10 пoльзoвaтeлeй cочли этoт oтзыв полeзным',
                                useless: '4 пoльзoвaтеля cочли этoт oтзыв бecпoлезным',
                            },
                        },
                        {
                            id: 92480757,
                            date: '16 июля 2019',
                            grade: 5,
                            state: 'UNMODERATED',
                            agreeCount: 4,
                            disagreeCount: 1,
                            text:
                                'Телефон супер. Работает мощно. Игры все тянет, на все реагирует быстро, переключается между приложениями так же быстро, не лагает. Но появился один минус! После очередного обновления ПО стал ужасно греться от просмотра видео, когда играешь. Надеюсь после следующего ПО это пройдет.',
                            pros: 'Мощный, быстрый, хороший звук и картинка',
                            cons: 'после обновления ПО стал нагреваться',
                            author: {
                                name: 'Богдан В.',
                                avatarUrl:
                                    'https://avatars.mds.yandex.net/get-yapic/32838/LxR3bSWQhdZ65BR37O5ARet0uRw-1/islands-middle',
                                grades: 2,
                                visibility: 'NAME',
                            },
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
                            recommend: true,
                            usageTime: 'FEW_MONTHS',
                            verifiedBuyer: false,
                            model: {
                                id: 1732171388,
                            },
                            facts: [
                                {
                                    id: 0,
                                    title: 'Экран',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Камера',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Время автономной работы',
                                    value: 4,
                                },
                                {
                                    id: 0,
                                    title: 'Объем памяти',
                                    value: 5,
                                },
                                {
                                    id: 0,
                                    title: 'Производительность',
                                    value: 5,
                                },
                            ],
                            agreeCountText: '4 пользователя сочли',
                            disagreeCountText: '1 пользователь счёл',
                            constants: {
                                label: 'Отличнaя мoдель',
                                time: 'нecкoлько мecяцев',
                                timeLabel: 'Oпыт иcпoльзования:',
                                pros: 'Дoстoинcтвa:',
                                cons: 'Недoстaтки:',
                                text: 'Кoммeнтapий:',
                                gradeTooltip: 'Oцeнка пoльзoватeля: 5 из 5',
                                useful: '4 пользoватeля coчли этoт отзыв пoлезным',
                                useless: '1 пoльзoвaтeль счёл этoт oтзыв бecпoлeзным',
                            },
                        },
                    ],
                },
                searchData: {
                    isOpinionsFound: true,
                    constants: {},
                },
            },
            expected: {
                propName: 'opinions',
                builder: [
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_WEEKS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                            '[v2].author.anonymous',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.4.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_WEEKS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                            '[v2].author.anonymous',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_WEEKS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                            '[v2].author.anonymous',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                    {
                        constants: [
                            '[v2].grade.5.label',
                            '[v2].usageTime.FEW_MONTHS.time',
                            '[v2].usageTime.timeLabel',
                            '[v2].pros',
                            '[v2].cons',
                            '[v2].text',
                            '[v2].gradeTooltip',
                            '[v2].opinionRate.useful',
                            '[v2].opinionRate.useless',
                        ],
                    },
                ],
                injector: expect.arrayContaining([
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5', // eslint-disable-line
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Хорошая модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                    expect.objectContaining({
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: ${grade} из 5',
                            useful: '${agreeCountText} этот отзыв полезным',
                            useless: '${disagreeCountText} этот отзыв бесполезным',
                        },
                    }),
                ]),
                enricher: [
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '151 пользователь счёл этот отзыв полезным',
                            useless: '35 пользователей сочли этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    },
                    {
                        constants: {
                            label: 'Хорошая модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 4 из 5',
                            useful: '101 пользователь счёл этот отзыв полезным',
                            useless: '26 пользователей сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '203 пользователя сочли этот отзыв полезным',
                            useless: '59 пользователей сочли этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '135 пользователей сочли этот отзыв полезным',
                            useless: '43 пользователя сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '57 пользователей сочли этот отзыв полезным',
                            useless: '19 пользователей сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '17 пользователей сочли этот отзыв полезным',
                            useless: '6 пользователей сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько недель',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '45 пользователей сочли этот отзыв полезным',
                            useless: '18 пользователей сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '87 пользователей сочли этот отзыв полезным',
                            useless: '36 пользователей сочли этот отзыв бесполезным',
                            anonymous: 'Пользователь скрыл свои данные',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '10 пользователей сочли этот отзыв полезным',
                            useless: '4 пользователя сочли этот отзыв бесполезным',
                        },
                    },
                    {
                        constants: {
                            label: 'Отличная модель',
                            time: 'несколько месяцев',
                            timeLabel: 'Опыт использования:',
                            pros: 'Достоинства:',
                            cons: 'Недостатки:',
                            text: 'Комментарий:',
                            gradeTooltip: 'Оценка пользователя: 5 из 5',
                            useful: '4 пользователя сочли этот отзыв полезным',
                            useless: '1 пользователь счёл этот отзыв бесполезным',
                        },
                    },
                ],
            },
        },
        {
            name: 'test tab constants',
            input: {
                result: {
                    offers: [{}],
                    model: {},
                },
                searchData: {
                    isOpinionsFound: true,
                    isOpinionsEnabled: true,
                    constants: {},
                },
            },
            expected: {
                propName: 'tabs',
                builder: { constants: ['offers', 'opinions', '[ab].opinions.similarModels'] },
                injector: {
                    constants: {
                        offers: 'Предложения магазинов',
                        opinions: 'Отзывы покупателей',
                        similarModels: 'Похожие товары',
                    },
                },
            },
        },
        {
            name: 'test settings constants',
            input: {
                result: {},
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'settings',
                builder: {
                    constants: ['yourRegion', 'changeRegion', 'changeSettings', 'disable'],
                },
                injector: {
                    constants: {
                        yourRegion: 'Ваш регион:',
                        changeRegion: 'Изменить регион',
                        changeSettings: 'Изменить настройки',
                        disable: 'Выключить на этом сайте',
                    },
                },
            },
        },
        {
            name: 'test info constants',
            input: {
                result: {},
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'info',
                builder: {
                    constants: [
                        'helpText',
                        'yandexLLC',
                        'feedbackText',
                        'licenseText',
                        'disableText',
                        'featuresText',
                        'upperLine',
                        'prefix',
                        'text',
                    ],
                },
                injector: {
                    constants: {
                        text:
                            'Это приложение подсказывает вам более выгодные цены на товары, на которые вы смотрите прямо сейчас.',
                        helpText: 'Помощь',
                        feedbackText: 'Обратная связь',
                        yandexLLC: '© %s ООО «Яндекс.Маркет»',
                        licenseText: 'Лицензионное соглашение',
                        disableText: 'Как отключить Советника',
                        featuresText: 'Что ещё умеет Советник',
                        upperLine: 'Яндекс.Советник',
                        prefix: 'для ',
                    },
                },
            },
        },
        {
            name: 'test footer constants',
            input: {
                result: {
                    offers: [{}],
                    model: {},
                },
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'footer',
                builder: {
                    constants: [
                        'changeRegion',
                        'changeCurrentRegion',
                        'autoDetectedRegion',
                        'infoAboutShop',
                        'infoAboutShopLegal',
                        'feedback',
                        'wrongProductDetect',
                        'gotoMarket',
                        'data',
                    ],
                },
                injector: {
                    constants: {
                        changeRegion: 'Изменить регион',
                        changeCurrentRegion: 'Изменить текущий регион',
                        autoDetectedRegion: 'Автоматически',
                        infoAboutShop: 'Информация о продавцах',
                        infoAboutShopLegal: 'Юридическая информация о продавцах',
                        feedback: 'Сообщить об ошибке',
                        wrongProductDetect: 'Неверно определен товар',
                        gotoMarket: 'Перейти на Яндекс.Маркет',
                        data: {
                            prefix: 'Данные',
                            letter: 'Я',
                            suffix: 'ндекс.Маркета',
                        },
                    },
                },
            },
        },
        {
            name: 'test feedback constants',
            input: {
                result: {},
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'feedback',
                builder: {
                    constants: [
                        'thanks',
                        'text',
                        'title',
                        'error',
                        'placeholder',
                        'closeButtonText',
                        'submitButtonText',
                        'radioReports',
                    ],
                },
                injector: {
                    constants: {
                        thanks: 'Спасибо!',
                        text: 'Ваше сообщение поможет улучшить Советника',
                        title: 'Спасибо, мы получили ваш отчёт об ошибке',
                        error: 'Комментарий не может быть пустым.',
                        placeholder:
                            'Пожалуйста, расскажите, что именно случилось. Ваш комментарий поможет нам исправить проблему быстрее.',
                        closeButtonText: 'Закрыть',
                        submitButtonText: 'Отправить',
                        radioReports: {
                            model: {
                                text: 'Не тот товар',
                                value: 'model',
                            },
                            features: {
                                text: 'У товара не те характеристики (цвет, объем памяти и т.п.)',
                                value: 'features',
                            },
                            notCheaper: {
                                text: 'Предложение Советника дороже',
                                value: 'not_cheaper',
                            },
                            multipleProducts: {
                                text: 'На этой странице несколько товаров',
                                value: 'multiple_products',
                            },
                            notProductPage: {
                                text: 'На этой странице нет товаров',
                                value: 'not_product_page',
                            },
                        },
                    },
                },
            },
        },
        {
            name: 'test opinionsInfo constants',
            input: {
                result: {
                    model: {
                        reviewsCount: 124,
                    },
                },
                searchData: {
                    isOpinionsFound: true,
                    constants: {},
                },
            },
            expected: {
                propName: 'opinionsInfo',
                builder: { constants: ['[v2].allOpinions'] },
                injector: {
                    constants: {
                        allOpinions: 'Посмотреть все отзывы на Яндекс.Маркете (${model.reviewsCount})',
                    },
                },
                enricher: {
                    constants: {
                        allOpinions: 'Посмотреть все отзывы на Яндекс.Маркете (124)',
                    },
                },
            },
        },
        {
            name: 'test searchPopup constants',
            input: {
                result: {},
                searchData: {
                    constants: {},
                },
            },
            expected: {
                propName: 'searchPopup',
                builder: { constants: ['inYourRegion', 'showMore', 'showAllOffers', 'ya', 'market'] },
                injector: {
                    constants: {
                        inYourRegion: 'в вашем регионе:',
                        showMore: 'Показать еще',
                        showAllOffers: 'Посмотреть все предложения на',
                        ya: 'Я',
                        market: 'ндекс.Маркете',
                    },
                },
            },
        },
    ] as Array<TestCase>;

    testTable.forEach(({ name, input, expected }: TestCase) => {
        test(name, (done) => {
            const request: {
                result: { [item: string]: any };
                searchData: { [item: string]: any };
            } = input;

            constantsBuilder(request, null, () => {
                expect(request.searchData.constants[expected.propName]).toEqual(expected.builder);

                constantsInjector(request, null, () => {
                    expect(request.result[expected.propName]).toEqual(expected.injector);

                    if (!expected.enricher) {
                        done();
                        return;
                    }

                    constantsEnricher(request, null, () => {
                        const mapperFunc = (obj: { constants: { [item: string]: any } }) => {
                            const { constants } = obj || {};
                            const result: any = {};

                            Object.keys(constants).forEach((key) => {
                                result[key] = langMixer.removeZeroSymbols(
                                    langMixer.mixString(constants[key], /[a-z\u2004]/gi, 1),
                                );
                            });

                            return { constants: result };
                        };

                        const resultElement = request.result[expected.propName];

                        let result = {};

                        if (Array.isArray(resultElement)) {
                            result = resultElement.map(mapperFunc);
                        } else {
                            result = mapperFunc(resultElement);
                        }

                        expect(result).toEqual(expected.enricher);
                        done();
                    });
                });
            });
        });
    });
});
