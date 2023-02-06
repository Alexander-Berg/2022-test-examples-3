import {
  ERROR_FIELD_MAX_LENGTH,
  ERROR_FIELD_REQUIRED,
  MAX_FIELD_LENGTH,
  transformGeoResponse,
  validateAddress
} from 'gm-shops/models/Shops/ShopInfo/utils'

describe('ShopInfo utils', () => {
  it('transformGeoResponse корректно преобразовывает GeoResponse', () => {
    const result = transformGeoResponse([
      {kind: 'country', name: 'Россия'},
      {kind: 'province', name: 'Центральный федеральный округ'},
      {kind: 'province', name: 'Москва'},
      {kind: 'locality', name: 'Москва'},
      {kind: 'street', name: 'Цветной бульвар'},
      {kind: 'house', name: '2'}
    ])
    const expectedResult = {city: 'Москва', address: 'Цветной бульвар, 2'}
    expect(result).toEqual(expectedResult)
  })

  it('transformGeoResponse корректно преобразовывает пустой GeoResponse', () => {
    const result = transformGeoResponse([])
    const expectedResult = {city: '', address: ''}
    expect(result).toEqual(expectedResult)
  })

  it('валидация корректного адреса', () => {
    expect(validateAddress('Россия, Москва, Цветной бульвар, 2, A')).toBe(undefined)
  })

  it('валидация пустого адреса', () => {
    expect(validateAddress('')).toBe(ERROR_FIELD_REQUIRED)
  })

  it('валидация некорректного адреса', () => {
    const invalidAddress = 'address'.repeat(37)
    const expectedResult = invalidAddress.length > MAX_FIELD_LENGTH ? ERROR_FIELD_MAX_LENGTH : undefined
    expect(validateAddress(invalidAddress)).toBe(expectedResult)
  })
})
