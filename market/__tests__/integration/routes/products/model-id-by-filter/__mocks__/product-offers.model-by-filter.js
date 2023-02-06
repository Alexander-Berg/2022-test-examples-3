/* eslint-disable max-len */
import { REPORT_DEV_HOST, REPORT_DEV_PORT, REPORT_DEV_PATH } from '../../../../../../src/env';

const HOST = `${REPORT_DEV_HOST}:${REPORT_DEV_PORT}`;

const ROUTE = new RegExp(`/${REPORT_DEV_PATH}`);

const RESPONSE = {
    search: {
        total: 1,
        totalOffers: 0,
        totalFreeOffers: 0,
        totalOffersBeforeFilters: 0,
        totalModels: 0,
        totalPassedAllGlFilters: 0,
        adult: false,
        salesDetected: false,
        maxDiscountPercent: 0,
        isDeliveryIncluded: false,
        isPickupIncluded: false,
        results: [
            {
                showUid: '',
                entity: 'product',
                vendor: {
                    entity: 'vendor',
                    id: 686779,
                    name: 'Seagate',
                    slug: 'seagate',
                    website: 'https://www.seagate.com/ru/ru',
                    logo: {
                        entity: 'picture',
                        url: '//avatars.mds.yandex.net/get-mpic/1705137/img_id474286622556761381.png/orig',
                        thumbnails: [],
                        signatures: [],
                    },
                    filter: '7893318:686779',
                },
                titles: {
                    raw: 'Жесткий диск Seagate Barracuda 2 TB ST2000DM008',
                    highlighted: [
                        {
                            value: 'Жесткий диск Seagate Barracuda 2 TB ST2000DM008',
                        },
                    ],
                },
                titlesWithoutVendor: {
                    raw: 'Жесткий диск Barracuda 2 TB ST2000DM008',
                    highlighted: [
                        {
                            value: 'Жесткий диск Barracuda 2 TB ST2000DM008',
                        },
                    ],
                },
                slug: 'zhestkii-disk-seagate-barracuda-2-tb-st2000dm008',
                description:
                    'для настольного компьютера, 3.5", SATA 6Gb/s, 2000 ГБ, буфер 256 МБ, скорость вращения 7200 rpm',
                eligibleForBookingInUserRegion: false,
                categories: [
                    {
                        entity: 'category',
                        id: 91033,
                        nid: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        type: 'guru',
                        cpaType: 'cpc_and_cpa',
                        isLeaf: true,
                        kinds: [],
                    },
                ],
                urls: {},
                navnodes: [
                    {
                        entity: 'navnode',
                        id: 55316,
                        name: 'Внутренние жесткие диски',
                        slug: 'vnutrennie-zhestkie-diski',
                        fullName: 'Внутренние жесткие диски',
                        isLeaf: true,
                        rootNavnode: {},
                    },
                ],
                pictures: [
                    {
                        entity: 'picture',
                        original: {
                            containerWidth: 493,
                            containerHeight: 701,
                            url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/orig',
                            width: 493,
                            height: 701,
                        },
                        thumbnails: [
                            {
                                containerWidth: 50,
                                containerHeight: 50,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/1hq',
                                width: 50,
                                height: 50,
                            },
                            {
                                containerWidth: 100,
                                containerHeight: 100,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/2hq',
                                width: 100,
                                height: 100,
                            },
                            {
                                containerWidth: 75,
                                containerHeight: 75,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/3hq',
                                width: 75,
                                height: 75,
                            },
                            {
                                containerWidth: 150,
                                containerHeight: 150,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/4hq',
                                width: 150,
                                height: 150,
                            },
                            {
                                containerWidth: 200,
                                containerHeight: 200,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/5hq',
                                width: 200,
                                height: 200,
                            },
                            {
                                containerWidth: 250,
                                containerHeight: 250,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/6hq',
                                width: 250,
                                height: 250,
                            },
                            {
                                containerWidth: 120,
                                containerHeight: 120,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/7hq',
                                width: 120,
                                height: 120,
                            },
                            {
                                containerWidth: 240,
                                containerHeight: 240,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/8hq',
                                width: 240,
                                height: 240,
                            },
                            {
                                containerWidth: 500,
                                containerHeight: 500,
                                url: '//avatars.mds.yandex.net/get-mpic/1361544/img_id8895360478653894514.jpeg/9hq',
                                width: 500,
                                height: 500,
                            },
                        ],
                        signatures: [],
                    },
                ],
                filters: [
                    {
                        id: '7893318',
                        type: 'enum',
                        name: 'Производитель',
                        xslname: 'vendor',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 1,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 2,
                                found: 1,
                                value: 'Seagate',
                                vendor: {
                                    name: 'Seagate',
                                    entity: 'vendor',
                                    id: 686779,
                                },
                                id: '686779',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['686779'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '17688484',
                        type: 'enum',
                        name: 'Емкость',
                        xslname: 'volume',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 2,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 7,
                                found: 1,
                                value: '2 ТБ',
                                id: '17688609',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['17688609'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127045',
                        type: 'enum',
                        name: 'Форм-фактор',
                        xslname: 'FormFactor',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 3,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 4,
                                found: 1,
                                value: '3.5"',
                                id: '12107451',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['12107451'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127044',
                        type: 'enum',
                        name: 'Назначение',
                        xslname: 'TypeOfUsage',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 4,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 20,
                                found: 1,
                                value: 'для настольного компьютера',
                                id: '12107443',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['12107443'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '15277575',
                        type: 'boolean',
                        name: 'Игровой',
                        xslname: 'gaming',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 5,
                        noffers: 1,
                        values: [
                            {
                                initialFound: 0,
                                found: 0,
                                value: '1',
                                id: '1',
                            },
                            {
                                initialFound: 0,
                                found: 0,
                                value: '0',
                                id: '0',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '6319041',
                        type: 'enum',
                        name: 'Тип',
                        xslname: 'TypeHDDSSD',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 6,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 0,
                                found: 1,
                                value: 'HDD',
                                id: '12107475',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['12107475'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '7747587',
                        type: 'enum',
                        name: 'Интерфейс SATA',
                        xslname: 'SATAint',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 7,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 11,
                                found: 1,
                                value: 'SATA 6Gb/s',
                                id: '12107549',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['12107549'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127055',
                        type: 'boolean',
                        name: 'Интерфейс SAS',
                        xslname: 'SASInt',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 8,
                        noffers: 1,
                        values: [
                            {
                                initialFound: 0,
                                found: 0,
                                value: '1',
                                id: '1',
                            },
                            {
                                initialFound: 0,
                                found: 0,
                                value: '0',
                                id: '0',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '17688041',
                        type: 'enum',
                        name: 'Объем буфера, МБ',
                        xslname: 'buffer_volume',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 10,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 39,
                                found: 1,
                                value: '256',
                                id: '17688070',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['17688070'],
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '5127070',
                        type: 'number',
                        name: 'Скорость вращения',
                        xslname: 'Speed',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        unit: 'об/мин',
                        position: 11,
                        noffers: 1,
                        precision: 0,
                        values: [
                            {
                                ranges: '7200, 7500',
                                max: '7200',
                                initialMax: '7200',
                                initialMin: '7200',
                                min: '7200',
                                id: 'found',
                            },
                        ],
                        meta: {},
                    },
                    {
                        id: '12782797',
                        type: 'enum',
                        name: 'Линейка',
                        xslname: 'vendor_line',
                        subType: '',
                        kind: 1,
                        isGuruLight: true,
                        position: 12,
                        noffers: 1,
                        valuesCount: 1,
                        values: [
                            {
                                initialFound: 1,
                                popularity: 15,
                                found: 1,
                                value: 'Barracuda',
                                vendor: {
                                    name: 'Seagate',
                                    entity: 'vendor',
                                    id: 686779,
                                },
                                id: '13733421',
                            },
                        ],
                        valuesGroups: [
                            {
                                type: 'all',
                                valuesIds: ['13733421'],
                            },
                        ],
                        meta: {},
                    },
                ],
                meta: {},
                type: 'model',
                id: 130084187,
                modelName: {
                    raw: 'ST2000DM008',
                },
                modelCreator: 'market',
                offers: {
                    count: 64,
                    cutPriceCount: 0,
                    skuOffersCount: 0,
                },
                isNew: false,
                prices: {
                    min: '4020',
                    max: '5696',
                    currency: 'RUR',
                    avg: '4670',
                },
                opinions: 111,
                rating: 4.5,
                preciseRating: 4.53,
                ratingCount: 581,
                reviews: 0,
                reasonsToBuy: [
                    {
                        value: 11.00380039,
                        type: 'consumerFactor',
                        id: 'bestseller',
                    },
                    {
                        value: 660.2280273,
                        type: 'statFactor',
                        id: 'bought_n_times',
                    },
                    {
                        value: 17770,
                        type: 'statFactor',
                        id: 'viewed_n_times',
                    },
                ],
                specs: {
                    full: [
                        {
                            groupName: 'Общие характеристики',
                            groupSpecs: [
                                {
                                    desc:
                                        'Все накопители (или жесткие диски, как их традиционно называют) можно разбить на три типа: HDD, SSD и гибридный.',
                                    mainProperty: true,
                                    name: 'Тип',
                                    usedParams: [
                                        {
                                            id: 6319041,
                                            name: 'Тип',
                                        },
                                    ],
                                    value: 'HDD',
                                },
                                {
                                    name: 'Поддержка секторов размером 4 КБ',
                                    usedParams: [
                                        {
                                            id: 6519465,
                                            name: 'Поддержка секторов размером 4 КБ',
                                        },
                                    ],
                                    value: 'есть',
                                },
                                {
                                    desc:
                                        'Жесткие диски условно можно разделить на четыре типа: внешние, диски для настольных компьютеров, для ноутбуков и для серверов. Также существуют сетевые накопители (NAS), которые могут включать несколько жестких дисков, объединенных в массив (обычно RAID). Каждый тип обладает конструктивными особенностями, делающими его применение наиболее целесообразным в определенных случаях.',
                                    mainProperty: true,
                                    name: 'Назначение',
                                    usedParams: [
                                        {
                                            id: 5127044,
                                            name: 'Назначение',
                                        },
                                    ],
                                    value: 'для настольного компьютера',
                                },
                                {
                                    desc:
                                        'Все выпускаемые жесткие диски имеют стандартные размеры и посадочные отверстия для крепления. В ПК, ноутбуках или серверах для установки жесткого диска имеются специальные установочные места определенного форм-фактора.',
                                    mainProperty: true,
                                    name: 'Форм-фактор',
                                    usedParams: [
                                        {
                                            id: 5127045,
                                            name: 'Форм-фактор',
                                        },
                                    ],
                                    value: '3.5"',
                                },
                            ],
                        },
                        {
                            groupName: 'Характеристики накопителя',
                            groupSpecs: [
                                {
                                    mainProperty: true,
                                    name: 'Объем',
                                    usedParams: [
                                        {
                                            id: 5127047,
                                            name: 'Емкость (точно)',
                                        },
                                    ],
                                    value: '2000 ГБ',
                                },
                                {
                                    mainProperty: true,
                                    name: 'Скорость записи/Скорость чтения',
                                    usedParams: [
                                        {
                                            id: 5127072,
                                            name: 'Скорость чтения',
                                        },
                                        {
                                            id: 5127071,
                                            name: 'Скорость записи',
                                        },
                                    ],
                                    value: '220/220 МБ/с',
                                },
                                {
                                    desc:
                                        'Современные жесткие диски обязательно имеют оперативную память, которую называют кэшем или буфером. Это память, предназначенная для хранения данных, обращение к которым происходит наиболее часто. Данные при этом считываются не с дисковой пластины, а из буфера, что обеспечивает более высокую скорость передачи данных.',
                                    mainProperty: true,
                                    name: 'Объем буферной памяти',
                                    usedParams: [
                                        {
                                            id: 17688041,
                                            name: 'Объем буфера, МБ',
                                        },
                                    ],
                                    value: '256 МБ',
                                },
                                {
                                    name: 'Количество головок',
                                    usedParams: [
                                        {
                                            id: 5127083,
                                            name: 'Количество головок',
                                        },
                                    ],
                                    value: '2',
                                },
                                {
                                    name: 'Количество пластин',
                                    usedParams: [
                                        {
                                            id: 5127084,
                                            name: 'Количество пластин',
                                        },
                                    ],
                                    value: '1',
                                },
                                {
                                    desc:
                                        'Параметр, характеризующий скорость вращения шпинделя жесткого диска. Чем больше этот параметр, тем быстрее происходит процесс обращения к информации, хранящейся на винчестере.',
                                    mainProperty: true,
                                    name: 'Скорость вращения',
                                    usedParams: [
                                        {
                                            id: 5127070,
                                            name: 'Скорость вращения',
                                        },
                                    ],
                                    value: '7200 rpm',
                                },
                            ],
                        },
                        {
                            groupName: 'Интерфейс',
                            groupSpecs: [
                                {
                                    mainProperty: true,
                                    name: 'Подключение',
                                    usedParams: [
                                        {
                                            id: 5127062,
                                            name: 'Интерфейс Ethernet',
                                        },
                                        {
                                            id: 5127065,
                                            name: 'ExpressCard/34',
                                        },
                                        {
                                            id: 5127063,
                                            name: 'Интерфейс Fibre Channel',
                                        },
                                        {
                                            id: 5127059,
                                            name: 'Интерфейс FireWire 800',
                                        },
                                        {
                                            id: 5127057,
                                            name: 'Интерфейс FireWire',
                                        },
                                        {
                                            id: 5127052,
                                            name: 'Интерфейс IDE',
                                        },
                                        {
                                            id: 5127058,
                                            name: 'Количество интерфейсов FireWire',
                                        },
                                        {
                                            id: 5127061,
                                            name: 'Количество интерфейсов FireWire 800',
                                        },
                                        {
                                            id: 6275045,
                                            name: 'Интерфейс PCI-E',
                                        },
                                        {
                                            id: 6280970,
                                            name: 'Тип PCI-E',
                                        },
                                        {
                                            id: 5127055,
                                            name: 'Интерфейс SAS',
                                        },
                                        {
                                            id: 7747587,
                                            name: 'Интерфейс SATA',
                                        },
                                        {
                                            id: 5127053,
                                            name: 'Интерфейс SCSI',
                                        },
                                        {
                                            id: 10771605,
                                            name: 'Интерфейс Thunderbolt',
                                        },
                                        {
                                            id: 5127054,
                                            name: 'Тип SCSI',
                                        },
                                        {
                                            id: 7795327,
                                            name: 'Интерфейс USB',
                                        },
                                        {
                                            id: 6413580,
                                            name: 'Интерфейс ZIF 40 pin',
                                        },
                                        {
                                            id: 5127064,
                                            name: 'Интерфейс eSATA',
                                        },
                                        {
                                            id: 6280969,
                                            name: 'Интерфейс mini PCI-E',
                                        },
                                    ],
                                    value: 'SATA 6Gbit/s',
                                },
                                {
                                    desc:
                                        'Максимальная скорость передачи данных внешнего интерфейса накопителя. Нужно отметить, что реальная скорость записи и чтения данных обычно меньше, чем скорость интерфейса.',
                                    name: 'Макс. скорость интерфейса',
                                    usedParams: [
                                        {
                                            id: 5127073,
                                            name: 'Макс. скорость интерфейса',
                                        },
                                    ],
                                    value: '600 МБ/с',
                                },
                                {
                                    name: 'Поддержка NCQ',
                                    usedParams: [
                                        {
                                            id: 5127087,
                                            name: 'Поддержка NCQ',
                                        },
                                    ],
                                    value: 'есть',
                                },
                            ],
                        },
                        {
                            groupName: 'Механика/Надежность',
                            groupSpecs: [
                                {
                                    name: 'Максимальная рабочая температура',
                                    usedParams: [
                                        {
                                            id: 7269794,
                                            name: 'Макс. рабочая температура',
                                        },
                                        {
                                            id: 7269657,
                                            name: 'Мин. рабочая температура',
                                        },
                                    ],
                                    value: '60 °C',
                                },
                            ],
                        },
                        {
                            groupName: 'Дополнительно',
                            groupSpecs: [
                                {
                                    name: 'Потребляемая мощность',
                                    usedParams: [
                                        {
                                            id: 7269859,
                                            name: 'Потребляемая мощность в спящем режиме',
                                        },
                                        {
                                            id: 6088346,
                                            name: 'Потребляемая мощность',
                                        },
                                    ],
                                    value: '4.30 Вт',
                                },
                                {
                                    name: 'Размеры (ШхВхД)',
                                    usedParams: [
                                        {
                                            id: 5127095,
                                            name: 'Высота',
                                        },
                                        {
                                            id: 5127096,
                                            name: 'Длина',
                                        },
                                        {
                                            id: 5127094,
                                            name: 'Ширина',
                                        },
                                    ],
                                    value: '101.6x20.2x146.99 мм',
                                },
                                {
                                    name: 'Вес',
                                    usedParams: [
                                        {
                                            id: 5127097,
                                            name: 'Вес',
                                        },
                                    ],
                                    value: '415 г',
                                },
                                {
                                    name: 'Срок службы',
                                    usedParams: [
                                        {
                                            id: 16000938,
                                            name: 'Срок службы',
                                        },
                                        {
                                            id: 16312663,
                                            name: 'Дополнительные условия использования',
                                        },
                                        {
                                            id: 16312646,
                                            name: 'Единица измерения срока службы',
                                        },
                                    ],
                                    value: '12 мес.',
                                },
                                {
                                    name: 'Гарантийный срок',
                                    usedParams: [
                                        {
                                            id: 16000940,
                                            name: 'Гарантийный срок',
                                        },
                                        {
                                            id: 16312664,
                                            name: 'Дополнительные условия гарантии',
                                        },
                                        {
                                            id: 16312655,
                                            name: 'Единица измерения гарантийного срока',
                                        },
                                    ],
                                    value: '24 мес., официальная двухлетняя ограниченная гарантия',
                                },
                            ],
                        },
                    ],
                },
                lingua: {
                    type: {
                        nominative: '',
                        genitive: '',
                        dative: '',
                        accusative: '',
                    },
                },
                retailersCount: 84,
                skuStats: {
                    totalCount: 1,
                    beforeFiltersCount: 1,
                    afterFiltersCount: 1,
                },
                promo: {
                    whitePromoCount: 0,
                },
            },
        ],
    },
};
module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
