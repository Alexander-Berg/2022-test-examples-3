import ApiMock from 'core-legacy/api/mock/ApiMock'
import createEnv from 'core-legacy/test-utils/create-env'
import serializeModel from 'core-legacy/test-utils/serialize-model'
import AppState, {AppStateType} from 'core-legacy/AppState'

import {singleOrder, search, activeOrders} from './mocks'
import {VendorApi} from 'core-legacy/types/restyped'

describe('Orders', () => {
  const api = new ApiMock<VendorApi>()
  const env = createEnv({api})

  api.mockGet(
    `/4.0/restapp-front/api/v1/client/orders/${singleOrder.payload.id}` as '/4.0/restapp-front/api/v1/client/orders/:id',
    singleOrder
  )
  api.mockPost('/4.0/restapp-front/api/v1/client/orders/search', search)
  api.mockGet('/4.0/restapp-front/api/v1/client/orders/active', activeOrders)

  const appState: AppStateType = AppState.create({auth: {token: 'token'}}, env)
  const orders = appState.orders

  test('Initial state', async () => {
    expect(serializeModel(orders)).toMatchSnapshot()
  })

  test('getOrder', async () => {
    await orders.getOrder(singleOrder.payload.id)

    expect(serializeModel(orders)).toMatchSnapshot()
  })

  test('getOrderById', async () => {
    await orders.getOrderById(singleOrder.payload.id)

    expect(serializeModel(orders)).toMatchSnapshot()
  })

  test('getOrders', async () => {
    await orders.getOrders()

    expect(serializeModel(orders)).toMatchSnapshot()
  })

  test('getActiveOrders', async () => {
    await orders.getActiveOrders()

    expect(serializeModel(orders)).toMatchSnapshot()
  })

  test('fetchOrders', async () => {
    await orders.fetchOrders()

    expect(serializeModel(orders)).toMatchSnapshot()
  })
})
