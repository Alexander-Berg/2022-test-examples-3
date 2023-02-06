import AllThreadType from '../../../interfaces/state/station/AllThreadType';
import StationEventList from '../../../interfaces/state/station/StationEventList';
import StationTime from '../../../interfaces/state/station/StationTime';
import DateRobot from '../../../interfaces/date/DateRobot';

import getFilteredThreads from '../getFilteredThreads';

const threads = [
    {
        eventDt: {
            time: '11:57',
            datetime: '2020-04-28T11:57:00+03:00',
        },
        number: '731М',
        title: 'Москва — Смоленск',
        transportType: 'train',
        canonicalUid: 'R_731M_112',
        departureFrom: '2020-04-28T11:57:00+03:00',
        transportSubtype: {
            code: 'lastdal',
            title: '«Ласточка»',
        },
        deluxeTrainTitle: '«Ласточка»',
        platform: '',
        companyId: 112,
    },
    {
        eventDt: {
            time: '07:36',
            datetime: '2020-04-28T07:36:00+03:00',
        },
        number: '7107',
        title: 'Москва (Белорусский вокзал) — Одинцово',
        transportType: 'suburban',
        canonicalUid: 'R_7107_2000006_g20_4',
        isExpress: true,
        isAeroExpress: false,
        stops: {
            text: 'Сколково (бывш. Инновационный центр), Одинцово',
            type: 'stops',
        },
        departureFrom: '2020-04-28T07:36:00+03:00',
        transportSubtype: {
            title: 'экспресс РЭКС',
        },
        platform: '',
        companyId: 153,
    },
    {
        eventDt: {
            time: '07:36',
            datetime: '2020-04-28T07:36:00+03:00',
        },
        number: '7107',
        title: 'Москва (Белорусский вокзал) — Одинцово',
        transportType: 'suburban',
        canonicalUid: 'R_7107_2000006_g20_4',
        isExpress: true,
        isAeroExpress: true,
        stops: {
            text: 'Сколково (бывш. Инновационный центр), Одинцово',
            type: 'stops',
        },
        departureFrom: '2020-04-28T07:36:00+03:00',
        transportSubtype: {
            title: 'экспресс РЭКС',
        },
        platform: '',
        companyId: 153,
    },
    {
        eventDt: {
            time: '06:20',
            datetime: '2020-04-28T06:20:00+03:00',
        },
        number: 'SU 1138',
        transportType: 'plane',
        companyId: 26,
        terminalName: 'B',
        aviaLink:
            'https://front.avia.tst.yandex.ru/flights/SU-1138/?from=SVO&lang=ru&when=2020-04-28',
        routeStations: [{settlement: 'Сочи', iataCode: 'AER', title: 'Адлер'}],
        status: {status: 'unknown'},
    },
    {
        eventDt: {
            time: '12:00',
            datetime: '2020-04-28T12:00:00+03:00',
        },
        number: 'SU 1320',
        transportType: 'plane',
        companyId: 431,
        terminalName: 'C',
        aviaLink:
            'https://front.avia.tst.yandex.ru/flights/SU-1320/?from=SVO&lang=ru&when=2020-04-28',
        routeStations: [
            {settlement: 'Мурманск', iataCode: 'MMK', title: 'Мурманск'},
            {settlement: 'Екатеринбург', iataCode: 'SVO', title: 'Кольцово'},
        ],
        codeshares: [{number: 'KL 2854', companyId: 1418}],
        status: {
            status: 'departed',
            actualDt: '2020-04-28T07:15:00+03:00',
            gate: '113',
        },
    },
] as unknown as AllThreadType[];

const companiesById = {
    26: {
        title: 'Аэрофлот',
        url: 'http://www.aeroflot.ru/',
        hidden: false,
        id: 26,
        icon: 'data/company/icon/aeroflot-fav_2.png',
    },
    431: {
        title: 'Aeromexicо',
        url: 'http://www.aeromexico.com/',
        hidden: false,
        id: 431,
        icon: 'data/company/icon/Aeromexico-fav.png',
    },
    1418: {
        title: 'KLM',
        url: '',
        hidden: false,
        id: 1418,
        icon: 'data/company/icon/KLM-fav.png',
    },
};

const event = StationEventList.departure;

const commonSearchParams = {
    event,
    threads,
    companiesById,
    time: null,
    terminalName: '',
    isMobile: false,
};

