import ApiMock from 'core-legacy/api/mock/ApiMock'
import createEnv from 'core-legacy/test-utils/create-env'
import serializeModel from 'core-legacy/test-utils/serialize-model'
import Exp3 from '../index'
import {VendorApi} from 'core-legacy/types/restyped'

describe('Exp3', () => {
  const apiMock = new ApiMock<VendorApi>()

  apiMock.mockPost('/api/configs' as any, {
    items: [
      {name: 'restapp_logs', value: {requestInterval: 1000}},
      {name: 'restapp_place_edit', value: {address_editable: false, name_editable: false, enabled: false}},
      {name: 'restapp_bundle_update_interval', value: {interval: 300}},
      {name: 'restapp_communications', value: {enabled: false}},
      {name: 'restapp_menu_sufficient_photo_widget', value: {enabled: true, sufficient_photo_percent: 75}},
      {name: 'restapp_discounts', value: {enabled: false, roles: ['ROLE_MANAGER']}},
      {
        name: 'restapp_push_notifications',
        value: {
          history_polling_socket_connected: 360,
          history_polling_socket_disconnected: 90,
          orders_polling_socket_connected: 120,
          orders_polling_socket_disconnected: 30
        }
      },
      {name: 'restapp_common', value: {configsPollInterval: 600000}},
    ],
    version: 361418
  })

  apiMock.mockPost('/4.0/restapp-front/eats/v1/ab/experiments' as any, {
    version: 333489,
    items: [{name: 'restapp_header_help_button', value: {enabled: true}}]
  })

  test('Initialization', async () => {
    const exp3 = Exp3.create({}, createEnv({api: apiMock}))

    expect(serializeModel(exp3)).toMatchSnapshot()

    await exp3.initialize()

    expect(serializeModel(exp3)).toMatchSnapshot()
  })

  test('Initialization reject on request fail', () => {
    apiMock.mock.onPost('/api/configs').reply(500, {})

    const exp3 = Exp3.create({}, createEnv({api: apiMock}))

    // tslint:disable-next-line:no-floating-promises
    expect(exp3.initialize()).rejects.toBeDefined()
  })
})
