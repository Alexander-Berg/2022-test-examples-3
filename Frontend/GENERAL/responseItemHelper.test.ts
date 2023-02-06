import { IRequestData } from '../../typings/apphost';

import { addReqId } from './responseItemHelper';

jest.mock('./httpProtobuf', () => ({
    decode: () => {},
    toObject: () => ({ Content: '' }),
}));

describe('responseItemHelper', () => {
    describe('addReqId', () => {
        let requestData: IRequestData;
        let rawResponse: string;

        beforeEach(() => {
            requestData = {
                headers: [
                    ['x-yandex-req-id', 'example'],
                ],
            } as unknown as IRequestData;

            rawResponse = '{"example":"value"}';
        });

        it('should add reqId field to JSON', () => {
            expect(addReqId(requestData, rawResponse)).toBe('{"example":"value","reqId":"example"}');
        });

        it('should add reqId field to empty JSON', () => {
            expect(addReqId(requestData, '{}')).toBe('{"reqId":"example"}');
        });

        it('should preserve rawResponse if reqId is not defined', () => {
            const withoutReqIdValue = {
                ...requestData,
                headers: [
                    ['x-yandex-req-id', ''],
                ],
            } as unknown as IRequestData;

            const preservedResponse = '{"example":"value"}';

            expect(addReqId(withoutReqIdValue, rawResponse)).toBe(preservedResponse);

            const withoutReqIdHeader = {
                ...requestData,
                headers: [],
            } as unknown as IRequestData;

            expect(addReqId(withoutReqIdHeader, rawResponse)).toBe(preservedResponse);
        });
    });
});
