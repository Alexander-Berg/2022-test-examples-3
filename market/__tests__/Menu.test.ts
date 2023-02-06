import {getEnv} from 'mobx-state-tree'
import {AppStateType} from 'core-legacy/AppState'
import {TestingEnv} from 'core-legacy/test-utils/create-env'
import {createProvidersWrapper} from 'core-legacy/test-utils'

import Menu, {IMenuEnv, MenuBody, MenuType} from '../Menu'
import {REVISION, CATEGORIES, ITEMS, MENU, DEFAULT_CATEGORY, UPDATE_RESPONSE} from './mocks'
import Category, {MenuCategoryType} from '../Category'

describe('Menu model', async () => {
  let env: TestingEnv
  let menu: MenuType | null
  let state: AppStateType

  beforeEach(async () => {
    const provider = await createProvidersWrapper({
      stateSnapshot: {
        auth: {token: 'token', directToken: 'directToken'},
        exp3: {
          configs: {
          }
        }
      }
    })

    state = provider.appState
    env = provider.env
    menu = state?.context?.place?.menu ?? Menu.create({})
  })

  afterAll(() => {
    jest.clearAllMocks()
  })

  it('Логи на ошибки новых ручек', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet(
      '/4.0/restapp-front/eats-restapp-menu/v1/menu/revision' as any,
      {},
      {
        status: 400,
        params: {
          place_id: menu?.place.id
        }
      }
    )
    env.api.mockPost(
      `/4.0/restapp-front/eats-restapp-menu/v1/menu/update?place_id=${menu?.place.id}&revision=${REVISION}` as any,
      {},
      {status: 400}
    )
    env.api.mockGet(
      '/4.0/restapp-front/eats-restapp-menu/v1/menu/active' as any,
      {},
      {
        status: 400,
        params: {
          place_id: menu?.place.id
        }
      }
    )

    const requestsArray = [
      menu?.loadMenuWithNewApi(),
      menu?.loadRevision(),
      menu?.updateMenuWithNewApi({categories: CATEGORIES, items: ITEMS})
    ]

    await Promise.all(requestsArray)
    expect(logger.error).toBeCalledTimes(requestsArray.length)
  })

  it('Логи ошибок старых ручек', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet(
      `/4.0/restapp-front/api/v1/client/menu/${menu?.place.id}/reactivate-timestamps` as any,
      {},
      {status: 400}
    )
    env.api.mockPost(`/4.0/restapp-front/api/v1/client/place/${menu?.place.id}/menu` as any, {}, {status: 400})
    env.api.mockGet('/4.0/restapp-front/api/v1/client/categories/standard' as any, {}, {status: 400})
    env.api.mockGet(`/4.0/restapp-front/api/v1/client/place/${menu?.place.id}/menu` as any, {}, {status: 400})

    const requestsArray = [
      menu?.updateMenuWithOldApi({categories: CATEGORIES, items: ITEMS}),
      menu?.loadReactivateDates(),
      menu?.loadDefaultCategories(),
      menu?.loadMenuWithOldApi()
    ]

    await Promise.all(requestsArray)
    expect(logger.error).toBeCalledTimes(requestsArray.length)
  })

  it('вызов loadMenuWithNewApi без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet('/4.0/restapp-front/eats-restapp-menu/v1/menu/active' as any, MENU, {
      params: {place_id: menu?.place.id}
    })

    const response = await menu?.loadMenuWithNewApi()

    expect(logger.error).not.toBeCalled()
    expect(response).toEqual({items: ITEMS, categories: CATEGORIES})
  })

  it('вызов loadMenuWithOldApi без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet(`/4.0/restapp-front/api/v1/client/place/${menu?.place.id}/menu` as any, {
      payload: {categories: CATEGORIES, items: ITEMS}
    })

    const response = await state?.context?.place?.menu?.loadMenuWithOldApi()

    expect(logger.error).not.toBeCalled()
    expect(response).toEqual({items: ITEMS, categories: CATEGORIES} as MenuBody)
  })

  it('вызов updateMenuWithOldApi без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockPost(`/4.0/restapp-front/api/v1/client/place/${menu?.place.id}/menu` as any, {
      isNewJob: true,
      jobId: '123456'
    })

    const response = await menu?.updateMenuWithOldApi({categories: CATEGORIES, items: ITEMS})

    expect(logger.error).not.toBeCalled()
    expect(response).toEqual({isNewJob: true, jobId: '123456'})
  })

  it('вызов loadRevision без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet('/4.0/restapp-front/eats-restapp-menu/v1/menu/revision' as any, {
      params: {place_id: menu?.place.id, revision: REVISION}
    })

    await menu?.loadRevision()

    expect(logger.error).not.toBeCalled()
  })

  it('Вызов updateMenuWithNewApi без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockPost(
      `/4.0/restapp-front/eats-restapp-menu/v1/menu/update?place_id=${menu?.place.id}&revision=${menu?.revision}` as any,
      UPDATE_RESPONSE,
      {status: 200}
    )

    const response = await menu?.updateMenuWithNewApi({categories: CATEGORIES, items: ITEMS})

    expect(logger.error).not.toBeCalled()
    expect(response).toEqual(UPDATE_RESPONSE)
  })

  it('Вызов loadDefaultCategories без ошибок', async () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.error = jest.fn()

    env.api.mockGet(
      '/4.0/restapp-front/api/v1/client/categories/standard' as any,
      {categories: DEFAULT_CATEGORY},
      {status: 200}
    )

    await menu?.loadDefaultCategories()

    expect(logger.error).not.toBeCalled()
    expect(menu?.defaultCategories).toEqual(DEFAULT_CATEGORY)
  })

  it('switchOffPhotoWidget вернет false', () => {
    expect(menu?.photoPercentWidgetIsOpen).toBeFalsy()

    menu?.switchOffPhotoWidget()

    expect(menu?.photoPercentWidgetIsOpen).toBeFalsy()
  })

  it('switchOffPhotoWidget вернет true', () => {
    expect(menu?.photoPercentWidgetIsOpen).toBeFalsy()

    menu?.togglePhotoWidget()

    expect(menu?.photoPercentWidgetIsOpen).toBeTruthy()
  })

  it('currency', () => {
    expect(menu?.currency).toEqual({code: 'RUB', sign: '₽'})
  })

  it('integrationType вернет native', () => {
    expect(menu?.integrationType).toBe('native')
  })

  it('timestamps вернет пустой массив', () => {
    expect(menu?.timestamps).toEqual([])
  })

  it('timestamps вернет timestampsData', () => {
    const {logger} = getEnv<IMenuEnv>(menu as MenuType)
    logger.info = jest.fn()

    expect(menu?.timestamps).toEqual([])

    menu?.selectCategory(Category.create(CATEGORIES[0]) as MenuCategoryType)

    expect(menu?.photoPercentWidgetIsOpen).toBeFalsy()
    expect(logger.info).toBeCalled()
  })

  it('setSearch', () => {
    menu?.setSearch('search')
    expect(menu?.search).toBe('search')

    menu?.setSearch('')
    expect(menu?.search).toBe('')
  })
})
