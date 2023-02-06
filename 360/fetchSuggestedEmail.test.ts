import { testSaga, expectSaga } from 'redux-saga-test-plan';

import { fetchSuggestedEmailSaga } from './fetchSuggestedEmail';
import { setSuggestedEmail, getSuggestedEmail, getChosenEmail, getConnectedEmail } from '../slices';
import { getApi } from '../api/getApi';
import { select } from 'redux-saga/effects';

jest.mock('@ps-int/beautiful-email-select/src/api', () => undefined);
jest.mock('@ps-int/beautiful-email-select/src/helpers/get-time-until-get-status', () => undefined);
jest.mock('@ps-int/ufo-rocks/lib/metrika', () => undefined);

describe('BeautifulEmail/Sagas › fetchSuggestedEmail', () => {
    it('should do nothing if already has connectedEmail', () => {
        testSaga(fetchSuggestedEmailSaga)
            .next()
            .select(getConnectedEmail)
            .next('a@b.ru')
            .select(getChosenEmail)
            .next(undefined)
            .select(getSuggestedEmail)
            .next(undefined)
            .isDone();
    });

    it('should do nothing if already has chosenEmail', () => {
        testSaga(fetchSuggestedEmailSaga)
            .next()
            .select(getConnectedEmail)
            .next(undefined)
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getSuggestedEmail)
            .next(undefined)
            .isDone();
    });

    it('should do nothing if already has suggestedEmail', () => {
        testSaga(fetchSuggestedEmailSaga)
            .next()
            .select(getConnectedEmail)
            .next(undefined)
            .select(getChosenEmail)
            .next(undefined)
            .select(getSuggestedEmail)
            .next('a@b.ru')
            .isDone();
    });

    it('should call API and set top result', () => {
        const apiMock = {
            getSuggestList: jest.fn(() => ({
                suggested_domains: [{
                    login: 'aaa',
                    name: 'bbb.ru'
                }]
            }))
        };

        return expectSaga(fetchSuggestedEmailSaga)
            .provide([
                [select(getConnectedEmail), undefined],
                [select(getChosenEmail), undefined],
                [select(getSuggestedEmail), undefined],
                [select(getApi), apiMock]
            ])
            .call([apiMock, apiMock.getSuggestList], undefined, 1)
            .put(setSuggestedEmail('aaa@bbb.ru'))
            .silentRun();
    });
});
