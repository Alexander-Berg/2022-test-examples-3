import {FilterTransportType} from '../../transportType';
import IStateSearch from '../../../interfaces/state/search/IStateSearch';

import shouldRequestSuburbanTransfer from '../shouldRequestSuburbanTransfer';
import getTransportTypeForRequestTransfer from '../getTransportTypeForRequestTransfer';

jest.mock('../shouldRequestSuburbanTransfer');

const context = {
    when: {
        date: '22.12.2017',
    },
    transportType: FilterTransportType.all,
};
const searchResult = {
    context,
    segments: Array.from({length: 5}).map(() => ({isTransfer: false})),
} as unknown as IStateSearch;

describe('shouldAddTransfer', () => {
    it('при поиске на все дни - не запрашиваем пересадки', () => {
        expect(
            getTransportTypeForRequestTransfer({
                ...searchResult,
                context: {
                    ...context,
                    when: {
                        date: undefined,
                    },
                },
            } as unknown as IStateSearch),
        ).toBeUndefined();
    });

    it('если сегментов >= 3х и запрос пересадок электричками запрещен - не запрашиваем пересадки', () => {
        (shouldRequestSuburbanTransfer as jest.Mock).mockReturnValueOnce(false);

        expect(
            getTransportTypeForRequestTransfer(searchResult),
        ).toBeUndefined();
    });

    it('если сегментов >= 3х и запрос пересадок электричками разрешен - запрашиваем пересадки электричками', () => {
        (shouldRequestSuburbanTransfer as jest.Mock).mockReturnValueOnce(true);

        expect(getTransportTypeForRequestTransfer(searchResult)).toBe(
            FilterTransportType.suburban,
        );
    });

    it('если сегментов < 3 - запрашиваем пересадки тем типом транспорта, которым искали', () => {
        (shouldRequestSuburbanTransfer as jest.Mock).mockReturnValueOnce(false);

        expect(
            getTransportTypeForRequestTransfer({
                segments: Array.from({length: 2}).map(() => ({
                    isTransfer: false,
                })),
                context: {
                    ...context,
                    transportType: FilterTransportType.plane,
                },
            } as unknown as IStateSearch),
        ).toBe(FilterTransportType.plane);
    });

    it('При подсчете кол-ва сегментов не должны учиываться пересадки', () => {
        (shouldRequestSuburbanTransfer as jest.Mock).mockReturnValueOnce(false);

        expect(
            getTransportTypeForRequestTransfer({
                segments: Array.from({length: 2})
                    .map(() => ({isTransfer: false}))
                    .concat(
                        Array.from({length: 2}).map(() => ({isTransfer: true})),
                    ),
                context: {
                    ...context,
                    transportType: FilterTransportType.plane,
                },
            } as unknown as IStateSearch),
        ).toBe(FilterTransportType.plane);
    });
});
