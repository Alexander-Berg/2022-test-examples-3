import {Api} from 'core-legacy/types/restyped/generic/api'
import {placeId} from 'core-legacy/models/Places/__tests__/mocks'

export const activeOrders: Api['/4.0/restapp-front/api/v1/client/orders/active']['GET']['response'] = {
  isSuccess: true,
  payload: [
    {
      id: '352172bb-3de0-40c6-97c3-52aa431d7755',
      externalId: '201125-520773',
      type: 'native',
      comment: '',
      createdAt: '2020-11-25T18:08:38+03:00',
      restaurantConfirmedAt: '2020-12-17T16:51:26+03:00',
      endDate: '2020-11-25T18:38:16+03:00',
      sum: 1400,
      detailSum: {
        itemsCost: 1400,
        deliveryCost: 0
      },
      countPersons: 1,
      status: 'given',
      deliveryStatus: 'arrived_to_restaurant',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 1,
      restaurant: {
        id: placeId,
        name: 'Кофемания (Москва, Лесная улица, 5)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842984422,
          name: 'Домашние котлетки с картофельным пюре',
          quantity: 1,
          sum: 650,
          categoryId: 3378633,
          categoryName: 'Горячие блюда из птицы',
          weight: '300 г',
          options: [
            {
              id: 342966098,
              name: 'На пару',
              sum: 0,
              quantity: 1,
              groupId: 12465388,
              groupName: 'Способ приготовления',
              promoInfos: [],
              menuItemOptionId: 93948453
            },
            {
              id: 342966099,
              name: 'Черный',
              sum: 0,
              quantity: 1,
              groupId: 12465393,
              groupName: 'Хлеб',
              promoInfos: [],
              menuItemOptionId: 93948463
            }
          ],
          promoInfos: [],
          menuItemId: 37660948
        },
        {
          id: 842984423,
          name: 'Поке с лососем терияки',
          quantity: 1,
          sum: 750,
          categoryId: 3378598,
          categoryName: 'Рыбные горячие блюда',
          weight: '300 г',
          options: [
            {
              id: 342966100,
              name: 'Дикий рис',
              sum: 0,
              quantity: 1,
              groupId: 21342558,
              groupName: 'Гарнир',
              promoInfos: [],
              menuItemOptionId: 173675263
            }
          ],
          promoInfos: [],
          menuItemId: 145389612
        }
      ],
      courier: {
        name: 'Карл',
        phone: '',
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: null,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T00:10:17+03:00',
      onHold: false,
      changeId: null,
      costIncreaseAllowed: false
    },
    {
      id: '1d97c26c-9856-410c-9316-a662d91f9587',
      externalId: '201207-416844',
      type: 'native',
      comment: '',
      createdAt: '2020-12-07T13:57:31+03:00',
      restaurantConfirmedAt: '2020-12-17T16:42:42+03:00',
      endDate: '2020-12-07T14:11:10+03:00',
      sum: 4200,
      detailSum: {
        itemsCost: 4200,
        deliveryCost: 0
      },
      countPersons: 1,
      status: 'given',
      deliveryStatus: 'accepted',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 71,
      restaurant: {
        id: placeId,
        name: 'Кофемания (Москва, Лесная улица, 5)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842985267,
          name: 'Домашние котлетки с картофельным пюре',
          quantity: 3,
          sum: 650,
          categoryId: 3378633,
          categoryName: 'Горячие блюда из птицы',
          weight: '300 г',
          options: [
            {
              id: 342966487,
              name: 'На гриле',
              sum: 0,
              quantity: 1,
              groupId: 12465388,
              groupName: 'Способ приготовления',
              promoInfos: [],
              menuItemOptionId: 93948458
            },
            {
              id: 342966488,
              name: 'Черный',
              sum: 0,
              quantity: 1,
              groupId: 12465393,
              groupName: 'Хлеб',
              promoInfos: [],
              menuItemOptionId: 93948463
            }
          ],
          promoInfos: [],
          menuItemId: 37660948
        },
        {
          id: 842985268,
          name: 'Пельмени с лососем',
          quantity: 3,
          sum: 750,
          categoryId: 3378598,
          categoryName: 'Рыбные горячие блюда',
          weight: '250 г',
          options: [
            {
              id: 342966489,
              name: 'Растопленное сливочное масло с укропом',
              sum: 0,
              quantity: 1,
              groupId: 21142127,
              groupName: 'Масло на выбор',
              promoInfos: [],
              menuItemOptionId: 172484262
            }
          ],
          promoInfos: [],
          menuItemId: 145389602
        },
        {
          id: 842985269,
          name: 'Домашние котлетки с картофельным пюре',
          quantity: 1,
          sum: 0,
          categoryId: 3378633,
          categoryName: 'Горячие блюда из птицы',
          weight: '300 г',
          options: [
            {
              id: 342966490,
              name: 'На гриле',
              sum: 0,
              quantity: 1,
              groupId: 12465388,
              groupName: 'Способ приготовления',
              promoInfos: [],
              menuItemOptionId: 93948458
            }
          ],
          promoInfos: [
            {
              promo: {
                name: '1+1',
                originId: 1
              },
              discount: 650,
              discountType: 'gift',
              originalPrice: 1000
            }
          ],
          menuItemId: 37660948
        }
      ],
      courier: {
        name: 'Влас',
        phone: '',
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: null,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T00:10:06+03:00',
      onHold: false,
      changeId: 202,
      costIncreaseAllowed: false
    },
    {
      id: '69861587-31f3-4e3e-8dca-c4b3b782be7d',
      externalId: '210112-238943',
      type: 'native',
      comment: '',
      createdAt: '2021-01-12T17:45:51+03:00',
      restaurantConfirmedAt: '2021-01-12T17:46:12+03:00',
      endDate: '2021-01-12T17:55:30+03:00',
      sum: 650,
      detailSum: {
        itemsCost: 650,
        deliveryCost: 0
      },
      countPersons: 1,
      status: 'accepted',
      deliveryStatus: 'taken',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 74,
      restaurant: {
        id: placeId,
        name: 'Кофемания NEW (Москва, Лесная улица, 5)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842988124,
          name: 'Домашние котлетки с картофельным пюре',
          quantity: 1,
          sum: 650,
          categoryId: 3378633,
          categoryName: 'Горячие блюда из птицы',
          weight: '300 г',
          options: [
            {
              id: 342967752,
              name: 'На пару',
              sum: 0,
              quantity: 1,
              groupId: 12465388,
              groupName: 'Способ приготовления',
              promoInfos: [],
              menuItemOptionId: 93948453
            },
            {
              id: 342967753,
              name: 'Черный',
              sum: 0,
              quantity: 1,
              groupId: 12465393,
              groupName: 'Хлеб',
              promoInfos: [],
              menuItemOptionId: 93948463
            }
          ],
          promoInfos: [],
          menuItemId: 37660948
        }
      ],
      courier: {
        name: 'Самозанят',
        phone: '+79999999999',
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: true,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T01:10:04+03:00',
      onHold: false,
      changeId: null,
      costIncreaseAllowed: false
    },
    {
      id: '8fbfd3a8-561e-4836-8520-88de1b5ea0a9',
      externalId: '210125-244320',
      type: 'native',
      comment: '',
      createdAt: '2021-01-25T12:54:16+03:00',
      restaurantConfirmedAt: null,
      endDate: '2021-01-25T13:05:48+03:00',
      sum: 135,
      detailSum: {
        itemsCost: 135,
        deliveryCost: 0
      },
      countPersons: 1,
      status: 'new',
      deliveryStatus: 'taken',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 15,
      restaurant: {
        id: 46874,
        name: 'Макдоналдс (Москва, Варшавское шоссе, 87Б)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842988976,
          name: 'Биг Мак',
          quantity: 1,
          sum: 135,
          categoryId: 1053441,
          categoryName: 'Сандвичи, Роллы, Стартеры',
          weight: '210 г',
          options: [],
          promoInfos: [],
          menuItemId: 12948921
        }
      ],
      courier: {
        name: null,
        phone: null,
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: null,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T12:54:16+03:00',
      onHold: false,
      changeId: null,
      costIncreaseAllowed: false
    }
  ],
  meta: {
    count: 4
  }
}

