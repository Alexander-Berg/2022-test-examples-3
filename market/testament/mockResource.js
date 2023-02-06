// @flow

/* eslint-disable global-require */

/**
 * Способ замокать bcm-ресурс своими данными
 */
export default function mockResource(resourcePath: string, moduleName: string, createResponse: (params: mixed) => mixed) {
    const {[moduleName]: actualFn} = jest.requireActual(resourcePath);
    // flowlint-next-line unclear-type: off
    const spy = jest.spyOn((require: any)(resourcePath), moduleName)
        .mockImplementation(async (ctx, params) => {
            try {
                return actualFn.schema.process(createResponse(params), {}, params);
            } catch (error) {
                // eslint-disable-next-line no-console
                console.log('An error occurred during processing mocked resource response!');
                console.error(error);
                throw error;
            }
        });

    return () => {
        spy.mockRestore();
    };
}
