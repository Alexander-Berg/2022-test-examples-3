import { statuses } from '../../../constants/index';

const templateBill = {
  active: true,
  caption: 'Тест',
  closed: null,
  created: '2018-09-27T14:28:39.954329+00:00',
  date: '2018-09-27T14:28:39.954329+00:00',
  items: [
    {
      amount: 1,
      currency: 'RUB',
      name: 'А',
      nds: 'nds_18',
      price: 111,
      product_id: 69
    },
    {
      amount: 1,
      currency: 'RUB',
      name: 'Б',
      nds: 'nds_18',
      price: 111,
      product_id: 70
    }
  ],
  name: 'Тест',
  orderUrl:
    'http://local.yandex.ru:3000/transaction/gAAAAABbrk1ic2Q1tP6MZEA_QliG2aODbj5sORZck4qUXxu8a4WWkIJRdDHHj0gzT5A9-fSDEK6a6BmIk52iHSewwVOK3H0e5Sxz9IcfsQoDjiSVGB14opbJ3LYiptpZGlWBkJxP4QoB5wavI_pDrXd5Pd0AM8x1…',
  price: 222,
  uid: 4014115522,
  userEmail: 'test@test.test',
  userDescription: 'test'
};

export default {
  PAY: Object.keys(statuses.PAY).reduce(
    (acc, status, i) => ({
      ...acc,
      [status]: {
        ...templateBill,
        id: (i + 1) * 2,
        type: 'PAY',
        status
      }
    }),
    {}
  ),
  REFUND: Object.keys(statuses.REFUND).reduce(
    (acc, status, i) => ({
      ...acc,
      [status]: {
        ...templateBill,
        id: (i + 1) * 2 - 1,
        type: 'REFUND',
        status
      }
    }),
    {}
  )
};
