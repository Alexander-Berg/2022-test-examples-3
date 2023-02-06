import { expectSaga, testSaga } from 'redux-saga-test-plan';

import { updateDomainStatus } from './updateDomainStatus';
import {
    DomainStatus,
    getConnectedEmail,
    getDomainStatus,
    setConnectedEmail,
    setDomainStatus,
    setTimeoutDate
} from '../slices';
import { select, call } from 'redux-saga/effects';
import { getApi } from '../api/getApi';

jest.mock('@ps-int/beautiful-email-select/src/api', () => undefined);
jest.mock('@ps-int/beautiful-email-select/src/helpers/get-time-until-get-status', () => () => 42);
jest.mock('@ps-int/ufo-rocks/lib/metrika', () => undefined);

describe('BeautifulEmail/Sagas › updateDomainStatus', () => {
    it('should call getDomainStatus API w/o args', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [select(getConnectedEmail), 'a@b.ru'],
                [select(getDomainStatus), DomainStatus.shouldFetch],
            ])
            .call([apiMock, apiMock.getDomainStatus])
            .silentRun();
    });

    it('should set infoUnavailable if got unknown status', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), { status: '123', domain: 'a.ru' }],
                [select(getConnectedEmail), 'a@b.ru'],
                [select(getDomainStatus), DomainStatus.shouldFetch],
            ])
            .put(setDomainStatus(DomainStatus.infoUnavailable))
            .silentRun();
    });

    it('should set infoUnavailable if API throws', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        testSaga(updateDomainStatus)
            .next()
            .select(getApi)
            .next(apiMock)
            .call([apiMock, apiMock.getDomainStatus])
            .throw(new Error('111'))
            .select(getConnectedEmail)
            .next(undefined)
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .put(setDomainStatus(DomainStatus.infoUnavailable))
            .next()
            .isDone();
    });

    it('should set readyToRegister if got NOT_FOUND', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        testSaga(updateDomainStatus)
            .next()
            .select(getApi)
            .next(apiMock)
            .call([apiMock, apiMock.getDomainStatus])
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            .throw({ code: 'NOT_FOUND' })
            .select(getConnectedEmail)
            .next(undefined)
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .put(setDomainStatus(DomainStatus.readyToRegister))
            .next()
            .isDone();
    });

    it('should set new status', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), {
                    status: 'registered',
                    domain: 'b.ru'
                }],
                [select(getConnectedEmail), 'a@b.ru'],
                [select(getDomainStatus), DomainStatus.connecting],
            ])
            .put(setDomainStatus(DomainStatus.connected))
            .silentRun();
    });

    it('should not set status if it was not changed', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), {
                    status: 'pending_registrar',
                    domain: 'b.ru'
                }],
                [select(getConnectedEmail), 'a@b.ru'],
                [select(getDomainStatus), DomainStatus.connecting],
            ])
            .not.put(setDomainStatus(DomainStatus.connected))
            .silentRun();
    });

    it('should set email if it is absent in store', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), {
                    status: 'wait_dns_entries',
                    domain: 'pupkin.ru',
                    login: 'vasya'
                }],
                [select(getConnectedEmail), undefined],
                [select(getDomainStatus), DomainStatus.shouldFetch],
            ])
            .put(setConnectedEmail('vasya@pupkin.ru'))
            .silentRun();
    });

    it('should set infoUnavailable if email in store and domain from API are different', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), {
                    status: 'wait_dns_entries',
                    domain: 'pupkin.ru',
                    login: 'vasya'
                }],
                [select(getConnectedEmail), 'pupkin@vasya.ru'],
                [select(getDomainStatus), DomainStatus.shouldFetch],
            ])
            .put(setDomainStatus(DomainStatus.infoUnavailable))
            .silentRun();
    });

    it('should not set infoUnavailable if email in store and domain from API differs only in case', () => {
        const apiMock = {
            getDomainStatus: jest.fn()
        };

        return expectSaga(updateDomainStatus)
            .provide([
                [select(getApi), apiMock],
                [call([apiMock, apiMock.getDomainStatus]), {
                    status: 'wait_dns_entries',
                    domain: 'PuPkIn.ru',
                    login: 'VaSyA'
                }],
                [select(getConnectedEmail), 'vasya@pupkin.ru'],
                [select(getDomainStatus), DomainStatus.shouldFetch],
            ])
            .not.put(setDomainStatus(DomainStatus.infoUnavailable))
            .put(setDomainStatus(DomainStatus.connecting))
            .silentRun();
    });

    const testTimeout = (status: 'cancelled_by_user' | 'expired') => {
        it(`should set "timeout" status + timeoutDate if got '${status}' and 'register_allowed: false'`, () => {
            const apiMock = {
                getDomainStatus: jest.fn()
            };
            const registerAllowedTS = '2021-11-30T13:28:28.726369';

            return expectSaga(updateDomainStatus)
                .provide([
                    [select(getApi), apiMock],
                    [call([apiMock, apiMock.getDomainStatus]), {
                        status,
                        domain: 'pupkin.ru',
                        login: 'vasya',
                        register_allowed: false,
                        register_allowed_ts: registerAllowedTS,
                    }],
                    [select(getConnectedEmail), undefined],
                    [select(getDomainStatus), DomainStatus.shouldFetch],
                ])
                .put(setDomainStatus(DomainStatus.timeout))
                .put(setTimeoutDate(new Date(registerAllowedTS)))
                .silentRun();
        });
    };

    testTimeout('cancelled_by_user');
    testTimeout('expired');

    const testTimeoutEnded = (status: 'cancelled_by_user' | 'expired') => {
        it(`should set "readyToRegister" status if got '${status}' and 'register_allowed: true'`, () => {
            const apiMock = {
                getDomainStatus: jest.fn()
            };

            return expectSaga(updateDomainStatus)
                .provide([
                    [select(getApi), apiMock],
                    [call([apiMock, apiMock.getDomainStatus]), {
                        status,
                        domain: 'pupkin.ru',
                        login: 'vasya',
                        register_allowed: true,
                    }],
                    [select(getConnectedEmail), undefined],
                    [select(getDomainStatus), DomainStatus.shouldFetch],
                ])
                .put(setDomainStatus(DomainStatus.readyToRegister))
                .silentRun();
        });
    };

    testTimeoutEnded('cancelled_by_user');
    testTimeoutEnded('expired');
});