export const search: Api['/4.0/restapp-front/api/v1/client/orders/search']['POST']['response'] = {
  isSuccess: true,
  payload: [
    {
      id: 'da6643a4-7820-4ab6-a1bc-84562876a033',
      externalId: '210125-211568',
      type: 'native',
      comment: '',
      createdAt: '2021-01-25T12:32:45+03:00',
      restaurantConfirmedAt: '2021-01-25T12:44:01+03:00',
      endDate: '2021-01-25T12:37:15+03:00',
      sum: 780,
      detailSum: {
        itemsCost: 780,
        deliveryCost: 0
      },
      countPersons: 0,
      status: 'released',
      deliveryStatus: 'taken',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 64,
      restaurant: {
        id: placeId,
        name: 'Кофемания NEW (Москва, Лесная улица, 5)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842988975,
          name: 'Суп с фрикадельками',
          quantity: 2,
          sum: 390,
          categoryId: 3378613,
          categoryName: 'Детское меню',
          weight: '220 г',
          options: [],
          promoInfos: [],
          menuItemId: 37660653
        }
      ],
      courier: {
        name: 'Юлий',
        phone: '+79942220019,2141',
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: true,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T12:52:34+03:00',
      onHold: false,
      changeId: null,
      costIncreaseAllowed: false
    },
    {
      id: '8fbfd3a8-561e-4836-8520-88de1b5ea0a9',
      externalId: '210125-244320',
      type: 'native',
      comment: '',
      createdAt: '2021-01-25T12:54:16+03:00',
      restaurantConfirmedAt: null,
      endDate: '2021-01-25T13:05:48+03:00',
      sum: 135,
      detailSum: {
        itemsCost: 135,
        deliveryCost: 0
      },
      countPersons: 1,
      status: 'new',
      deliveryStatus: 'taken',
      paymentType: 'cashless',
      changeOn: null,
      timeToDelivery: 15,
      restaurant: {
        id: 46874,
        name: 'Макдоналдс (Москва, Варшавское шоссе, 87Б)'
      },
      user: {
        name: 'Yandex.Eda',
        phoneNumber: '88006001210',
        address: null,
        addressComment: null,
        extendedAddress: {
          city: null,
          street: null,
          house: null,
          entrance: null,
          floor: null,
          office: null,
          plot: null,
          building: null,
          doorcode: null,
          comment: null
        }
      },
      items: [
        {
          id: 842988976,
          name: 'Биг Мак',
          quantity: 1,
          sum: 135,
          categoryId: 1053441,
          categoryName: 'Сандвичи, Роллы, Стартеры',
          weight: '210 г',
          options: [],
          promoInfos: [],
          menuItemId: 12948921
        }
      ],
      courier: {
        name: null,
        phone: null,
        comment: null,
        approximateArrivalTime: null,
        metaInfo: {
          isRover: false,
          isHardOfHearing: false
        }
      },
      promoInfos: [],
      totalDiscount: 0,
      isReimbursementToPlace: null,
      currency: {
        code: 'RUB',
        sign: '₽',
        decimalPlaces: 0
      },
      changeInitializedAt: null,
      lastUpdatedAt: '2021-01-25T12:54:16+03:00',
      onHold: false,
      changeId: null,
      costIncreaseAllowed: false
    }
  ],
  meta: {
    count: 2
  }
}

