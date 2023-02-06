import { GotInstance, GotJSONFn } from '@yandex-int/si.ci.requests';
import debugFactory from 'debug';

import BookingApiService, { DevicesDownloader } from '../../src/lib/api/booking';
import SandboxerMock from './sandboxer';

export default function createBookingApiServiceMock(mock: Function) {
    const debug = debugFactory('test');

    const requests = mock().mockResolvedValue({ body: [] }) as unknown as GotInstance<GotJSONFn>;

    const sandbox = new SandboxerMock({ token: 'fake-token' }, mock());
    const devicesDownloader = new DevicesDownloader({ debug, sandbox, requests });

    return new BookingApiService({ debug, devicesDownloader, requests });
}
