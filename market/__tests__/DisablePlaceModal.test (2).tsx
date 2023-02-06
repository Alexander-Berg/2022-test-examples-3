import ApiScenario from 'core-legacy/api/mock/ApiScenario'
import React from 'react'
import {renderWithProviders} from 'core-legacy/test-utils'
import {VendorApi} from 'core-legacy/types/restyped'
import DisableShopModal from '../index'
import {fulfilledShopsEntities, shopId} from 'gm-shops/models/Shops/__tests__/mocks'
import {mockAutostopRules} from 'core-legacy/api/mock/mocks/shops'
import {activeOrders} from 'core-legacy/models/Orders/__tests__/mocks'
import ApiMock from 'core-legacy/api/mock/ApiMock'
import {getSnapshot} from 'mobx-state-tree'
import createEnv from 'core-legacy/test-utils/create-env'
import AppState, {AppStateType} from 'core-legacy/AppState'
import {fireEvent} from '@testing-library/react'
import serializeModel from 'core-legacy/test-utils/serialize-model'

describe('shop::disable-modal', () => {
  test('autostop modal', async () => {
    const api = new ApiMock<VendorApi>()

    api.mockGet(
      '/4.0/restapp-front/api/v1/client/orders/active',
      activeOrders as VendorApi['/4.0/restapp-front/api/v1/client/orders/active']['GET']['response']
    )
    api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', {
      isSuccess: true,
      payload: [fulfilledShopsEntities[shopId]]
    } as any)
    const env = createEnv({api})

    const appState: AppStateType = AppState.create({auth: {token: 'token'}}, env)

    const orders = appState.orders
    await orders.getActiveOrders()

    const {asFragment, getByText, getByTestId, getAllByRole, findByTestId} = await renderWithProviders(
      <DisableShopModal restaurant={appState.shops.getShopById(shopId)!} />,
      {
        stateSnapshot: {
          orders: getSnapshot(orders)
        },
        extendApiMock(api: ApiScenario<VendorApi>) {
          api.mockGet('/4.0/restapp-front/shop/v1/autostop-rules', mockAutostopRules, {
            params: {
              shopId
            }
          })
        }
      }
    )

    expect(asFragment()).toMatchSnapshot('loading')
    await findByTestId('ui__modal')
    expect(asFragment()).toMatchSnapshot('show autostop disable reason form')

    const secondReasonChip = getByText('Мероприятие, ЧП')
    fireEvent.click(secondReasonChip)
    const chipGroup = getByTestId('shop__autostop-modal-chip-group')
    expect(chipGroup).toMatchSnapshot('second chip selected')

    const disableShopButton = getAllByRole('button')[1]
    fireEvent.click(disableShopButton)
    expect(disableShopButton).toMatchSnapshot('button loading')

    expect(serializeModel(appState.shops)).toMatchSnapshot('pending disable shop')
  })
})
