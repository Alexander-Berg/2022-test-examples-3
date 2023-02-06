import {
    IAviaTestContextParams,
    IAviaTestContextAnswer,
} from './types/AviaTestContext';

import {TestApiClient} from 'helpers/utilities/api/TestApiClient';

const DEFAULT_TIMEOUT = 5000;

export class TestAviaApiClient extends TestApiClient {
    async getTestContextToken(
        params: IAviaTestContextParams,
    ): Promise<IAviaTestContextAnswer> {
        try {
            const response = await this.apiClient.get(
                '/api/avia/booking/testContext',
                {
                    params,
                    timeout: DEFAULT_TIMEOUT,
                },
            );

            return response.data;
        } catch (e) {
            throw new Error(
                `Не удалось получить testToken. Параметры: ${JSON.stringify(
                    params,
                )}`,
            );
        }
    }
}
