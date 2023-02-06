import {SHOP_STATUS} from 'gm-shops/models/Shops/ShopStatus'
import {ShopType} from 'gm-shops/models/Shops/Shop'
import {Shops} from 'gm-shops/models/Shops/Shops/Shops'

export const shopsAllMock = ([
  {
    id: 1,
    selfName: 'НанаНуна',
    selfAddress: 'Москва, улица Петровка, 20/1',
    iaNative: true,
    status: {shopStatus: SHOP_STATUS.ACTIVE}
  },
  {
    id: 2,
    selfName: 'Булка',
    selfAddress: 'Москва, Дорожная улица, 60Б',
    isNative: true,
    status: {shopStatus: SHOP_STATUS.DISABLED}
  },
  {
    id: 3,
    selfName: 'Макдоналдс',
    selfAddress: 'Москва, Варшавское шоссе, 87А',
    isNative: true,
    status: {shopStatus: SHOP_STATUS.DISABLE_PENDING}
  },
  {
    id: 4,
    selfName: 'Суши-Пицца',
    selfAddress: 'Москва, улица Хамовнический Вал, 34',
    isNative: true,
    status: {shopStatus: SHOP_STATUS.SELF_REG_PENDING}
  },
  {
    id: 5,
    selfName: 'Пицца Плюс',
    selfAddress: 'Москва, Долгопрудная аллея, 15к1',
    isNative: true,
    status: {shopStatus: SHOP_STATUS.SELF_REG_PENDING}
  }
] as any) as ShopType[]

const emptyFunction = () => {}
export const shopsMock = ({
  selectedRestType: 'all',
  setSelectedRestType: emptyFunction,
  all: shopsAllMock,
  filteredShops: shopsAllMock,
  hasOnlyOneShop: false,
  first: shopsAllMock[0]
} as any) as Shops
