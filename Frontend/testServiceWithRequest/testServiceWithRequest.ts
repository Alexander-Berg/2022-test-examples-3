import {
    testServiceWithRequestBeforeEach,
    TestServiceWithRequestCallback,
    TestServiceWithRequestCallbackOptions,
} from 'services/spec/testServiceWithRequestBeforeEach/testServiceWithRequestBeforeEach';

export function testServiceWithRequest(title: string, callback: TestServiceWithRequestCallback) {
    let options: TestServiceWithRequestCallbackOptions;

    describe('test service with request', function() {
        testServiceWithRequestBeforeEach(async(obj) => {
            options = obj;
        });

        it(title, async function() {
            await callback(options);
        });
    });
}
