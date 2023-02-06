import ApolloClient from 'apollo-client';

import {
  Delivery,
  Order,
  OrderPaymentStatus,
  OrderPaymentType,
  VatType,
} from '@/apollo/generated/graphql';

export const testDelivery: Delivery = {
  address: '',
  intervalTo: '',
  intervalFrom: '',
  recipientFio: '',
  realRecipientPhone: '',
  longitude: 0,
  latitude: 0,
  addressDetails: {},
  expectedTime: '',
};

export const testDelivery1: Delivery = {
  ...testDelivery,
  realRecipientPhone: '+123456789',
};

export const testDelivery2: Delivery = {
  ...testDelivery,
  realRecipientPhone: '+987654321',
  recipientEmail: 'test@test.com',
};

export const testDelivery3: Delivery = {
  ...testDelivery,
  recipientEmail: 'test@test.com',
};

export const testOrder1: Order = {
  externalOrderId: '123',
  paymentType: OrderPaymentType.Card,
  paymentStatus: OrderPaymentStatus.Paid,
  orderPrice: 0,
  deliveryPrice: 0,
  totalPrice: 0,
  hasReturn: false,
  items: [
    {
      name: 'testItem1',
      price: 10,
      count: 1,
      service: false,
      sumPrice: 10,
      vatType: VatType.Vat_20,
    },
    {
      name: 'testItem2',
      price: 20,
      count: 2,
      service: false,
      sumPrice: 40,
      vatType: VatType.Vat_20,
    },
    {
      name: 'testDelivery',
      price: 50,
      count: 1,
      service: true,
      sumPrice: 50,
      vatType: VatType.Vat_20,
    },
    {
      name: 'testItem3',
      price: 20,
      count: 10,
      service: false,
      sumPrice: 200,
      vatType: VatType.Vat_20,
    },
  ],
  delivery: testDelivery1,
  cheques: [],
  places: [
    {
      barcode: '45',
    },
  ],
  ordinalNumber: 1,
};

export const testOrder2 = {
  ...testOrder1,
  delivery: testDelivery2,
};

export const testOrder3 = {
  ...testOrder1,
  delivery: testDelivery3,
};

export const schedulerTestShiftId = 'schedulerTest';
export const operationsTestShiftId = 'operationsTest';
export const schedulerHookTestShiftId = 'schedulerHookTestShiftId';

export const clientWithSuccessMutations = {
  mutate: async (_args: any): Promise<any> => {
    return new Promise((resolve, _reject) => {
      resolve(undefined);
    });
  },
} as ApolloClient<object>;

export const clientWithErrorMutations = {
  mutate: async (_args: any): Promise<any> => {
    return new Promise((_resolve, reject) => {
      reject();
    });
  },
} as ApolloClient<object>;

let queryCount = 0;
const failQueries = 6;

export const client = {
  mutate: async (_args: any): Promise<any> => {
    return new Promise((resolve, reject) => {
      queryCount += 1;

      if (queryCount <= failQueries) {
        reject();
      } else {
        resolve(undefined);
      }
    });
  },
} as ApolloClient<object>;

export const testAsyncCallback = (_args: any): Promise<void> =>
  Promise.resolve();

export const testAsyncCallbackWithoutParameters = (): Promise<void> =>
  Promise.resolve();
