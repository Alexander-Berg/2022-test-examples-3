import { expectSaga, testSaga } from 'redux-saga-test-plan';

import { confirmPhone, isUserRecentlyConfirmedPhone } from './confirmPhone';
import { call, getContext, select } from 'redux-saga/effects';
import { CloseReason, PassportChildWindow } from '../../../helpers/PassportChildWindow';
import logSagaError from '../../../helpers/logSagaError';
import { countMetrikaWithCurrentPage, countOnboardingMetrika } from '../../../../utils/metrika';
import { BEAUTIFUL_EMAIL_METRIKA_KEY } from '../consts';

jest.mock('@ps-int/ufo-rocks/lib/metrika', () => undefined);

describe('BeautifulEmail/Sagas', () => {
    describe('isUserRecentlyConfirmedPhone', () => {
        it('should call blackbox API', () => {
            const apiFn = jest.fn(() => Promise.resolve({ check: true }));

            testSaga(isUserRecentlyConfirmedPhone)
                .next()
                .getContext('api')
                .next(apiFn)
                .call(apiFn, 'blackbox/is-user-recently-confirmed-phone')
                .next()
                .isDone();
        });

        it('should return value from API (true value)', () => {
            const apiFn = jest.fn(() => Promise.resolve({ check: true }));

            return expectSaga(isUserRecentlyConfirmedPhone)
                .provide([
                    [getContext('api'), apiFn],
                    [call(apiFn), null]
                ])
                .returns(true)
                .silentRun();
        });

        it('should return value from API (false value)', () => {
            const apiFn = jest.fn(() => Promise.resolve({ check: false }));

            return expectSaga(isUserRecentlyConfirmedPhone)
                .provide([
                    [getContext('api'), apiFn],
                    [call(apiFn), null]
                ])
                .returns(false)
                .silentRun();
        });

        it('should return false if API throws', () => {
            const apiFn = jest.fn(() => Promise.reject('error'));

            return expectSaga(isUserRecentlyConfirmedPhone)
                .provide([
                    [getContext('api'), apiFn],
                    [call(apiFn), null]
                ])
                .returns(false)
                .silentRun();
        });
    });

    describe('confirmPhone', () => {
        it('should show passport window', () => {
            const passportChildWindow = { show: jest.fn(() => CloseReason.SUCCESS) };

            const apiFn = jest.fn(() => Promise.resolve({ check: true }));

            return expectSaga(confirmPhone)
                .provide([
                    [
                        select(),
                        {
                            yandexServices: { passport: 'https://passport.ya.ru' },
                            authData: { uid: '123' },
                            onboarding: { isVisible: false }
                        }
                    ],
                    [
                        call([PassportChildWindow, PassportChildWindow.create], {
                            url: 'https://passport.ya.ru/phoneconfirm'
                        }),
                        passportChildWindow
                    ],
                    [getContext('api'), apiFn],
                    [call(apiFn), null]
                ])
                .call([passportChildWindow, passportChildWindow.show], '123')
                .silentRun();
        });

        const testFailedConfirmation = (reason: CloseReason, isVisible: boolean) => {
            const passportChildWindow = { show: jest.fn(() => reason) };

            expect(() => {
                testSaga(confirmPhone)
                    .next()
                    .select()
                    .next({
                        yandexServices: { passport: 'https://passport.ya.ru' },
                        authData: { uid: '123' },
                        onboarding: { isVisible }
                    })
                    .call([PassportChildWindow, PassportChildWindow.create], {
                        url: 'https://passport.ya.ru/phoneconfirm'
                    })
                    .next(passportChildWindow)
                    .call([passportChildWindow, passportChildWindow.show], '123')
                    .next(reason)
                    .call(
                        isVisible ? countOnboardingMetrika : countMetrikaWithCurrentPage,
                        BEAUTIFUL_EMAIL_METRIKA_KEY,
                        reason === CloseReason.BLOCKED ? 'phone confirmation blocked' : 'phone confirmation canceled'
                    )
                    .next()
                    .spawn(logSagaError, 'beautifulEmail', 'confirmPhone', Error('phone confirm cancelled'))
                    .next();
            }).toThrowError(Error('phone confirm cancelled'));
        };

        it('should throw if phone confirmation was cancelled', () => {
            testFailedConfirmation(CloseReason.CANCEL, false);
        });

        it('should throw if phone confirmation was blocked', () => {
            testFailedConfirmation(CloseReason.BLOCKED, false);
        });

        it('should throw if phone confirmation was cancelled from onboarding', () => {
            testFailedConfirmation(CloseReason.CANCEL, true);
        });

        it('should throw if phone confirmation was blocked  from onboarding', () => {
            testFailedConfirmation(CloseReason.BLOCKED, true);
        });
    });
});
