import {getCompanyFlights} from 'selectors/avia/utils/getCompanyFlights';
import {
    BASE_GROUP_VARIANT,
    MIN_PRICE_GROUP_VARIANT,
    COMPANY_PRICE_GROUP_VARIANT,
} from 'selectors/avia/utils/__mocks__/mocks';
import {
    IAviaVariantGroup,
    EAviaVariantGroupType,
} from 'selectors/avia/utils/denormalization/variantGroup';

const VARIANT_FROM_COMPANY: IAviaVariantGroup = {
    ...COMPANY_PRICE_GROUP_VARIANT,
    type: EAviaVariantGroupType.aviacompany,
};

describe('getCompanyFlights', () => {
    it('нет данных - вернёт пустой массив', () => {
        expect(getCompanyFlights([])).toEqual([]);
    });

    it('среди вариантов нет предложений от авиакомпаний - вернёт пустой массив', () => {
        expect(
            getCompanyFlights([BASE_GROUP_VARIANT, MIN_PRICE_GROUP_VARIANT]),
        ).toEqual([]);
    });

    it('среди предложений есть хотя бы одно от авиакомпании - вернёт массив с этими вариантами', () => {
        expect(
            getCompanyFlights([
                BASE_GROUP_VARIANT,
                COMPANY_PRICE_GROUP_VARIANT,
                MIN_PRICE_GROUP_VARIANT,
                COMPANY_PRICE_GROUP_VARIANT,
            ]),
        ).toEqual([VARIANT_FROM_COMPANY, VARIANT_FROM_COMPANY]);
    });
});