export const singleOrder: Api['/4.0/restapp-front/api/v1/client/orders/:id']['GET']['response'] = {
  isSuccess: true,
  payload: {
    id: '1d97c26c-9856-410c-9316-a662d91f9587',
    externalId: '201207-416844',
    type: 'native',
    comment: '',
    createdAt: '2020-12-07T13:57:31+03:00',
    restaurantConfirmedAt: '2020-12-17T16:42:42+03:00',
    endDate: '2020-12-07T14:11:10+03:00',
    sum: 4200,
    detailSum: {
      itemsCost: 4200,
      deliveryCost: 0
    },
    countPersons: 1,
    status: 'given',
    deliveryStatus: 'accepted',
    paymentType: 'cashless',
    changeOn: null,
    timeToDelivery: 71,
    restaurant: {
      id: placeId,
      name: 'Кофемания (Москва, Лесная улица, 5)'
    },
    user: {
      name: 'Yandex.Eda',
      phoneNumber: '88006001210',
      address: null,
      addressComment: null,
      extendedAddress: {
        city: null,
        street: null,
        house: null,
        entrance: null,
        floor: null,
        office: null,
        plot: null,
        building: null,
        doorcode: null,
        comment: null
      }
    },
    items: [
      {
        id: 842985267,
        name: 'Домашние котлетки с картофельным пюре',
        quantity: 3,
        sum: 650,
        categoryId: 3378633,
        categoryName: 'Горячие блюда из птицы',
        weight: '300 г',
        options: [
          {
            id: 342966487,
            name: 'На гриле',
            sum: 0,
            quantity: 1,
            groupId: 12465388,
            groupName: 'Способ приготовления',
            promoInfos: [],
            menuItemOptionId: 93948458
          },
          {
            id: 342966488,
            name: 'Черный',
            sum: 0,
            quantity: 1,
            groupId: 12465393,
            groupName: 'Хлеб',
            promoInfos: [],
            menuItemOptionId: 93948463
          }
        ],
        promoInfos: [],
        menuItemId: 37660948
      },
      {
        id: 842985268,
        name: 'Пельмени с лососем',
        quantity: 3,
        sum: 750,
        categoryId: 3378598,
        categoryName: 'Рыбные горячие блюда',
        weight: '250 г',
        options: [
          {
            id: 342966489,
            name: 'Растопленное сливочное масло с укропом',
            sum: 0,
            quantity: 1,
            groupId: 21142127,
            groupName: 'Масло на выбор',
            promoInfos: [],
            menuItemOptionId: 172484262
          }
        ],
        promoInfos: [],
        menuItemId: 145389602
      },
      {
        id: 842985269,
        name: 'Домашние котлетки с картофельным пюре',
        quantity: 1,
        sum: 0,
        categoryId: 3378633,
        categoryName: 'Горячие блюда из птицы',
        weight: '300 г',
        options: [
          {
            id: 342966490,
            name: 'На гриле',
            sum: 0,
            quantity: 1,
            groupId: 12465388,
            groupName: 'Способ приготовления',
            promoInfos: [],
            menuItemOptionId: 93948458
          }
        ],
        promoInfos: [],
        menuItemId: 37660948
      }
    ],
    courier: {
      name: 'Влас',
      phone: '',
      comment: null,
      approximateArrivalTime: null,
      metaInfo: {
        isRover: false,
        isHardOfHearing: false
      }
    },
    promoInfos: [],
    totalDiscount: 0,
    isReimbursementToPlace: null,
    currency: {
      code: 'RUB',
      sign: '₽',
      decimalPlaces: 0
    },
    changeInitializedAt: null,
    lastUpdatedAt: '2021-03-21T22:10:10+03:00',
    onHold: false,
    changeId: 202,
    costIncreaseAllowed: false
  }
}
