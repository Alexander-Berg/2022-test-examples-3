import {PLACE_STATUS} from 'core-legacy/models/Places/PlaceStatus'
import {PlaceType} from 'core-legacy/models/Places/Place'
import {Places} from 'core-legacy/models/Places/Places/Places'

export const placesAllMock = ([
  {
    id: 1,
    selfName: 'НанаНуна',
    selfAddress: 'Москва, улица Петровка, 20/1',
    iaNative: true,
    status: {placeStatus: PLACE_STATUS.ACTIVE},
  },
  {
    id: 2,
    selfName: 'Булка',
    selfAddress: 'Москва, Дорожная улица, 60Б',
    isNative: true,
    status: {placeStatus: PLACE_STATUS.DISABLED},
  },
  {
    id: 3,
    selfName: 'Макдоналдс',
    selfAddress: 'Москва, Варшавское шоссе, 87А',
    isNative: true,
    status: {placeStatus: PLACE_STATUS.DISABLE_PENDING},
  },
  {
    id: 4,
    selfName: 'Суши-Пицца',
    selfAddress: 'Москва, улица Хамовнический Вал, 34',
    isNative: true,
    status: {placeStatus: PLACE_STATUS.SELF_REG_PENDING},
  },
  {
    id: 5,
    selfName: 'Пицца Плюс',
    selfAddress: 'Москва, Долгопрудная аллея, 15к1',
    isNative: true,
    status: {placeStatus: PLACE_STATUS.SELF_REG_PENDING},
  }
] as any) as PlaceType[]

const emptyFunction = () => {}
export const placesMock = ({
  selectedRestType: 'all',
  setSelectedRestType: emptyFunction,
  all: placesAllMock,
  filteredPlaces: placesAllMock,
  hasOnlyOnePlace: false,
  first: placesAllMock[0]
} as any) as Places
