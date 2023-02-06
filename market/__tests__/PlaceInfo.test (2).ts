import ApiMock from 'core-legacy/api/mock/ApiMock'
import {SnackbarNotifierStub} from 'core-legacy/test-utils/create-env'
import ShopInfo from 'gm-shops/models/Shops/ShopInfo/ShopInfo'
import {Instance} from 'mobx-state-tree'
import {mockShopInfo} from './mocks'

const testUpdate = {name: 'NewName'}
describe('ShopInfo', () => {
  const api = new ApiMock()
  const snackbarNotifier = new SnackbarNotifierStub()
  let shopInfo: Instance<typeof ShopInfo>

  beforeEach(() => {
    shopInfo = ShopInfo.create(mockShopInfo, {
      api,
      snackbarNotifier
    })
    snackbarNotifier.enqueue = jest.fn()
  })

  it('должен показываться снэкбар при успешном изменении данных о ресторане', async () => {
    api.mockPatch('/4.0/restapp-front/shops/v1/update', {results: []})
    await shopInfo.editShop(123, testUpdate)
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('должен показываться снэкбар при ошибке изменения данных о ресторане ', async () => {
    api.mockPatch('/4.0/restapp-front/shops/v1/update', {}, {status: 400})
    await shopInfo.editShop(123, testUpdate)
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('если пришел массив с ошибками, то они пробрасываются throw', async () => {
    const errors = [{error: 'error', name: 'name'}]

    api.request.patch = jest.fn().mockRejectedValue({
      code: '400',
      details: {errors}
    })

    try {
      await shopInfo.editShop(123, testUpdate)
    } catch (e) {
      expect(e).toBe(errors)
    }
  })
})
