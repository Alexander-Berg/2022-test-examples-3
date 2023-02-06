// eslint-disable-next-line import/order
import Asker from 'asker';

import type {Config} from './worker';
import mockedRequest, {MockedTransportRequest} from './mockedTransport';

export default function askerAdapter(
    this: Asker,
    request: MockedTransportRequest,
): void {
    mockedRequest(request).then(response =>
        this.done(null, {
            ...response,
            meta: this.getResponseMetaBase(),
        }),
    );
}

export function mockAsker(moduleName: string, initConfig: Config): void {
    const originalAsker = jest.requireActual(moduleName);
    const originalTryHTTP = originalAsker.prototype._tryHttpRequest;
    jest.spyOn(originalAsker.prototype, '_tryHttpRequest').mockImplementation(
        // @ts-ignore
        function (options): Promise<unknown> {
            if (
                // @ts-ignore
                options.hostname.includes(initConfig.host) ||
                // @ts-ignore
                options.host.includes(initConfig.host)
            ) {
                // @ts-ignore
                return askerAdapter.call(this, options);
            }

            // @ts-ignore
            return originalTryHTTP.call(this, options);
        },
    );
}
