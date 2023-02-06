jest.disableAutomock();

import buildTransportCityUrl from '../transportCityUrl';

const data = {
    transportType: 'suburban',
    slug: 'moscow',
};
const path = `/${data.transportType}/${data.slug}`;

describe('transportCityUrl', () => {
    it('Должен вернуться null если нет transportType', () => {
        expect(
            buildTransportCityUrl({
                ...data,
                transportType: null,
            }),
        ).toBe(null);
    });

    it('Должен вернуться null если нет slug', () => {
        expect(
            buildTransportCityUrl({
                ...data,
                slug: null,
            }),
        ).toBe(null);
    });

    it('Должен вернуться null если для переданного transportType нет страницы города+транспорта', () => {
        expect(
            buildTransportCityUrl({
                ...data,
                transportType: 'unknown',
            }),
        ).toBe(null);
    });

    it('Должна вернуться ссылка', () => {
        expect(buildTransportCityUrl(data)).toBe(path);
    });
});
