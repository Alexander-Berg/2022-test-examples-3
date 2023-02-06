import moment from 'moment';

import StationType from '../../interfaces/state/station/StationType';
import StationSubtype from '../../interfaces/state/station/StationSubtype';
import IStationFromApi from '../../interfaces/state/station/IStationFromApi';
import IApi from '../../interfaces/api/IApi';
import IRouteMiddlewareParams from '../../interfaces/router/IRouteMiddlewareParams';
import StationReqQuery from '../../interfaces/router/StationReqQuery';
import StationReqParams from '../../interfaces/router/StationReqParams';
import EnvironmentType from '../../interfaces/EnvironmentType';

import {getStore, getReq, getRes, getNext, getApi} from '../utils/testUtils';
import getQuery from '../../lib/url/getQuery';
import station from '../station';

interface IUrl {
    from: string;

    to?: string;
    status?: number;
}

describe('Контрольный список редиректов со старого стека на новый', () => {
    const urls: IUrl[] = [
        {
            from: 'https://rasp.yandex.ru/station/2000003?start=2020-06-04T09:00:00&span=5',
            to: 'https://rasp.yandex.ru/station/2000003/?date=2020-06-04',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/?filter=all&start=2020-06-04T09:00:00&event=departure',
            to: 'https://t.rasp.yandex.ru/station/2000003/?date=2020-06-04',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003?span=tomorrow&type=train&event=departure',
            to: 'https://rasp.yandex.ru/station/2000003/?date=tomorrow',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/train/?event=departure&span=tomorrow',
            to: 'https://t.rasp.yandex.ru/station/2000003/?date=tomorrow',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/train/?start=2020-06-03T09:00:00&span=5&event=departure',
            to: 'https://rasp.yandex.ru/station/2000003/?date=2020-06-03',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/train/?start=2020-06-03T09:00:00&span=5&event=departure',
            to: 'https://t.rasp.yandex.ru/station/2000003/?date=2020-06-03',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/train/?span=day',
            to: 'https://t.rasp.yandex.ru/station/2000003/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/train/?span=schedule',
            to: 'https://t.rasp.yandex.ru/station/2000003/?date=all-days',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?span=day&type=suburban',
            to: 'https://rasp.yandex.ru/station/2000003/suburban/',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?type=tablo',
            to: 'https://rasp.yandex.ru/station/2000003/tablo/',
            status: 301,
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/suburban/?filter=today',
            to: 'https://t.rasp.yandex.ru/station/2000003/suburban/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/suburban/?filter=',
            to: 'https://t.rasp.yandex.ru/station/2000003/suburban/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/suburban/?filter=all',
            to: 'https://t.rasp.yandex.ru/station/2000003/suburban/?date=all-days',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/?event=arrival',
            to: undefined,
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/suburban',
            to: 'https://rasp.yandex.ru/station/2000003/suburban/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/suburban',
            to: 'https://t.rasp.yandex.ru/station/2000003/suburban/',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?date=tomorrow&event=arrival&direction=mars',
            to: 'https://rasp.yandex.ru/station/2000003/?event=arrival&date=tomorrow&direction=mars',
        },
        // Редирект направления для параметра subdir (используется на старом стеке)
        {
            from: 'https://rasp.yandex.ru/station/2000001?type=suburban&direction=msk_gor&span=schedule&subdir=Горьковское+направление',
            to: 'https://rasp.yandex.ru/station/2000001/suburban/?date=all-days&direction=Горьковское+направление',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/2000003/?date=today',
            to: 'https://t.rasp.yandex.ru/station/2000003/',
        },
        {
            from: 'https://rasp.yandex.ru/station/9603766/?type=train',
            to: 'https://rasp.yandex.ru/station/9603766/',
            status: 302,
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9603766/?type=train',
            to: 'https://t.rasp.yandex.ru/station/9603766/',
            status: 302,
        },
        {
            from: 'https://rasp.yandex.ru/station/9603766/suburban',
            to: 'https://rasp.yandex.ru/station/9603766/',
            status: 302,
        },
        {
            from: 'https://rasp.yandex.ru/station/9603766/?type=suburban',
            to: 'https://rasp.yandex.ru/station/9603766/',
            status: 301,
        },
        {
            from: 'https://rasp.yandex.ru/station/9600645/',
            to: 'https://rasp.yandex.ru/info/station/9600645',
            status: 302,
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?utm_campaign=campaign&utm_chegototam=1&utm_content=content&utm_medium=medium&utm_source=test&utm_term=term',
            to: undefined,
        },
        {
            // Если урл отличается только utm-метками, которые идут последними параметрами, то не редиректим на урл с метками в алфавитном порядке
            from: 'https://rasp.yandex.ru/station/2000003/?utm_medium=medium&utm_content=content&from=koldunshik',
            to: undefined,
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?utm_medium=medium&from=koldunshik&event=arrival',
            to: 'https://rasp.yandex.ru/station/2000003/?event=arrival&from=koldunshik&utm_medium=medium',
        },
        {
            // Тест, чтобы удостовериться в правильности логики, которая не редиректит, если урлы отличаются только параметрами отслеживания в конце урлов
            from: 'https://rasp.yandex.ru/station/2000003/?date=all-days&utm_medium=medium&span=schedule',
            to: 'https://rasp.yandex.ru/station/2000003/?date=all-days&utm_medium=medium',
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/?from=koldunshik',
            to: undefined,
        },
        {
            from: 'https://rasp.yandex.ru/station/2000003/suburban?utm_medium=medium&from=koldunshik',
            to: 'https://rasp.yandex.ru/station/2000003/suburban/?from=koldunshik&utm_medium=medium',
        },
        {
            from: 'https://rasp.yandex.ru/station/9600370/?type=suburban',
            to: 'https://rasp.yandex.ru/station/9600370/',
        },
        // Удаляем параметр direction для нежелезнодорожных станций
        {
            from: 'https://rasp.yandex.ru/station/9600370?span=tomorrow&direction=на+Екатеринбург&type=suburban',
            to: 'https://rasp.yandex.ru/station/9600370/?date=tomorrow',
        },
        {
            from: 'https://rasp.yandex.ru/station/9600213/suburban/?type=suburban',
            to: 'https://rasp.yandex.ru/station/9600213/',
        },
        {
            from: 'https://rasp.yandex.ru/station/9600213?type=tablo',
            to: 'https://rasp.yandex.ru/station/9600213/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9600213/suburban/',
            to: 'https://t.rasp.yandex.ru/station/9600213/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9600213/plane/?event=arrival',
            to: 'https://t.rasp.yandex.ru/station/9600213/?event=arrival',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9600213/plane/?event=departure',
            to: 'https://t.rasp.yandex.ru/station/9600213/',
        },
        {
            // Редирект с цифрового обозначения терминала на буквенный
            from: 'https://rasp.yandex.ru/station/9600213/?terminal=3',
            to: 'https://rasp.yandex.ru/station/9600213/?terminal=D',
        },
        {
            // Параметры должны сортироваться в определенном порядке
            from: 'https://rasp.yandex.ru/station/9600213/?date=tomorrow&terminal=D&event=arrival&time=00:00-02:00&search=test',
            to: 'https://rasp.yandex.ru/station/9600213/?event=arrival&terminal=D&date=tomorrow&time=00%3A00-02%3A00&search=test',
        },
        {
            // В случае неверного параметра time редиректим 302 на урл без него
            from: 'https://rasp.yandex.ru/station/9600213/?date=tomorrow&time=00:15-02:00',
            to: 'https://rasp.yandex.ru/station/9600213/?date=tomorrow',
            status: 302,
        },
        {
            // На таче нет поиска по ниткам, поэтому редиректим на урл без него
            from: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow&search=test',
            to: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow',
            status: 302,
        },
        {
            // Если нет терминала в ответе, редиректим на урл без терминала
            from: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow&terminal=B',
            to: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow',
            status: 301,
        },
        {
            // Если нет терминала в ответе, редиректим на урл без терминала, не должно возникать повторных редиректов
            from: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow&terminal=B&search=test',
            to: 'https://t.rasp.yandex.ru/station/9600213/?date=tomorrow',
            status: 302,
        },
        {
            from: 'https://rasp.yandex.ru/station/9600379/?terminal=1',
            to: 'https://rasp.yandex.ru/station/9600379/',
        },
        {
            from: 'https://rasp.yandex.ru/station/9600379/?terminal=B',
            to: 'https://rasp.yandex.ru/station/9600379/',
        },
        {
            from: 'https://rasp.yandex.ru/station/9600213/?searchTab=route&event=arrival',
            to: 'https://rasp.yandex.ru/station/9600213/?event=arrival',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9600213/?type=schedule',
            to: 'https://t.rasp.yandex.ru/station/9600213/',
        },
        // Страница автобусной станции
        {
            from: 'https://rasp.yandex.ru/station/9635953/?span=day',
            to: 'https://rasp.yandex.ru/station/9635953/',
        },
        {
            from: 'https://rasp.yandex.ru/station/9635953/?span=tomorrow',
            to: 'https://rasp.yandex.ru/station/9635953/?date=tomorrow',
        },
        {
            from: 'https://rasp.yandex.ru/station/9635953/?span=schedule',
            to: 'https://rasp.yandex.ru/station/9635953/?date=all-days',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9635953/?filter=',
            to: 'https://t.rasp.yandex.ru/station/9635953/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9635953/?filter=today',
            to: 'https://t.rasp.yandex.ru/station/9635953/',
        },
        {
            from: 'https://t.rasp.yandex.ru/station/9635953/?filter=all',
            to: 'https://t.rasp.yandex.ru/station/9635953/?date=all-days',
        },
        {
            from: 'https://rasp.yandex.ru/station/9635953/bus/?span=schedule',
            to: 'https://rasp.yandex.ru/station/9635953/?date=all-days',
        },
        // Фильтр по остановке
        {
            from: 'https://rasp.yandex.ru/station/9635953/?stop=1',
            to: undefined,
        },
        // Редирект для фильтра по несуществующей остановке
        {
            from: 'https://rasp.yandex.ru/station/9635953/?stop=2',
            to: 'https://rasp.yandex.ru/station/9635953/',
            status: 302,
        },
    ];

    const next = getNext();

    function getSubtype(
        subtypes: StationSubtype[],
        mainSubtype: StationSubtype,
        subtype?: StationSubtype,
    ): StationSubtype | undefined {
        if (!subtype) {
            return mainSubtype;
        }

        if (subtypes.includes(subtype)) {
            return subtype;
        }
    }

    const api = getApi({
        execStation: ({stationId, subtype}) => {
            switch (stationId) {
                case 2000001:
                case 2000003: {
                    const subtypes = [
                        StationSubtype.suburban,
                        StationSubtype.train,
                        StationSubtype.tablo,
                    ];
                    const mainSubtype = StationSubtype.train;

                    return Promise.resolve({
                        type: StationType.railroad,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                    } as IStationFromApi);
                }

                case 9603766: {
                    const subtypes = [StationSubtype.suburban];
                    const mainSubtype = StationSubtype.suburban;

                    return Promise.resolve({
                        type: StationType.railroad,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                    } as IStationFromApi);
                }

                case 9600645: {
                    const subtypes = [
                        StationSubtype.suburban,
                        StationSubtype.train,
                    ];

                    return Promise.resolve({
                        type: StationType.railroad,
                        subtypes,
                        notEnoughInfo: true,
                    } as IStationFromApi);
                }

                case 9600213: {
                    // Шереметьево
                    const subtypes = [StationSubtype.plane];
                    const mainSubtype = StationSubtype.plane;

                    return Promise.resolve({
                        type: StationType.plane,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                        terminals: [
                            {
                                id: 3,
                                name: 'D',
                            },
                        ],
                    } as IStationFromApi);
                }

                case 9600379: {
                    // Аэропорт Казань
                    const subtypes = [StationSubtype.plane];
                    const mainSubtype = StationSubtype.plane;

                    return Promise.resolve({
                        type: StationType.plane,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                        terminals: [],
                    } as unknown as IStationFromApi);
                }

                case 9600370: {
                    // Кольцово
                    const subtypes = [StationSubtype.plane];
                    const mainSubtype = StationSubtype.plane;

                    return Promise.resolve({
                        type: StationType.plane,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                        terminals: [],
                    } as unknown as IStationFromApi);
                }

                case 9635953: {
                    //  Екатеринбург (Автовокзал Северный)
                    const subtypes = [StationSubtype.schedule];
                    const mainSubtype = StationSubtype.schedule;

                    return Promise.resolve({
                        type: StationType.bus,
                        subtypes,
                        mainSubtype,
                        currentSubtype: getSubtype(
                            subtypes,
                            mainSubtype,
                            subtype,
                        ),
                    } as IStationFromApi);
                }
            }

            return Promise.resolve({} as IStationFromApi);
        },
        // execStation: () => Promise.resolve({} as IStationFromApi),
        execStationStops: () =>
            Promise.resolve({
                stops: [
                    {
                        id: 1,
                        title: 'testTitle',
                    },
                ],
            }),
        execStationCityStations: () => Promise.resolve(undefined),
        execStationPopularDirections2: () => Promise.resolve(undefined),
    } as unknown as IApi);

    async function getRedirect(url): Promise<{status?: number; url?: string}> {
        const match = url.match(
            /^https?:\/\/(t\.)?rasp\.yandex\.ru\/station\/(\d+)(?:\/(train|suburban|tablo))?/i,
        );

        if (!match) {
            return {};
        }

        const isMobile = Boolean(match[1]);
        const id = match[2];
        const subtype = match[3] || '';
        const params: StationReqParams = {
            id,
            subtype,
        };
        const query = getQuery<StationReqQuery>(url);

        const store = getStore({
            searchForm: {
                time: {
                    now: 1576137067523,
                    timezone: 'Europe/Moscow',
                },
            },
            flags: {},
            isTouch: isMobile,
            environment: {
                type: EnvironmentType.server,
            },
            page: {},
            station: {
                now: moment().format(),
                threads: [],
            },
        });

        const req = getReq({
            params,
            query,
            originalUrl: url.replace(/^https?:\/\/(t\.)?rasp\.yandex\.ru/i, ''),
        });
        const redirect = jest.fn();
        const res = getRes({redirect});

        await station({store, req, res, next, api} as IRouteMiddlewareParams);
        const [redirectStatus, redirectUrl] = redirect.mock.calls[0] || [];

        return {
            status: redirectStatus,
            url: redirectUrl
                ? `https://${isMobile ? 't.' : ''}rasp.yandex.ru${redirectUrl}`
                : redirectUrl,
        };
    }

    urls.forEach(({from, to, status = 301}) => {
        it(`url: ${from} to be ${to}`, async () => {
            const result = await getRedirect(from);
            const decodedUrl = result.url ? decodeURI(result.url) : result.url;

            // проверяем первый редирект
            expect(decodedUrl).toBe(to);

            if (result.url) {
                expect(result.status).toBe(status);

                // проверяем не возникнет ли повторного редиректа
                const secondResult = await getRedirect(result.url);

                expect(secondResult.url).toBeUndefined();
                expect(secondResult.status).toBeUndefined();
            }
        });
    });
});
