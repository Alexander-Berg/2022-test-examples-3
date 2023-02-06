jest.disableAutomock();

import {SUBURBAN_TYPE} from '../../transportType';

import buildTransportUrl from '../transportUrl';

describe('transportUrl', () => {
    it('Должен вернуться null если нет transportType', () => {
        expect(buildTransportUrl({})).toBe(null);
    });

    it('Должен вернуться null если для переданного transportType нет страницы транспорта', () => {
        expect(buildTransportUrl({transportType: 'unknown'})).toBe(null);
    });

    it('Дожен вернуться ссылка для suburban', () => {
        expect(buildTransportUrl({transportType: SUBURBAN_TYPE})).toBe(
            `/${SUBURBAN_TYPE}`,
        );
    });
});
