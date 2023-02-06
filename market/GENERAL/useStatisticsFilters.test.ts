import {renderHook, WrapperComponent} from '@testing-library/react-hooks'

import {createProvidersWrapper} from 'core-legacy/test-utils'

import useStatisticsFilters from './useStatisticsFilters'
import {mockMultipleRestaurantsResponse} from 'core-legacy/api/mock/mocks/restaurants'
import moment from 'moment'

const NON_EXISTEND_PLACE_ID = 13134134135135
const DEFAULT_PLACE_ID = '123'
const DEFAULT_FROM = '2021-11-11'
const DEFAULT_TO = '2021-11-01'
const DEFAULT_PROMO_TYPE = 'gift'

export const PROMOS_TYPES = ['gift', 'discount_menu', 'discount_part', 'one_plus_one']

const resetUrlPath = () => {
  window.history.replaceState({}, 'Promos', '/metrics/promos')
}

const renderStatisticsHook = (wrapper: WrapperComponent<unknown>) =>
  renderHook(
    () =>
      useStatisticsFilters(
        {
          placeId: DEFAULT_PLACE_ID,
          from: DEFAULT_FROM,
          to: DEFAULT_TO,
          promoType: DEFAULT_PROMO_TYPE
        },
        ({placeId: placeIdToValidate, from: fromToValidate, to: toValidate, promoType: promoTypeToValidate}) => {
          const placeFound = true

          const momentFrom = moment(fromToValidate)
          const momentTo = moment(toValidate)

          const isFromValid = fromToValidate && momentFrom.isValid
          const isToValid =
            toValidate && momentTo.isValid && momentTo.isSameOrAfter(momentFrom) && momentTo.isSameOrBefore(moment())

          const isPromoTypeValid = promoTypeToValidate && PROMOS_TYPES.includes(promoTypeToValidate)

          return !!(placeIdToValidate && placeFound && isFromValid && isToValid && isPromoTypeValid)
        }
      ),
    {wrapper}
  )

beforeEach(resetUrlPath)

afterEach(resetUrlPath)

describe('useStatisticsFilters', () => {
  test('В случае url без параметров, выставляет дефолтные', async () => {
    const {wrapper} = await createProvidersWrapper({
      extendApiMock(api) {
        api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', mockMultipleRestaurantsResponse())
      }
    })
    const {result} = renderStatisticsHook(wrapper)

    expect(result.current).toMatchSnapshot('valid response')
  })

  test('В случае невалидного url, выставляет дефолтные', async () => {
    window.history.replaceState({}, 'Promos', `/metrics/promos?placeId=${NON_EXISTEND_PLACE_ID}`)

    const {wrapper} = await createProvidersWrapper({
      extendApiMock(api) {
        api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', mockMultipleRestaurantsResponse())
      }
    })
    const {result} = renderStatisticsHook(wrapper)

    expect(result.current).toMatchSnapshot('valid response')
  })

  test('Метод апдейта фильтров - невалидное значение', async () => {
    const {wrapper} = await createProvidersWrapper({
      extendApiMock(api) {
        api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', mockMultipleRestaurantsResponse())
      }
    })
    const {result} = renderStatisticsHook(wrapper)

    result.current.onFilterChange('placeId', NON_EXISTEND_PLACE_ID.toString())

    expect(result.current.isValidFilters).toBe(false)
  })

  test('Метод апдейта фильтров - валидное значение', async () => {
    const {wrapper} = await createProvidersWrapper({
      extendApiMock(api) {
        api.mockGet('/4.0/restapp-front/api/v1/client/restaurants', mockMultipleRestaurantsResponse())
      }
    })
    const {result} = renderStatisticsHook(wrapper)

    const NEW_PROMO_TYPE_VALUE = 'discount_menu'

    result.current.onFilterChange('promoType', NEW_PROMO_TYPE_VALUE)

    expect(result.current.promoType).toBe(NEW_PROMO_TYPE_VALUE)
  })
})
