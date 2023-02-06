import {FilterTransportType} from '../../transportType';
import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';
import IWhen from '../../../interfaces/state/IWhen';
import ISearchFiltering from '../../../interfaces/state/search/ISearchFiltering';

import isLastochkaPage from '../isLastochkaPage';

const when = {special: DateSpecialValue.today} as IWhen;
const filtering = {filters: {lastochka: {value: true}}} as ISearchFiltering;

describe('isLastochkaPage', () => {
    it('Вернёт true когда все параметры соответствуют ожидаемым', () => {
        expect(
            isLastochkaPage(FilterTransportType.suburban, when, filtering),
        ).toBe(true);
    });

    it('Вернёт false, когда какой-либо из параметров не соответствует ожидаемым', () => {
        expect(
            isLastochkaPage(FilterTransportType.plane, when, filtering),
        ).toBe(false);

        expect(
            isLastochkaPage(
                FilterTransportType.suburban,
                {special: DateSpecialValue.allDays} as IWhen,
                filtering,
            ),
        ).toBe(false);

        expect(
            isLastochkaPage(FilterTransportType.suburban, when, {
                filters: {lastochka: {value: false}},
            } as ISearchFiltering),
        ).toBe(false);
    });
});
