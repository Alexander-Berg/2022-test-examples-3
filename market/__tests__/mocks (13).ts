import {BillingType} from 'core-legacy/models/Places/PlaceInfo'

export const billingMock: BillingType = {
  account: '40702810500000143829',
  accountancyEmail: 'tochka35@yandex.ru',
  accountancyPhone: {number: '+74956252892', type: 'accountancy', description: ''},
  address: {postcode: '103051', full: 'Москва г, Петровка ул, дом № 20/1'},
  balanceDateStart: '2019-07-01',
  balanceExternalId: '180406-32',
  bik: '044525411',
  inn: '7707201970',
  kpp: '770701001',
  name: 'ООО "ПАРНАС ТРЕЙД"',
  postAddress: {postcode: '103051', full: 'Москва г, Петровка ул, дом № 20/1'},
  signer: {
    authorityDetails: null,
    authorityDoc: 'Устав',
    name: 'Пазылова Жылдызкан Тойгонбаевна',
    position: 'Генеральный директор'
  }
}
