/* eslint-disable @typescript-eslint/no-magic-numbers */
import { renderHook } from '@testing-library/react-hooks';

import { HttpStatusCode } from 'shared/consts/HttpStatusCode';
import { FetchError } from 'shared/helpers/fetchRequest/fetchRequest';
import { useRetry } from 'shared/hooks/useRetry/useRetry';

const ERROR_500 = {
    __FetchError__: {
        message: '500ka',
        payload: {
            status: HttpStatusCode.INTERNAL_SERVER_ERROR,
            statusText: '500ka',
            req: {
                url: 'https://nice.url',
                query: '?with=query',
            },
            res: {
                error_details: {
                    http_code: 500,
                    special_info: {
                        error_code: 'incorrect_request',
                        user_id: '6b356a7b-0ab7-4d06-a908-6864482c2826',
                        session_info: {
                            source_location: ['drive/backend/processors/hardware/processor.cpp:23'],
                            'TBeaconStateProcessor::ProcessServiceRequest': ['assertion object failed'],
                        },
                    },
                    debug_message: 'cannot find objectundefined',
                },
            },
            time: 100,
            canceled: false,
        },
    },
};

describe('useRetry', () => {
    it('should retry only 3 times', () => {
        let error: Optional<FetchError | Error> = FetchError.factory(ERROR_500);
        let reloadCount = 0;
        const reload = () => {
            error = FetchError.factory(ERROR_500);
            reloadCount++;
        };

        const { rerender } = renderHook(() => useRetry(error, reload));

        rerender({ error, reload });
        rerender({ error, reload });
        rerender({ error, reload });
        rerender({ error, reload });

        expect(reloadCount).toEqual(3);
    });

    it('should retry only 1 time', () => {
        let error: Optional<FetchError | Error> = FetchError.factory(ERROR_500);
        let reloadCount = 0;
        const reload = () => {
            reloadCount++;
            error = undefined;
        };

        const { rerender } = renderHook(() => useRetry(error, reload));

        rerender();
        rerender();
        rerender();
        rerender();

        expect(reloadCount).toEqual(1);
    });
});
