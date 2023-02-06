import {SnackbarNotifierStub} from 'core-legacy/test-utils/create-env'
import {getEnv, getSnapshot, Instance} from 'mobx-state-tree'
import {when} from 'mobx'
import {IEnv} from 'core-legacy/types'
import {LoggerStub} from 'shared/test-utils'
import {createPlaceFeatureModel, PlaceFeature, PlacesFeaturesResponse} from './createPlaceFeatureModel'

interface TestFeature extends PlaceFeature {
  status: string
}

const STATUS_RESPONSE_MOCK: PlacesFeaturesResponse<TestFeature> = {
  places: [
    {place_id: 1, status: 'disabled'},
    {place_id: 2, status: 'disabled'},
    {place_id: 3, status: 'enabled'},
    {place_id: 4, status: 'disabled'}
  ]
}

const ENABLE_RESPONSE_MOCK: PlacesFeaturesResponse<TestFeature> = {
  places: [{place_id: 1, status: 'enabled'}]
}

const DISABLE_RESPONSE_MOCK: PlacesFeaturesResponse<TestFeature> = {
  places: [{place_id: 3, status: 'disabled'}]
}

describe('createPlaceFeatureModel', () => {
  const getStatusesMock = jest.fn().mockResolvedValue(STATUS_RESPONSE_MOCK)
  const enableMock = jest.fn().mockResolvedValue(ENABLE_RESPONSE_MOCK)
  const disableMock = jest.fn().mockResolvedValue(DISABLE_RESPONSE_MOCK)

  const PlaceFeatureModel = createPlaceFeatureModel<TestFeature, IEnv>('PlaceFeature', () => ({
    getStatuses: getStatusesMock,
    enable: enableMock,
    disable: disableMock
  }))
  let placeFeature: Instance<typeof PlaceFeatureModel>
  const placeIds = [1, 2, 3, 4]
  const snackbarNotifier = new SnackbarNotifierStub()

  beforeEach((done) => {
    placeFeature = PlaceFeatureModel.create({}, {logger: new LoggerStub(), snackbarNotifier})
    placeFeature.setPlaceIds(placeIds)
    when(() => placeFeature.isReady, done)
  })

  it('должен отправиться запрос за статусами фичи', () => {
    expect(getStatusesMock).toBeCalled()
  })

  it('должно логировать ошибку при неуспешном запросе статусов фичи', async () => {
    const {logger} = getEnv<IEnv>(placeFeature)
    getStatusesMock.mockRejectedValueOnce(new Error('error'))
    logger.error = jest.fn()
    await placeFeature.refresh()
    expect(logger.error).toBeCalled()
  })

  it('должно очищать статусы фичи при передаче пустого массива ресторанов', () => {
    placeFeature.setPlaceIds([])
    expect(getSnapshot(placeFeature.placeIdToFeature)).toEqual({})
  })

  it('getByPlaceId', () => {
    expect(placeFeature.getByPlaceId(1)).toEqual(STATUS_RESPONSE_MOCK.places[0])
    expect(placeFeature.getByPlaceId(-5)).toBeUndefined()
  })

  it('enable', async () => {
    expect(placeFeature.getByPlaceId(1)?.status).toEqual('disabled')
    await placeFeature.enable([1])
    expect(placeFeature.getByPlaceId(1)?.status).toEqual('enabled')
    expect(enableMock).toBeCalledWith([1])
  })

  it('должен показываться снэкбар при ошибке включения фичи', async () => {
    snackbarNotifier.enqueue = jest.fn()
    enableMock.mockRejectedValueOnce(new Error('error'))
    await placeFeature.enable([1])
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('disable', async () => {
    expect(placeFeature.getByPlaceId(3)?.status).toEqual('enabled')
    await placeFeature.disable([3])
    expect(placeFeature.getByPlaceId(3)?.status).toEqual('disabled')
    expect(disableMock).toBeCalledWith([3])
  })

  it('должен показываться снэкбар при ошибке выключения самовывоза', async () => {
    snackbarNotifier.enqueue = jest.fn()
    disableMock.mockRejectedValueOnce(new Error('error'))
    await placeFeature.disable([3])
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('isLoading при включении/выключении фичи', async () => {
    expect(placeFeature.isLoading(-1)).toBe(false)

    expect(placeFeature.isLoading(1)).toBe(false)
    const enablePromise = placeFeature.enable([1])
    expect(placeFeature.isLoading(1)).toBe(true)
    await enablePromise
    expect(placeFeature.isLoading(1)).toBe(false)

    expect(placeFeature.isLoading(3)).toBe(false)
    const disablePromise = placeFeature.disable([3])
    expect(placeFeature.isLoading(3)).toBe(true)
    await disablePromise
    expect(placeFeature.isLoading(3)).toBe(false)
  })

  it('update', () => {
    const disabledFeature = {place_id: 3, status: 'disabled'}

    expect(placeFeature.getByPlaceId(3)?.status).toBe('enabled')

    placeFeature.update(disabledFeature)

    expect(placeFeature.getByPlaceId(3)?.status).toBe('disabled')
  })
})
