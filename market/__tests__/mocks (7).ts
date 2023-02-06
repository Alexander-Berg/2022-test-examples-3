import {Configs} from 'core-legacy/types/experiments'
import {ShopType} from 'gm-shops/models/Shops/Shop'

export const shopEditConfigMock: Configs['restapp_shop_edit'] = {
  address_editable: true,
  enabled: true,
  name_editable: true
}

export const shopMock: ShopType = {
  selfName: 'НанаНуна',
  selfAddress: 'Москва, улица Петровка, 20/1',
  info: {
    info: {
      email: 'valerij68@gulaeva.info',
      lprEmail: 'tochka35@yandex.ru',
      phones: [{number: '+79999999998', type: 'auto_call', description: null}],
      payments: ['Наличный расчет', 'Безналичный расчет'],
      addressComment: 'Вход через ул. Петровские линии 20/1',
      clientComment: 'Въезд с улицы Петровка на улицу Петровские Линии.'
    }
  }
} as ShopType
