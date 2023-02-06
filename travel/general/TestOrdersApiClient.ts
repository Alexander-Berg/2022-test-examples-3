import {
    IGetPaymentTestContextTokenParams,
    IGetPaymentTestContextTokenResponse,
} from 'helpers/project/common/api/types/PaymentTestContext';

import {TestApiClient} from 'helpers/utilities/api/TestApiClient';

const DEFAULT_TIMEOUT = 5000;

export class TestOrdersApiClient extends TestApiClient {
    async getPaymentTestContextToken(
        params: IGetPaymentTestContextTokenParams,
    ): Promise<IGetPaymentTestContextTokenResponse> {
        try {
            const response = await this.apiClient.get(
                '/api/orders/paymentTestContextToken',
                {
                    params,
                    timeout: DEFAULT_TIMEOUT,
                },
            );

            return response.data;
        } catch (e) {
            console.log(e);

            throw new Error(
                `Не удалось получить paymentToken. Параметры: ${JSON.stringify(
                    params,
                )}`,
            );
        }
    }
}
