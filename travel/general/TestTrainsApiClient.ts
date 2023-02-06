import {
    ITrainsTestContextTokenAnswer,
    ITrainsTestContextTokenParams,
} from './types/ITrainsTestContextToken';

import {TestOrdersApiClient} from 'helpers/project/common/api/TestOrdersApiClient';

const DEFAULT_TIMEOUT = 5000;

export class TestTrainsApiClient extends TestOrdersApiClient {
    // Ограничения для таймаутов - стоит ставить не меньше 10 секунд, чтобы не флапало (бывают лаги на беке до 6 с)
    static minTimeoutInSeconds: number = 10;

    async getTestContextToken(
        params: ITrainsTestContextTokenParams,
    ): Promise<ITrainsTestContextTokenAnswer> {
        try {
            const response = await this.apiClient.get(
                '/api/trains/testContextToken',
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
