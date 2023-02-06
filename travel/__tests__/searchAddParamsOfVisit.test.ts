import IState from '../../../interfaces/state/IState';
import {FilterTransportType} from '../../../lib/transportType';

import searchAddParamsOfVisit from '../searchAddParamsOfVisit';
import {params} from '../../../lib/yaMetrika';

jest.mock('../../../lib/yaMetrika', () => ({
    ...require.requireActual('../../../lib/yaMetrika'),
    params: jest.fn(),
}));

describe('searchAddParamsOfVisit', () => {
    it('Вызовет метод метрики params с параметрами поиска', () => {
        const result = searchAddParamsOfVisit({
            search: {
                context: {
                    from: {title: '', key: '', slug: 'from'},
                    to: {title: '', key: '', slug: 'to'},
                    searchNext: false,
                    when: {
                        text: 'завтра',
                    },
                    transportType: FilterTransportType.all,
                },
            },
        } as unknown as IState);

        expect(result).toBeUndefined();
        expect(params).toBeCalledWith(
            expect.objectContaining({
                fromSlug: 'from',
                toSlug: 'to',
                when: 'завтра',
                transportType: FilterTransportType.all,
            }),
        );
    });
});
