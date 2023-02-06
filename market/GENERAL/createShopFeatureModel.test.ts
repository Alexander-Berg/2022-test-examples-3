import {SnackbarNotifierStub} from 'core-legacy/test-utils/create-env'
import {getEnv, getSnapshot, Instance} from 'mobx-state-tree'
import {when} from 'mobx'
import {IEnv} from 'core-legacy/types'
import {LoggerStub} from 'shared/test-utils'
import {createShopFeatureModel, ShopFeature, ShopsFeaturesResponse} from './createShopFeatureModel'

interface TestFeature extends ShopFeature {
  status: string
}

const STATUS_RESPONSE_MOCK: ShopsFeaturesResponse<TestFeature> = {
  shops: [
    {shop_id: 1, status: 'disabled'},
    {shop_id: 2, status: 'disabled'},
    {shop_id: 3, status: 'enabled'},
    {shop_id: 4, status: 'disabled'}
  ]
}

const ENABLE_RESPONSE_MOCK: ShopsFeaturesResponse<TestFeature> = {
  shops: [{shop_id: 1, status: 'enabled'}]
}

const DISABLE_RESPONSE_MOCK: ShopsFeaturesResponse<TestFeature> = {
  shops: [{shop_id: 3, status: 'disabled'}]
}

describe('createShopFeatureModel', () => {
  const getStatusesMock = jest.fn().mockResolvedValue(STATUS_RESPONSE_MOCK)
  const enableMock = jest.fn().mockResolvedValue(ENABLE_RESPONSE_MOCK)
  const disableMock = jest.fn().mockResolvedValue(DISABLE_RESPONSE_MOCK)

  const ShopFeatureModel = createShopFeatureModel<TestFeature, IEnv>('ShopFeature', (env) => ({
    getStatuses: getStatusesMock,
    enable: enableMock,
    disable: disableMock
  }))
  let shopFeature: Instance<typeof ShopFeatureModel>
  const shopIds = [1, 2, 3, 4]
  const snackbarNotifier = new SnackbarNotifierStub()

  beforeEach((done) => {
    shopFeature = ShopFeatureModel.create({}, {logger: new LoggerStub(), snackbarNotifier})
    shopFeature.setShopIds(shopIds)
    when(() => shopFeature.isReady, done)
  })

  it('должен отправиться запрос за статусами фичи', () => {
    expect(getStatusesMock).toBeCalled()
  })

  it('должно логировать ошибку при неуспешном запросе статусов фичи', async () => {
    const {logger} = getEnv<IEnv>(shopFeature)
    getStatusesMock.mockRejectedValueOnce(new Error('error'))
    logger.error = jest.fn()
    await shopFeature.refresh()
    expect(logger.error).toBeCalled()
  })

  it('должно очищать статусы фичи при передаче пустого массива ресторанов', () => {
    shopFeature.setShopIds([])
    expect(getSnapshot(shopFeature.shopIdToFeature)).toEqual({})
  })

  it('getByShopId', () => {
    expect(shopFeature.getByShopId(1)).toEqual(STATUS_RESPONSE_MOCK.shops[0])
    expect(shopFeature.getByShopId(-5)).toBeUndefined()
  })

  it('enable', async () => {
    expect(shopFeature.getByShopId(1)?.status).toEqual('disabled')
    await shopFeature.enable([1])
    expect(shopFeature.getByShopId(1)?.status).toEqual('enabled')
    expect(enableMock).toBeCalledWith([1])
  })

  it('должен показываться снэкбар при ошибке включения фичи', async () => {
    snackbarNotifier.enqueue = jest.fn()
    enableMock.mockRejectedValueOnce(new Error('error'))
    await shopFeature.enable([1])
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('disable', async () => {
    expect(shopFeature.getByShopId(3)?.status).toEqual('enabled')
    await shopFeature.disable([3])
    expect(shopFeature.getByShopId(3)?.status).toEqual('disabled')
    expect(disableMock).toBeCalledWith([3])
  })

  it('должен показываться снэкбар при ошибке выключения самовывоза', async () => {
    snackbarNotifier.enqueue = jest.fn()
    disableMock.mockRejectedValueOnce(new Error('error'))
    await shopFeature.disable([3])
    expect(snackbarNotifier.enqueue).toBeCalled()
  })

  it('isLoading при включении/выключении фичи', async () => {
    expect(shopFeature.isLoading(-1)).toBe(false)

    expect(shopFeature.isLoading(1)).toBe(false)
    const enablePromise = shopFeature.enable([1])
    expect(shopFeature.isLoading(1)).toBe(true)
    await enablePromise
    expect(shopFeature.isLoading(1)).toBe(false)

    expect(shopFeature.isLoading(3)).toBe(false)
    const disablePromise = shopFeature.disable([3])
    expect(shopFeature.isLoading(3)).toBe(true)
    await disablePromise
    expect(shopFeature.isLoading(3)).toBe(false)
  })

  it('update', () => {
    const disabledFeature = {shop_id: 3, status: 'disabled'}

    expect(shopFeature.getByShopId(3)?.status).toBe('enabled')

    shopFeature.update(disabledFeature)

    expect(shopFeature.getByShopId(3)?.status).toBe('disabled')
  })
})
