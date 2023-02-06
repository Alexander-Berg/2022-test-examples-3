import ApiMock from 'core-legacy/api/mock/ApiMock'
import {VendorApi} from 'core-legacy/types/restyped'

test('api mock', async () => {
  const apiMock = new ApiMock<VendorApi>()

  apiMock.mockGet('/4.0/restapp-front/marketing/v1/ad/balance', {balance: '1'})

  const {balance} = await apiMock.request.get('/4.0/restapp-front/marketing/v1/ad/balance')
  expect(balance).toEqual('1')

  // Mock once doesnt overrides default mock
  apiMock.mockGet('/4.0/restapp-front/marketing/v1/ad/balance', {balance: '2'}, {once: true})
  const {balance: balance2} = await apiMock.request.get('/4.0/restapp-front/marketing/v1/ad/balance')

  expect(balance2).toEqual('1')
})
