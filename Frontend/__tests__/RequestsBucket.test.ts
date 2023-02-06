import { RequestTimeoutError } from '@yandex-int/messenger.errors';
import { RequestsBucket } from '../index';

describe('#RequestsBucket', () => {
    it('should await for response', async () => {
        const buckets = new RequestsBucket();
        const reqId = 1;
        const expectedResult = 'test';

        setTimeout(() => {
            buckets.result(reqId, undefined, expectedResult);
        }, 10);

        const actualResult = await buckets.request(() => {
            return reqId;
        });

        expect(actualResult).toEqual(expectedResult);
    });

    it('should throw timeout error', async () => {
        const buckets = new RequestsBucket();
        const reqId = 1;

        expect.assertions(1);
        buckets.request(() => {
            return reqId;
        }, 0).catch((error) => expect(error).toBeInstanceOf(RequestTimeoutError));
    });
});
