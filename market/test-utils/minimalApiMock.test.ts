import {getMinimalApiMock} from 'core-legacy/test-utils/minimalApiMock'
import {mockInfoResponse} from 'core-legacy/api/mock/mocks/common'

test.skip('api mock works correctly', async () => {
  const api = getMinimalApiMock()

  const response = await api.request.get('/4.0/restapp-front/api/v1/client/info')
  expect(response).toEqual(mockInfoResponse())

  api.mockPost('/4.0/restapp-front/api/v1/client/logs', {isSuccess: true})
  const response2 = await api.request.post('/4.0/restapp-front/api/v1/client/logs')

  expect(response2.isSuccess).toBe(true)
})
