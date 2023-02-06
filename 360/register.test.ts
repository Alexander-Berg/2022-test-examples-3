import { testSaga } from 'redux-saga-test-plan';

import { registerDomainSaga } from './register';
import {
    DomainStatus,
    startRegister,
    startPollStatus,
    getChosenEmail,
    setChosenEmail,
    setConnectedEmail,
    setDomainStatus,
    setShowRegisterErrorDialog
} from '../slices';
import {
    getIsVisible,
    setStatus,
    Status,
    setIsLoading as setOnboaringIsLoading,
    setBeautifulEmailStep,
    BeautifulEmailSteps
} from '../../onboarding/slices';
import { confirmPhone, isUserRecentlyConfirmedPhone } from './confirmPhone';
import { countMetrikaWithCurrentPage, countOnboardingMetrika } from '../../../../utils/metrika';
import { BEAUTIFUL_EMAIL_METRIKA_KEY } from '../consts';
import logSagaError from '../../../helpers/logSagaError';
import { getApi } from '../api/getApi';

jest.mock('@ps-int/beautiful-email-select/src/api', () => undefined);
jest.mock('@ps-int/beautiful-email-select/src/helpers/get-time-until-get-status', () => () => 42);
jest.mock('@ps-int/ufo-rocks/lib/metrika', () => undefined);

describe('BeautifulEmail/Sagas › registerDomainSaga', () => {
    it('should set registrationFailed if has no chosenEmail', () => {
        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next(undefined)
            .select(getIsVisible)
            .next(false)
            .put(setDomainStatus(DomainStatus.registrationFailed))
            .next()
            .put(setShowRegisterErrorDialog(true))
            .next()
            .call(countMetrikaWithCurrentPage, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registration error')
            .next()
            .spawn(logSagaError, 'beautifulEmail', 'registerSaga', Error('No email specified'))
            .next()
            .isDone();
    });

    it('should set registrationFailed if has no chosenEmail from onboarding', () => {
        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next(undefined)
            .select(getIsVisible)
            .next(true)
            .put(setDomainStatus(DomainStatus.registrationFailed))
            .next()
            .put(setOnboaringIsLoading(false))
            .next()
            .put(setStatus(Status.error))
            .next()
            .call(countOnboardingMetrika, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registration error')
            .next()
            .spawn(logSagaError, 'onboardingBeautifulEmail', 'registerSaga', Error('No email specified'))
            .next()
            .isDone();
    });

    it('should do nothing if afterBuy & user has no recently confirmed phone', () => {
        testSaga(registerDomainSaga, startRegister({ afterBuy: true }))
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next()
            .call(isUserRecentlyConfirmedPhone)
            .next(false)
            .isDone();
    });

    it('should do nothing if not afterBuy & user cancelled phone confirmation', () => {
        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next()
            .call(confirmPhone)
            .throw(new Error('phone confirm cancelled'))
            .isDone();
    });

    it('should call API & start poll status', () => {
        const apiFn = {
            registerDomain: jest.fn()
        };

        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next(false)
            .call(confirmPhone)
            .next(undefined)
            .put(setConnectedEmail('a@b.ru'))
            .next()
            .put(setChosenEmail(undefined))
            .next()
            .put(setDomainStatus(DomainStatus.connecting))
            .next()
            .select(getApi)
            .next(apiFn)
            .call([apiFn, apiFn.registerDomain], 'b.ru', 'a')
            .next({ result: 'success' })
            .put(startPollStatus())
            .next()
            .call(countMetrikaWithCurrentPage, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registered')
            .next()
            .isDone();
    });

    it('should call API & start poll status from onboarding', () => {
        const apiFn = {
            registerDomain: jest.fn()
        };

        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next(true)
            .call(confirmPhone)
            .next(undefined)
            .put(setConnectedEmail('a@b.ru'))
            .next()
            .put(setChosenEmail(undefined))
            .next()
            .put(setDomainStatus(DomainStatus.connecting))
            .next()
            .select(getApi)
            .next(apiFn)
            .call([apiFn, apiFn.registerDomain], 'b.ru', 'a')
            .next({ result: 'success' })
            .put(startPollStatus())
            .next()
            .put(setOnboaringIsLoading(false))
            .next()
            .put(setBeautifulEmailStep(BeautifulEmailSteps.success))
            .next()
            .put(setStatus(Status.activated))
            .next()
            .call(countOnboardingMetrika, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registered')
            .next()
            .isDone();
    });

    it('should set registrationFailed if registration failed', () => {
        const apiFn = {
            registerDomain: jest.fn()
        };

        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next(false)
            .call(confirmPhone)
            .next(undefined)
            .put(setConnectedEmail('a@b.ru'))
            .next()
            .put(setChosenEmail(undefined))
            .next()
            .put(setDomainStatus(DomainStatus.connecting))
            .next()
            .select(getApi)
            .next(apiFn)
            .call([apiFn, apiFn.registerDomain], 'b.ru', 'a')
            .next({ result: 'error' })
            .put(setDomainStatus(DomainStatus.registrationFailed))
            .next()
            .put(setConnectedEmail(undefined))
            .next()
            .put(setChosenEmail('a@b.ru'))
            .next()
            .put(setShowRegisterErrorDialog(true))
            .next()
            .call(countMetrikaWithCurrentPage, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registration error')
            .next()
            .spawn(logSagaError, 'beautifulEmail', 'registerSaga', Error('Domain registration failed'))
            .next()
            .isDone();
    });

    it('should set registrationFailed if registration failed from onboarding', () => {
        const apiFn = {
            registerDomain: jest.fn()
        };

        testSaga(registerDomainSaga, startRegister())
            .next()
            .select(getChosenEmail)
            .next('a@b.ru')
            .select(getIsVisible)
            .next(true)
            .call(confirmPhone)
            .next(undefined)
            .put(setConnectedEmail('a@b.ru'))
            .next()
            .put(setChosenEmail(undefined))
            .next()
            .put(setDomainStatus(DomainStatus.connecting))
            .next()
            .select(getApi)
            .next(apiFn)
            .call([apiFn, apiFn.registerDomain], 'b.ru', 'a')
            .next({ result: 'error' })
            .put(setDomainStatus(DomainStatus.registrationFailed))
            .next()
            .put(setConnectedEmail(undefined))
            .next()
            .put(setChosenEmail('a@b.ru'))
            .next()
            .put(setOnboaringIsLoading(false))
            .next()
            .put(setStatus(Status.error))
            .next()
            .call(countOnboardingMetrika, BEAUTIFUL_EMAIL_METRIKA_KEY, 'registration error')
            .next()
            .spawn(logSagaError, 'onboardingBeautifulEmail', 'registerSaga', Error('Domain registration failed'))
            .next()
            .isDone();
    });
});
