import { testSaga } from 'redux-saga-test-plan';

import { pollDomainStatusSaga } from './pollStatus';
import { DomainStatus, getDomainStatus, startPollStatus, setDomainStatus } from '../slices';
import { updateDomainStatus } from './updateDomainStatus';
import logSagaError from '../../../helpers/logSagaError';

jest.mock('@ps-int/beautiful-email-select/src/api', () => undefined);
jest.mock('@ps-int/beautiful-email-select/src/helpers/get-time-until-get-status', () => () => 42);
jest.mock('@ps-int/ufo-rocks/lib/metrika', () => undefined);

describe('BeautifulEmail/Sagas › pollDomainStatusSaga', () => {
    it('should do nothing if email is already connected', () => {
        testSaga(pollDomainStatusSaga, startPollStatus())
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connected)
            .isDone();
    });

    it('should call updateDomainStatus', () => {
        testSaga(pollDomainStatusSaga, startPollStatus({ immediately: true }))
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connected)
            .isDone();
    });

    it('should call updateDomainStatus with delay if no `immediately` flag', () => {
        testSaga(pollDomainStatusSaga, startPollStatus())
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .delay(42)
            .next()
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connected)
            .isDone();
    });

    it('should call updateDomainStatus until get finish status', () => {
        testSaga(pollDomainStatusSaga, startPollStatus({ immediately: true }))
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .delay(42)
            .next()
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .delay(42)
            .next()
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .delay(42)
            .next()
            .call(updateDomainStatus)
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connected)
            .isDone();
    });

    it('should set `infoUnavailable` and log error if error occurred', () => {
        const error = new Error();

        testSaga(pollDomainStatusSaga, startPollStatus())
            .next()
            .select(getDomainStatus)
            .next(DomainStatus.connecting)
            .delay(42)
            .next()
            .call(updateDomainStatus)
            .throw(error)
            .put(setDomainStatus(DomainStatus.infoUnavailable))
            .next()
            .spawn(logSagaError, 'beautifulEmail', 'pollStatusSaga', error)
            .next()
            .isDone();
    });
});
