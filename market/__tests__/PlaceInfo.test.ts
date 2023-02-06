import ApiMock from 'core-legacy/api/mock/ApiMock'
import {SnackbarNotifierStub} from 'core-legacy/test-utils/create-env'
import PlaceInfo from 'core-legacy/models/Places/PlaceInfo/PlaceInfo'
import {Instance} from 'mobx-state-tree'
import {mockPlaceInfo} from './mocks'

const testUpdate = {name: 'NewName'}
describe('PlaceInfo', () => {
  const api = new ApiMock()
  const snackbarNotifier = new SnackbarNotifierStub()
  let placeInfo: Instance<typeof PlaceInfo>

  beforeEach(() => {
    placeInfo = PlaceInfo.create(mockPlaceInfo, {
      api,
      snackbarNotifier
    })
    snackbarNotifier.enqueue = jest.fn()
  })

  it('должен показываться снэкбар при успешном изменении данных о ресторане', async () => {
    api.mockPatch('/4.0/restapp-front/places/v1/update', {results: []})
    await placeInfo.editPlace(123, testUpdate)
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('должен показываться снэкбар при ошибке изменения данных о ресторане ', async () => {
    api.mockPatch('/4.0/restapp-front/places/v1/update', {}, {status: 400})
    await placeInfo.editPlace(123, testUpdate)
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('если пришел массив с ошибками, то они пробрасываются throw', async () => {
    const errors = [{error: 'error', name: 'name'}]

    api.request.patch = jest.fn().mockRejectedValue({
      code: '400',
      details: {errors}
    })

    try {
      await placeInfo.editPlace(123, testUpdate)
    } catch (e) {
      expect(e).toBe(errors)
    }
  })
})
