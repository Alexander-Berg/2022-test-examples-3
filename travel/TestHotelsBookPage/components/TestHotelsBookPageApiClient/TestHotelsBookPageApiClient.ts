/* Types */
import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';
import {ITestBookOfferTokenResponse} from 'helpers/project/hotels/pages/TestHotelsBookPage/components/TestHotelsBookPageApiClient/types';

import {TestOrdersApiClient} from 'helpers/project/common/api/TestOrdersApiClient';

/* Constants */
const FETCH_OFFER_DEFAULT_TIMEOUT = 10000;
const FETCH_OFFER_HEADERS = {
    'Content-Type': 'application/json',
    Cookie: 'yandexuid=1',
};

export class TestHotelsBookPageApiClient extends TestOrdersApiClient {
    async getTestContextToken(
        params: IBookOfferRequestParams,
    ): Promise<ITestBookOfferTokenResponse> {
        const response = await this.apiClient.get(
            '/api/hotels/getTestBookOfferToken',
            {
                params,
                timeout: FETCH_OFFER_DEFAULT_TIMEOUT,
                headers: FETCH_OFFER_HEADERS,
            },
        );

        return response.data;
    }
}
