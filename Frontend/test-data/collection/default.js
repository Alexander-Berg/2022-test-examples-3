const data = require('../../tools/data');

module.exports = data.createSnippet({
    block: 'markup',
    content: [
        {
            content_type: 'divider',
        },
        {
            block: 'title',
            size: 'm',
            content: 'Коллекция 1',
        },
        {
            content_type: 'divider',
        },
        {
            block: 'collection',
            items: [
                {
                    action: 'https://www.gismeteo.ru/search/',
                    block: 'form',
                    content: [
                        {
                            block: 'form-line',
                            content: [
                                {
                                    block: 'input',
                                    counters: {
                                        enter: {
                                            path: '/input/enter',
                                        },
                                    },
                                    name: 'text',
                                    placeholder: 'Поиск...',
                                    type: 'search',
                                },
                            ],
                        },
                    ],
                    type: 'default',
                },
                {
                    block: 'advert',
                    type: 'yandex',
                    id: 'R-A-196263-1',
                },
                {
                    block: 'weather',
                    icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloudy/svg',
                    notice: 'Малооблачно, ощущается как +16°',
                    temp: '+14',
                    title: 'Погода в Москве',
                },
                {
                    content_type: 'table',
                    rows: [
                        {
                            cells: [
                                {
                                    block: 'definition',
                                    term: 'Ощущение',
                                    desc: '+19',
                                },
                                {
                                    block: 'definition',
                                    term: 'Ветер',
                                    desc: '12 м/с СЗ',
                                },
                            ],
                        },
                        {
                            cells: [
                                {
                                    block: 'definition',
                                    term: 'Давление',
                                    desc: '760 мм рт. ст.',
                                },
                                {
                                    block: 'definition',
                                    term: 'Влажность',
                                    desc: '67%',
                                },
                            ],
                        },
                        {
                            cells: [
                                {
                                    block: 'definition',
                                    term: 'Вода',
                                    desc: '+3',
                                },
                                {
                                    block: 'definition',
                                    term: 'Геомагн. поля',
                                    desc: '2 балла',
                                },
                            ],
                        },
                    ],
                    style: 'invisible',
                },
            ],
        },
        {
            content_type: 'divider',
        },
        {
            block: 'title',
            size: 'm',
            content: 'Коллекция 2',
        },
        {
            content_type: 'divider',
        },
        {
            block: 'collection',
            items: [
                {
                    block: 'tabs',
                    items: [
                        {
                            text: 'Сейчас',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/now/',
                        },
                        {
                            text: 'Сегодня',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/',
                        },
                        {
                            text: 'Завтра',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/tomorrow/',
                        },
                        {
                            text: '3 дня',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/3-days/',
                        },
                        {
                            text: '10 дней',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/10-days/',
                        },
                        {
                            text: '2 недели',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/2-weeks/',
                        },
                        {
                            text: 'Месяц',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/month/',
                        },
                        {
                            text: 'Радар',
                            url: 'https://www.gismeteo.ru/nowcast-moscow-4368/',
                        },
                        {
                            text: 'Геомагнитное поле',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/gm/',
                        },
                        {
                            text: 'Выходные',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/weekend/',
                        },
                        {
                            text: 'Неделя',
                            url: 'https://www.gismeteo.ru/weather-moscow-4368/weekly/',
                        },
                    ],
                },
                {
                    block: 'histogram',
                    cols: [
                        {
                            caption: '-7.6',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#63d6ff',
                            title: '0:00',
                            value: 30,
                        },
                        {
                            caption: '6.7',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#effae4',
                            title: '3:00',
                            value: 80,
                        },
                        {
                            caption: '-9.5',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#63d6ff',
                            title: '6:00',
                            value: 30,
                        },
                        {
                            caption: '-8.9',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#63d6ff',
                            title: '9:00',
                            value: 50,
                        },
                        {
                            caption: '-8.6',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#63d6ff',
                            title: '12:00',
                            value: 33,
                        },
                        {
                            caption: '-7.1',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloud-cloud/svg',
                            color: '#63d6ff',
                            title: '15:00',
                            value: 22,
                        },
                        {
                            caption: '-10.8',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloudy/svg',
                            color: '#63d6ff',
                            title: '18:00',
                            value: 11,
                        },
                        {
                            caption: '-12.8',
                            icon: 'http://avatars.mdst.yandex.net/get-turbo/5150/gis-cloudy/svg',
                            color: '#63d6ff',
                            title: '21:00',
                            value: 9,
                        },
                    ],
                },
            ],
        },
        {
            content_type: 'divider',
        },
        {
            block: 'title',
            size: 'm',
            content: 'Коллекция 3',
        },
        {
            content_type: 'divider',
        },
        {
            block: 'collection',
            items: [
                {
                    block: 'snippet',
                    thumb: {
                        position: 'left',
                        ratio: '1x1',
                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                    },
                    title: 'Животные со странной внешностью, которые нас удивили',
                    url: 'https://www.gismeteo.ru/news/sobytiya/26242-zhivotnye-so-strannoy-vneshnostyu-kotorye-nas-udivili/',
                },
                {
                    block: 'snippet',
                    thumb: {
                        position: 'left',
                        ratio: '1x1',
                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                    },
                    title: 'В США произошло сильное землетрясение, объявлена угроза цунами',
                    url: 'https://www.gismeteo.ru/news/stihiynye-yavleniya/26243-v-ssha-proizoshlo-silnoe-zemletryasenie-obyavlena-ugroza-tsunami/',
                },
                {
                    block: 'snippet',
                    thumb: {
                        position: 'left',
                        ratio: '1x1',
                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                    },
                    title: 'Погода в Москве: минувшая ночь стала самой холодной с начала сезона',
                    url: 'https://www.gismeteo.ru/news/klimat/26241-pogoda-v-moskve-minuvshaya-noch-stala-samoy-holodnoy-s-nachala-sezona/',
                },
                {
                    block: 'snippet',
                    thumb: {
                        position: 'left',
                        ratio: '1x1',
                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                    },
                    title: 'Как формируются подводные сосульки: видео',
                    url: 'https://www.gismeteo.ru/news/sobytiya/17953-kak-formiruyutsya-podvodnye-sosulki-v-vodah-antarktiki-video/',
                },
                {
                    block: 'snippet',
                    thumb: {
                        position: 'left',
                        ratio: '1x1',
                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                    },
                    title: 'Сибирские супер-морозы вышли на максимум',
                    url: 'https://www.gismeteo.ru/news/klimat/26222-sibirskie-super-morozy-vyshli-na-maksimum/',
                },
            ],
        },
    ],
});
