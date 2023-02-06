import { assert } from 'chai';
import type { ISnippetContext } from '@lib/Context';
import { AdapterUniSearchMedicineOffers } from '@features/UniSearch/UniSearch.features/UniSearchMedicine/UniSearchMedicine.features/Offers/UniSearchMedicineOffers.server';
import type { IPrivExternals, ISerpDocument } from '../../../../../typings';
import type { IBaseSnippet } from '../../UniSearchBase/UniSearchBase.typings';
import type {
    IAggregatorOffer,
    IUniSearchMedicineSnippetData,
    IUniSearchMedicineListSnippetItem,
} from '../UniSearchMedicine.typings';

import { AdapterUniSearchMedicineClinics as AdapterClinics } from '../UniSearchMedicine.features/Clinics/UniSearchMedicineClinics.server';
import { AdapterUniSearchMedicineBase } from '../UniSearchMedicine.server';
import type { IClinicProps, IItemSourceData } from '../UniSearchMedicine.typings';

type TSnippetData = IBaseSnippet<IUniSearchMedicineListSnippetItem> & IUniSearchMedicineSnippetData;

// Это надо будет как-то в хэлперы унести
interface IAdapterArguments {
    context: ISnippetContext;
    snippet: TSnippetData;
    document: ISerpDocument;
    privExternals: IPrivExternals;
}

class AdapterClinicsTest extends AdapterClinics {
    App = () => null
}

class AdapterUniSearchMedicineOffersTest extends AdapterUniSearchMedicineOffers {
    App = () => null
}

// Пока не нужен тест на платформу, просто здесь делаем конкретный класс
class AdapterTest extends AdapterUniSearchMedicineBase {
    App = () => null

    getMainProps() {
        return {
            specializations: 'specializations',
            features: [],
            description: 'description',
            photo: 'photo',
            name: 'name',
            sourceName: 'host',
            url: 'url',
            descriptionSource: { url: 'foo', text: 'bar' },
            featuresSource: { url: 'foo', text: 'bar' },
        };
    }

    getClinicsProps(clinics: IUniSearchMedicineListSnippetItem['clinics'], sources: IUniSearchMedicineListSnippetItem['sources']) {
        return new AdapterClinicsTest(this).getClinicProps(clinics || [], sources || []);
    }

    getOffersProps(offers?: IAggregatorOffer[]) {
        return new AdapterUniSearchMedicineOffersTest(this).getAggregatorOffers(offers);
    }

    getCommonSource(clinics: IClinicProps[], sources: IItemSourceData[]) {
        return new AdapterClinicsTest(this).getCommonSource(clinics || [], sources || []);
    }
}

const PRICES: Record<string, IAggregatorOffer['price']> = {
    default: {
        currency: 'RUR',
        product: 'за прием',
        value: 6000,
        text: '6000 ₽ за прием',
        url: '',
        name: '',
    },
};

describe('AdapterUniSearchMedicineBase', () => {
    const adapterArguments: IAdapterArguments = {
        context: {
            expFlags: {},
            device: {},
            query: {},
        } as unknown as ISnippetContext,
        document: {} as ISerpDocument,
        snippet: {} as TSnippetData,
        privExternals: {
            Counter: () => null,
            pushAssets: () => null,
        } as unknown as IPrivExternals,
    };

    describe('getAggregatorOffers', () => {
        it('should return offer props', () => {
            const adapter = new AdapterTest(adapterArguments);

            assert.deepEqual(adapter.getOffersProps([]), [], 'array is not eampty');
            assert.deepEqual(adapter.getOffersProps([{
                rating: {
                    cnt_reviews: 1,
                    average_rating: 3.9,
                },
                price: PRICES.default,
                source: {
                    url: 'https://foo.com',
                    name: 'Название_1',
                    favicon: 'https://foo.com/img',
                },
            }, {
                rating: {
                    cnt_reviews: 0,
                    average_rating: 3.9,
                    average_rating_raw: 4.5,
                },
                price: PRICES.default,
                source: {
                    url: 'https://foo.com',
                    name: 'Название_2',
                    favicon: 'https://foo.com/img',
                },
            }, {
                price: PRICES.default,
                source: {
                    url: 'https://foo.com',
                    name: 'Название_3',
                    favicon: 'https://foo.com/img',
                },
            }] as IAggregatorOffer[]), [{
                price: PRICES.default,
                rating: '3,9',
                reviewCount: '1 отзыв',
                name: 'Название_1',
                favicon: 'https://foo.com/img',
                url: 'https://foo.com',
            }, {
                price: PRICES.default,
                rating: undefined,
                reviewCount: 'нет отзывов',
                name: 'Название_2',
                favicon: 'https://foo.com/img',
                url: 'https://foo.com',
            }, {
                price: PRICES.default,
                rating: undefined,
                reviewCount: 'нет отзывов',
                name: 'Название_3',
                favicon: 'https://foo.com/img',
                url: 'https://foo.com',
            }], 'return incorrect items');
        });
    });
});
