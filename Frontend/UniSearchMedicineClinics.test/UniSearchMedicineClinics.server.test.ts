import { assert } from 'chai';
import type { ISnippetContext } from '@lib/Context';
import type { IPrivExternals, ISerpDocument } from '@typings';
import type {
    IUniSearchMedicineListSnippetItem,
} from '../../../UniSearchMedicine.typings';

import type { IUniSearchMedicineClinicsData } from '../UniSearchMedicineClinics.server';
import { AdapterUniSearchMedicineClinics } from '../UniSearchMedicineClinics.server';

type TSnippetData = IUniSearchMedicineClinicsData;

// Это надо будет как-то в хэлперы унести
interface IAdapterArguments {
    context: ISnippetContext;
    snippet: TSnippetData;
    document: ISerpDocument;
    privExternals: IPrivExternals;
}

// Пока не нужен тест на платформу, просто здесь делаем конкретный класс
class AdapterTest extends AdapterUniSearchMedicineClinics {
    App = () => null
}

const CLINICS: Record<string, IUniSearchMedicineListSnippetItem['clinics']> = {
    default: [
        {
            name: 'Медгород Чистые Пруды',
            open_hours: {
                text: 'пн-пт 07:30–21:00; сб,вс 07:30–19:00',
            },
            nearest_station: {
                color: 16744192,
                distance: '410 м',
                id: 'station__9858927',
                name: 'Тургеневская',
            },
            photos: {
                count: 14,
                photo: [
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2786941/2a00000174c5c14c42581e2e8810ee387c85/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2385630/2a00000174c5c14c61d666c53cda42ac4961/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2701558/2a00000174c5c14c7293feb663f2c60f916b/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2433982/2a00000174c5c14c898d157a2a4b0ecc391f/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4336915/2a0000017843dd212d33e2fd097d1d34d1e6/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4632172/2a0000017843dd68f6e50ac12ab0df9bc18d/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/3717246/2a0000017843ddbc6d5cb012b4cd249834b9/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4667561/2a0000017843dd42c661df50a19ca9c35470/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/4304228/2a0000017843de0f6f34517f9ddd69fd4c3e/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/5509094/2a0000017c8e947a9512806b02922c3c5310/%s',
                    },
                    {
                        urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2701879/2a00000174df59ecc5da1caadd445c6d6b3e/%s',
                    },
                ],
            },
            phone: '8 (495) 185-01-01',
            link: 'https://yandex.ru/maps/org/138687555688',
            address: 'Кривоколенный переулок, 9, стр. 1, Москва',
            logo: {
                urlTemplate: 'https://avatars.mds.yandex.net/get-altay/2389272/2a0000017402371887be8e02ed29a2e1f0cd/%s',
            },
            source_info: [
                {
                    id: 'docdoc.ru',
                    appointment_url: 'https://docdoc.ru/',
                },
            ],
        },
    ],
};

const SOURCES: Record<string, IUniSearchMedicineListSnippetItem['sources']> = {
    default: [
        {
            id: 'docdoc.ru',
            name: 'СберЗдоровье',
        },
    ],
};

describe('AdapterUniSearchMedicineClinics', () => {
    const adapterArguments: IAdapterArguments = {
        context: {
            expFlags: {},
            device: {},
            query: {},
        } as unknown as ISnippetContext,
        document: {} as ISerpDocument,
        snippet: {
            clinics: CLINICS.default,
            sources: SOURCES.default,
        } as TSnippetData,
        privExternals: {
            Counter: () => null,
            pushAssets: () => null,
        } as unknown as IPrivExternals,
    };

    describe('getAggregatorOffers', () => {
        it('should return offer props', () => {
            const adapter = new AdapterTest(adapterArguments);
            assert.deepEqual(adapter.getClinicProps([], []), [], 'array is not eampty');
            assert.deepEqual(
                adapter.getClinicProps(adapterArguments.snippet.clinics || [], adapterArguments.snippet.sources || []),
                [
                    {
                        name: 'Медгород Чистые Пруды',
                        address: 'Кривоколенный переулок, 9, стр. 1, Москва',
                        photo: 'https://avatars.mds.yandex.net/get-altay/2786941/2a00000174c5c14c42581e2e8810ee387c85/M',
                        link: 'https://yandex.ru/maps/org/138687555688',
                        rating: undefined,
                        ratingBase: undefined,
                        station: {
                            color: '#ff7f00',
                            name: 'м.\u00a0Тургеневская',
                            distance: '410 м',
                            id: 'station__9858927',
                        },
                        openHour: 'пн-пт 07:30–21:00; сб,вс 07:30–19:00',
                        logo: 'https://avatars.mds.yandex.net/get-altay/2389272/2a0000017402371887be8e02ed29a2e1f0cd/M',
                        metrikaProps: undefined,
                        minPrice: undefined,
                        sourcesInfo: [{
                            id: 'docdoc.ru',
                            appointmentUrl: 'https://docdoc.ru/',
                            price: undefined,
                            source: {
                                id: 'docdoc.ru',
                                name: 'СберЗдоровье',
                            },
                        }],
                        phone: undefined,
                        appointments: undefined,
                    },
                ],
                'return incorrect clinics',
            );
        });
    });
});
