import StationType from '../../../../interfaces/state/station/StationType';
import StationSubtype from '../../../../interfaces/state/station/StationSubtype';
import StationEventList from '../../../../interfaces/state/station/StationEventList';
import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';
import IStatePage from '../../../../interfaces/state/IStatePage';
import IStateStation from '../../../../interfaces/state/station/IStateStation';
import IStateFlags from '../../../../interfaces/state/flags/IStateFlags';
import ITerminal from '../../../../interfaces/state/station/ITerminal';

import getJsonLdCrumbsForStationPage from '../getJsonLdCrumbsForStationPage';

const page = {
    originUrl: 'https://rasp.yandex.ru',
    fullUrl: 'https://rasp.yandex.ru/station/2000006/?event=arrival',
};

const tld = Tld.ru;
const language = Lang.ru;

describe('getJsonLdCrumbsForStationPage', () => {
    it(`Верный JSON-LD для страницы станции, содержащий:
        ссылку на вид транспорта,
        ссылку на город,
        ссылку на станцию,
        ссылку на станцию прибытие`, () => {
        const station = {
            id: 2000006,
            type: StationType.railroad,
            title: 'Белорусский вокзал',
            currentSubtype: StationSubtype.train,
            mainSubtype: StationSubtype.train,
            settlement: {
                slug: 'moscow',
                title: 'Москва',
            },
            event: StationEventList.arrival,
            cityData: {
                settlement: {
                    titleGenitive: 'Москвы',
                    title: 'Москва',
                },
                cityStations: {
                    train: [
                        {
                            id: 2000001,
                            title: 'Курский вокзал',
                        },
                        {
                            id: 2000002,
                            title: 'Ярославский вокзал',
                        },
                    ],
                },
            },
        };

        const result = getJsonLdCrumbsForStationPage(
            tld,
            language,
            page as IStatePage,
            station as IStateStation,
            {} as IStateFlags,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru/train',
                        name: 'Расписание поездов',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/train/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/2000006/',
                        name: 'Белорусский вокзал',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': page.fullUrl,
                        name: 'Прибытие',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD для страницы станции, если currentSubtype !== mainSubtype, например для страницы электричек, содержащий:
        ссылку на вид транспорта - электрички,
        ссылку на город,
        ссылку на станцию,
        ссылку на станцию прибытие`, () => {
        const station = {
            id: 2000006,
            type: StationType.railroad,
            title: 'Белорусский вокзал',
            currentSubtype: StationSubtype.suburban,
            mainSubtype: StationSubtype.train,
            settlement: {
                slug: 'moscow',
                title: 'Москва',
            },
            event: StationEventList.arrival,
            cityData: {
                settlement: {
                    titleGenitive: 'Москвы',
                    title: 'Москва',
                },
                cityStations: {
                    train: [
                        {
                            id: 2000001,
                            title: 'Курский вокзал',
                        },
                        {
                            id: 2000002,
                            title: 'Ярославский вокзал',
                        },
                    ],
                },
            },
        };

        const suburbanPage = {
            ...page,
            fullUrl:
                'https://rasp.yandex.ru/station/2000006/suburban/?event=arrival',
        };

        const result = getJsonLdCrumbsForStationPage(
            tld,
            language,
            suburbanPage as IStatePage,
            station as IStateStation,
            {} as IStateFlags,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru/suburban',
                        name: 'Расписание электричек',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/suburban/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/2000006/suburban/',
                        name: 'Белорусский вокзал',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': suburbanPage.fullUrl,
                        name: 'Прибытие',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD со страницей аэропортов России,
        страницей города,
        страницей аэропорта на прибытие,
        страницей аэропорта на прибытие с выбранным терминалом`, () => {
        const stationPage = {
            originUrl: page.originUrl,
            fullUrl:
                'https://rasp.yandex.ru/station/9600213/?event=arrival&terminal=B',
        };
        const station = {
            id: 9600213,
            type: StationType.plane,
            title: 'Шереметьево',
            currentSubtype: StationSubtype.plane,
            mainSubtype: StationSubtype.plane,
            settlement: {
                slug: 'moscow',
                title: 'Москва',
            },
            event: StationEventList.arrival,
            terminals: [
                {
                    name: 'B',
                } as ITerminal,
                {
                    name: 'C',
                } as ITerminal,
            ],
            terminalName: 'B',
            cityData: {
                settlement: {
                    titleGenitive: 'Москвы',
                    title: 'Москва',
                },
                cityStations: {
                    plane: [
                        {
                            id: 9600216,
                            title: 'Домодедово',
                        },
                        {
                            id: 9600213,
                            title: 'Шереметьево',
                        },
                    ],
                },
            },
        };
        const flags = {};
        const result = getJsonLdCrumbsForStationPage(
            tld,
            language,
            stationPage as IStatePage,
            station as IStateStation,
            flags as IStateFlags,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane',
                        name: 'Расписание самолётов',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9600213/?event=arrival',
                        name: 'Шереметьево',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9600213/?event=arrival&terminal=B',
                        name: 'Терминал B',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD со страницей аэропортов России,
        страницей города,
        страницей аэропорта,
        страницей аэропорта на прибытие`, () => {
        const stationPage = {
            originUrl: page.originUrl,
            fullUrl: 'https://rasp.yandex.ru/station/9600213/?event=arrival',
        };
        const station = {
            id: 9600213,
            type: StationType.plane,
            title: 'Шереметьево',
            currentSubtype: StationSubtype.plane,
            mainSubtype: StationSubtype.plane,
            settlement: {
                slug: 'moscow',
                title: 'Москва',
            },
            event: StationEventList.arrival,
            cityData: {
                settlement: {
                    titleGenitive: 'Москвы',
                    title: 'Москва',
                },
                cityStations: {
                    plane: [
                        {
                            id: 9600216,
                            title: 'Домодедово',
                        },
                        {
                            id: 9600213,
                            title: 'Шереметьево',
                        },
                    ],
                },
            },
            terminals: [
                {
                    name: 'B',
                } as ITerminal,
                {
                    name: 'C',
                } as ITerminal,
            ],
            terminalName: '',
        };
        const flags = {};
        const result = getJsonLdCrumbsForStationPage(
            tld,
            language,
            stationPage as IStatePage,
            station as IStateStation,
            flags as IStateFlags,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane',
                        name: 'Расписание самолётов',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9600213/',
                        name: 'Шереметьево',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9600213/?event=arrival',
                        name: 'Прибытие',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD со страницей аэропортов России,
        страницей аэропорта`, () => {
        const stationPage = {
            originUrl: page.originUrl,
            fullUrl: 'https://rasp.yandex.ru/station/9600213',
        };
        const station = {
            id: 9600213,
            type: StationType.plane,
            title: 'Шереметьево',
            currentSubtype: StationSubtype.plane,
            mainSubtype: StationSubtype.plane,
            settlement: {
                slug: 'moscow',
                title: 'Москва',
            },
            event: StationEventList.departure,
            cityData: {
                settlement: {
                    titleGenitive: 'Москвы',
                    title: 'Москва',
                },
                cityStations: {
                    plane: [
                        {
                            id: 9600216,
                            title: 'Домодедово',
                        },
                        {
                            id: 9600213,
                            title: 'Шереметьево',
                        },
                    ],
                },
            },
            terminals: [
                {
                    name: 'B',
                } as ITerminal,
                {
                    name: 'C',
                } as ITerminal,
            ],
            terminalName: '',
        };
        const flags = {};
        const result = getJsonLdCrumbsForStationPage(
            tld,
            language,
            stationPage as IStatePage,
            station as IStateStation,
            flags as IStateFlags,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane',
                        name: 'Расписание самолётов',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9600213/',
                        name: 'Шереметьево',
                    },
                },
            ],
        });
    });
});