describe('getFilteredThreads', () => {
    it('Не должны ничего находить по undefined', () => {
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'undefined',
                isMobile: false,
            }),
        ).toEqual([]);

        // На таче нет поиска, поэтому поиск по строке игнорируется
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'undefined',
                isMobile: true,
            }),
        ).toEqual(threads);
    });

    it('Не ищем по остановкам', () => {
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Инновационный',
            }),
        ).toEqual([]);
    });

    it('Поиск по поездам и элетричкам', () => {
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'ласточка',
            }),
        ).toEqual([threads[0]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Смоленск',
            }),
        ).toEqual([threads[0]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Белорусский',
            }),
        ).toEqual([threads[1], threads[2]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'аэроэксп',
            }),
        ).toEqual([threads[2]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'РЭКС',
            }),
        ).toEqual([threads[1]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: '  РЭКС ',
            }),
        ).toEqual([threads[1]]);
    });

    it('Поиск по самолетам', () => {
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: '1138',
            }),
        ).toEqual([threads[3]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Аэрофлот',
            }),
        ).toEqual([threads[3]]);

        // departure
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Екатеринбург',
            }),
        ).toEqual([threads[4]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'Мурманск',
            }),
        ).toEqual([]);

        // arrival
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                event: StationEventList.arrival,
                search: 'Екатеринбург',
            }),
        ).toEqual([]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                event: StationEventList.arrival,
                search: 'Мурманск',
            }),
        ).toEqual([threads[4]]);

        // codeshares
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: '2854',
            }),
        ).toEqual([threads[4]]);

        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: 'KLM',
            }),
        ).toEqual([threads[4]]);
    });

    it('Фильтрация по терминалам', () => {
        const commonParams = {
            event,
            threads,
            companiesById,
            time: null,
            isMobile: false,
            search: '',
        };

        expect(
            getFilteredThreads({
                ...commonParams,
                terminalName: 'B',
            }),
        ).toEqual([threads[0], threads[1], threads[2], threads[3]]);

        expect(
            getFilteredThreads({
                ...commonParams,
                terminalName: 'B',
                isMobile: true,
            }),
        ).toEqual([threads[0], threads[1], threads[2], threads[3]]);

        expect(
            getFilteredThreads({
                ...commonParams,
                terminalName: '',
            }),
        ).toEqual(threads);
    });

    it('Фильтрация по времени', () => {
        const commonParams = {
            event,
            threads,
            companiesById,
            isMobile: false,
            search: '',
            terminalName: '',
            whenDate: '2020-04-28' as DateRobot,
        };

        expect(
            getFilteredThreads({
                ...commonParams,
                time: StationTime.all,
            }),
        ).toEqual(threads);

        // Для поездов в интерфейсе нет фильтра по времени, так что фильтр по времени для них игнорируется
        expect(
            getFilteredThreads({
                ...commonParams,
                time: StationTime['p6-8'],
            }),
        ).toEqual([threads[0], threads[1], threads[2], threads[3]]);

        // Пограничное время, типа 12:00 входит в оба диапазона: 10:00-12:00 и 12:00-14:00
        expect(
            getFilteredThreads({
                ...commonParams,
                time: StationTime['p10-12'],
            }),
        ).toEqual([threads[0], threads[1], threads[2], threads[4]]);

        expect(
            getFilteredThreads({
                ...commonParams,
                time: StationTime['p12-14'],
            }),
        ).toEqual([threads[0], threads[1], threads[2], threads[4]]);

        // Должен корректно работать последний интервал с 00:00
        const threads12 = [
            {
                eventDt: {time: '23:50'},
                transportType: 'plane',
            },
        ] as unknown as AllThreadType[];

        expect(
            getFilteredThreads({
                ...commonParams,
                threads: threads12,
                time: StationTime['p22-00'],
            }),
        ).toEqual(threads12);

        expect(
            getFilteredThreads({
                ...commonParams,
                threads: threads12,
                time: StationTime['p0-2'],
            }),
        ).toEqual([]);
    });

    it('Фильтрация по времени: Рейсы с вечера вчерашнего дня, фактически прилетевшие сегодня, должны попадать в интервал 00:00-02:00 И НЕ должны попадать в интервал 22:00-24:00', () => {
        const yesterdaysThread = [
            {
                eventDt: {time: '23:50', datetime: '2020-04-27T22:00:00+03:00'},
                transportType: 'plane',
            },
        ] as unknown as AllThreadType[];

        // Вчерашний рейс должен попадать в интервал 00:00-02:00
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: '',
                whenDate: '2020-04-28' as DateRobot,
                threads: yesterdaysThread,
                time: StationTime['p0-2'],
            }),
        ).toEqual(yesterdaysThread);

        // Вчерашний рейс НЕ должен попадать в интервал 22:00-24:00
        expect(
            getFilteredThreads({
                ...commonSearchParams,
                search: '',
                whenDate: '2020-04-28' as DateRobot,
                threads: yesterdaysThread,
                time: StationTime['p22-00'],
            }),
        ).toEqual([]);
    });
});
