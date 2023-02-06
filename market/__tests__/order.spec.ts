import { TaskStatus } from '@/apollo/generated/graphql';
import { TAG_RED } from '@/constants/colors';
import {
  PROVIDE_RECEIPT_DATA_OPERATION_NAME,
  PROVIDE_RETURNED_RECEIPT_DATA_OPERATION_NAME,
} from '@/constants/operations';

import { OrderTagType, getOrderAdditionalTags } from '../order';

jest.mock('@/nativeModules/crashlytics', () => ({}));

describe('utils/order/getOrderAdditionalTags', () => {
  const hasReturnStatuses = [true, false];
  const operationTypes = [
    undefined,
    PROVIDE_RECEIPT_DATA_OPERATION_NAME,
    PROVIDE_RETURNED_RECEIPT_DATA_OPERATION_NAME,
  ];

  it('Должна вернуть пустой массив, если передан TaskStatus.DeliveryFailed и любая комбинация других параметров', () => {
    for (const hasReturn of hasReturnStatuses) {
      for (const operationType of operationTypes) {
        expect(
          getOrderAdditionalTags({
            status: TaskStatus.DeliveryFailed,
            hasReturn,
            operationType,
          }),
        ).toEqual([]);
      }
    }
  });

  it('Должна вернуть пустой массив, если передан TaskStatus.Delivered и любая комбинация других параметров', () => {
    for (const hasReturn of hasReturnStatuses) {
      for (const operationType of operationTypes) {
        expect(
          getOrderAdditionalTags({
            status: TaskStatus.Delivered,
            hasReturn,
            operationType,
          }),
        ).toEqual([]);
      }
    }
  });

  it('Должна вернуть текст и цвет блока с информацией о возврате, если параметр hasReturn передан со значением true', () => {
    expect(
      getOrderAdditionalTags({
        status: TaskStatus.NotDelivered,
        hasReturn: true,
      }),
    ).toContainEqual({
      text: 'Возврат',
      type: OrderTagType.RETURN,
      color: TAG_RED,
    });
  });

  it('Должна вернуть текст и цвет блока с информацией об ошибке соединения, если передан параметр operationType', () => {
    expect(
      getOrderAdditionalTags({
        status: TaskStatus.NotDelivered,
        hasReturn: false,
        operationType: PROVIDE_RECEIPT_DATA_OPERATION_NAME,
      }),
    ).toContainEqual({
      text: 'Без интернета',
      type: OrderTagType.SALE_OFFLINE,
      color: TAG_RED,
    });

    expect(
      getOrderAdditionalTags({
        status: TaskStatus.NotDelivered,
        hasReturn: false,
        operationType: PROVIDE_RETURNED_RECEIPT_DATA_OPERATION_NAME,
      }),
    ).toContainEqual({
      text: 'Возврат без интернета',
      type: OrderTagType.RETURN_OFFLINE,
      color: TAG_RED,
    });
  });
});
