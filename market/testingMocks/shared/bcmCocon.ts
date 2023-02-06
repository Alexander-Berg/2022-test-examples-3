import {MockBackend} from '../mockBackend';
import {MockClient} from '../mockClient';

class CoconBackend extends MockBackend.setup('cocon', {}) {}

export class CoconClient extends MockClient.connect({backend: CoconBackend}) {
    // @ts-expect-error(TS7031) найдено в рамках MARKETPARTNER-16237
    static selectResult({result}) {
        return result;
    }

    // @ts-expect-error(TS7031) найдено в рамках MARKETPARTNER-16237
    async page({cabinetName, ...params}) {
        const response = await this.backend.fetch({
            logName: 'page',
            method: 'GET',
            pathname: `/cabinet/${cabinetName}/page`,
            query: params,
        });

        return CoconClient.selectResult(response);
    }
}
