import {PLACE_STATUS} from 'core-legacy/models/Places/PlaceStatus'

export const mockPlaceStatusList = {
  enabledRestaurants: [],
  selfRegCompleteRestaurants: [],
  selfRegPendingRestaurants: [],
  all: [
    {
      id: 1,
      selfName: 'НанаНуна',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.ACTIVATION_PENDING}
    },
    {
      id: 2,
      selfName: 'Булка',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.DISABLED_BY_AUTOSTOP}
    },
    {
      id: 3,
      selfName: 'Макдоналдс',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.DISABLE_PENDING}
    },
    {
      id: 4,
      selfName: 'Суши-Пицца',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.SELF_REG_PENDING}
    },
    {
      id: 5,
      selfName: 'Пицца Плюс',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.SELF_REG_COMPLETE}
    },
    {
      id: 6,
      selfName: 'Бургер кинг',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.ACTIVE}
    },
    {
      id: 7,
      selfName: 'Нихон',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.DISABLED}
    },
    {
      id: 8,
      selfName: 'Крем сода',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.DISABLED_BY_VENDOR}
    },
    {
      id: 9,
      selfName: 'Пишем',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.DISABLED_BY_YANDEX}
    },
    {
      id: 10,
      selfName: 'Чилис',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.CAN_NOT_BE_ENABLED}
    },
    {
      id: 11,
      selfName: "People's",
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.ACTIVE}
    },
    {
      id: 12,
      selfName: 'НанаНуна',
      selfAddress: 'Москва, улица Петровка, 20/1',
      isNative: true,
      status: {placeStatus: PLACE_STATUS.ACTIVE}
    }
  ]
}
