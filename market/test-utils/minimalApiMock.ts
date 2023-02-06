import {VendorApi} from 'core-legacy/types/restyped'
import {mockInfoResponse} from 'core-legacy/api/mock/mocks/common'
import {mockActiveOrdersResponse, mockOrdersSearchResponse} from 'core-legacy/api/mock/mocks/orders'
import {mockRestaurantsResponse, restaurantId, restaurantIdMP} from 'core-legacy/api/mock/mocks/restaurants'
import {mockConfigsResponse, mockExperimentsResponse} from 'core-legacy/api/mock/mocks/experiments'
import ApiScenario from 'core-legacy/api/mock/ApiScenario'

export function getMinimalApiMock(apiScenario?: ApiScenario<VendorApi>) {
  const minimalApiMock = apiScenario || new ApiScenario<VendorApi>()

  minimalApiMock
    .mockGet('/4.0/restapp-front/api/v1/client/info', mockInfoResponse())
    .mockGet('/4.0/restapp-front/api/v1/client/restaurants', mockRestaurantsResponse())
    .mockGet('/4.0/restapp-front/api/v1/client/orders/active', mockActiveOrdersResponse())
    .mockPost('/4.0/restapp-front/api/v1/client/orders/search', mockOrdersSearchResponse())
    .mockPost('/4.0/restapp-front/eats/v1/ab/experiments' as any, mockExperimentsResponse())
    .mockPost('/api/configs' as any, mockConfigsResponse())

  return minimalApiMock
}
