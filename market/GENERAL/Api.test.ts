import Api from './Api'
import MockAdapter from 'axios-mock-adapter'
import {AxiosInstance} from 'axios'

describe('Api', () => {
  test('заголовок x-yarequestid устанавливается с первым запросом и отправляется при каждом следующем запросе', async () => {
    const api = new Api<{'/test': {}}>('')
    const mockAdapter = new MockAdapter(api.request as AxiosInstance)
    const firstRequestIdMock = '123'

    mockAdapter.onGet().replyOnce(200, {}, {[api.requestIdHeader]: firstRequestIdMock})
    mockAdapter.onAny().reply(200, {}, {[api.requestIdHeader]: Math.random().toString()})

    await api.request.get('/test')
    expect(api.requestId).toEqual(firstRequestIdMock)

    const sentRequestIds: string[] = []

    api.request.interceptors.request.use((config) => {
      sentRequestIds.push(config.headers[api.requestIdHeader])
      return config
    })

    await api.request.get('/test')
    await api.request.post('/test')
    await api.request.put('/test')
    await api.request.delete('/test')

    expect(api.requestId).toEqual(firstRequestIdMock)
    expect(sentRequestIds).toHaveLength(4)
    expect(sentRequestIds.every((id) => id === firstRequestIdMock)).toBe(true)
  })
})
