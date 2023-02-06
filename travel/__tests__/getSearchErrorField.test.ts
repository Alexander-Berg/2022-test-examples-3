import ISearchFormError from '../../../interfaces/state/searchForm/ISearchFormError';
import SearchFormErrorField from '../../../../common/interfaces/state/searchForm/SearchFormErrorField';
import SearchFormErrorDateType from '../../../../common/interfaces/state/searchForm/SearchFormErrorDateType';

import getSearchErrorField from '../getSearchErrorField';

const error: ISearchFormError = {
    fields: [
        SearchFormErrorField.FROM,
        SearchFormErrorField.TO,
        SearchFormErrorField.WHEN,
    ],
    type: SearchFormErrorDateType.INCORRECT,
};

describe('getSearchErrorField', () => {
    it('должен вернуть when', () => {
        expect(getSearchErrorField(error)).toEqual(SearchFormErrorField.WHEN);
    });
});
