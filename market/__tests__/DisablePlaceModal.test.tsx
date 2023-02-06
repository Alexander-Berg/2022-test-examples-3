import ApiScenario from 'core-legacy/api/mock/ApiScenario'
import React from 'react'
import {renderWithProviders} from 'core-legacy/test-utils'
import {VendorApi} from 'core-legacy/types/restyped'
import DisablePlaceModal from '../index'
import {fulfilledPlacesEntities, placeId} from 'core-legacy/models/Places/__tests__/mocks'
import {mockAutostopRules} from 'core-legacy/api/mock/mocks/places'
import {activeOrders} from 'core-legacy/models/Orders/__tests__/mocks'
import ApiMock from 'core-legacy/api/mock/ApiMock'
import {getSnapshot} from 'mobx-state-tree'
import createEnv from 'core-legacy/test-utils/create-env'
import AppState, {AppStateType} from 'core-legacy/AppState'
import {fireEvent} from '@testing-library/react'
import serializeModel from 'core-legacy/test-utils/serialize-model'

describe('place::disable-modal', () => {
  test('autostop modal', async () => {
    const api = new ApiMock<VendorApi>()

    api.mockGet(
      '/4.0/restapp-front/api/v1/client/orders/active',
      activeOrders as VendorApi['/4.0/restapp-front/api/v1/client/orders/active']['GET']['response']
    )
    api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', {
      isSuccess: true,
      payload: [fulfilledPlacesEntities[placeId]]
    } as any)
    const env = createEnv({api})

    const appState: AppStateType = AppState.create({auth: {token: 'token'}}, env)

    const orders = appState.orders
    await orders.getActiveOrders()

    const {asFragment, getByText, getByTestId, getAllByRole, findByTestId} = await renderWithProviders(
      <DisablePlaceModal restaurant={appState.places.getPlaceById(placeId)!} />,
      {
        stateSnapshot: {
          orders: getSnapshot(orders)
        },
        extendApiMock(api: ApiScenario<VendorApi>) {
          api.mockGet('/4.0/restapp-front/place/v1/autostop-rules', mockAutostopRules, {
            params: {
              placeId
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
    const chipGroup = getByTestId('place__autostop-modal-chip-group')
    expect(chipGroup).toMatchSnapshot('second chip selected')

    const disablePlaceButton = getAllByRole('button')[1]
    fireEvent.click(disablePlaceButton)
    expect(disablePlaceButton).toMatchSnapshot('button loading')

    expect(serializeModel(appState.places)).toMatchSnapshot('pending disable place')
  })
})
