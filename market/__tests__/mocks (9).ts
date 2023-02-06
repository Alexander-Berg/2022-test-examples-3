import {SnapshotIn} from 'mobx-state-tree'
import ShopInfo from '../ShopInfo'

export const mockShopInfo: SnapshotIn<typeof ShopInfo> = {
  info: {
    name: 'Бургер кинг1235567',
    type: 'native',
    address: {
      country: 'Российская Федерация',
      city: 'Москва',
      street: 'улица Новый Арбат',
      building: '1с2',
      full: 'Россия, Москва, улица Новый Арбат, 1с2'
    },
    phones: [
      {
        number: '+79999999999',
        type: 'auto_call',
        description: null
      },
      {
        number: '+79999999999',
        type: 'official',
        description: ''
      },
      {
        number: '+79999999999',
        type: 'lpr',
        description: ''
      }
    ],
    email: 'aleksej.kirillov@zuravleva.com',
    lprEmail: 'arbat-k@blackstarburger.ru, Arbat@bdpa.ru, pro12@pro.ru',
    payments: ['Наличный расчет', 'Безналичный расчет'],
    addressComment: null,
    clientComment: null
  },
  shop_part: 10,
  billing: {
    inn: '772441870936',
    kpp: null,
    bik: '044525225',
    account: '40802810038000077669',
    name: 'ИП Кудимов Иван Владимирович',
    address: {
      postcode: '115612',
      full: 'Москва г, Борисовские Пруды ул, дом № 18, корпус 1, квартира 251'
    },
    postAddress: {
      postcode: '119019',
      full: 'Москва, ул Новый Арбат, 15 стр 1'
    },
    accountancyPhone: {
      number: '+79670414910',
      type: 'accountancy',
      description: ''
    },
    accountancyEmail: 'Arbat@bdpa.ru,cb.calc@bdpa.ru',
    signer: null,
    balanceExternalId: '180514-02',
    balanceDateStart: '2019-07-01'
  },
  commission: [
    {
      type: 'delivery',
      value: 23,
      acquiring: 0,
      fixed: null
    },
    {
      type: 'delivery',
      value: 10,
      acquiring: 0,
      fixed: null
    }
  ]
}
