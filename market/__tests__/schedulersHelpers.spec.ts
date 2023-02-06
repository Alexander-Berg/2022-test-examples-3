import { PROVIDE_RECEIPT_DATA_OPERATION_NAME } from '@/constants/operations';

import { isTaskWithPayment } from '../schedulers/helpers';

describe('Хелперы для оффлайна', () => {
  it('isTaskWithPayment на таску PROVIDE_RECEIPT_DATA_OPERATION_NAME с paymentType === CARD возвращает true', () => {
    expect(
      isTaskWithPayment({
        taskId: '123',
        variables: { paymentType: 'CARD' },
        operationName: PROVIDE_RECEIPT_DATA_OPERATION_NAME,
        orderId: 1,
        updateAt: 'updateAt',
        lastFailure: 'lastFailure',
      }),
    ).toBe(true);
  });

  it('isTaskWithPayment на неправильную таску с paymentType === CARD возвращает false', () => {
    expect(
      isTaskWithPayment({
        taskId: '123',
        variables: { paymentType: 'CARD' },
        operationName: 'WRONG_TASK_123',
        orderId: 1,
        updateAt: 'updateAt',
        lastFailure: 'lastFailure',
      }),
    ).toBe(false);
  });
});
