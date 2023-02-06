import { getCallInfo, updateCallInfo } from '@/nativeModules/storage/calls';

jest.mock('@react-native-community/async-storage');

const testUid = 'testCalls';
const testShiftId = 'testShiftId';
const testOrderId1 = 'testOrderId1';
const testOrderId2 = 'testOrderId2';

describe('Тестирование информации о звонках по заказам', () => {
  it('Нет информации о звонках', async () => {
    const callInfo = await getCallInfo(testUid, testShiftId, testOrderId1);

    expect(callInfo).toBeNull();
  });

  it('Информация о звонках обновляется', async () => {
    let result1 = await updateCallInfo(testUid, testShiftId, testOrderId1);
    let result2 = await updateCallInfo(testUid, testShiftId, testOrderId2);

    expect(result1).toBeTruthy();
    expect(result2).toBeTruthy();
    expect(result1).toEqual({
      callsCount: 1,
      orderId: testOrderId1,
      __typename: 'CallInfo',
    });
    expect(result2).toEqual({
      callsCount: 1,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });

    result1 = await getCallInfo(testUid, testShiftId, testOrderId1);
    result2 = await getCallInfo(testUid, testShiftId, testOrderId2);

    expect(result1).toBeTruthy();
    expect(result2).toBeTruthy();
    expect(result1).toEqual({
      callsCount: 1,
      orderId: testOrderId1,
      __typename: 'CallInfo',
    });
    expect(result2).toEqual({
      callsCount: 1,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });

    result1 = await updateCallInfo(testUid, testShiftId, testOrderId1);
    result2 = await updateCallInfo(testUid, testShiftId, testOrderId2);

    expect(result1).toBeTruthy();
    expect(result2).toBeTruthy();
    expect(result1).toEqual({
      callsCount: 2,
      orderId: testOrderId1,
      __typename: 'CallInfo',
    });
    expect(result2).toEqual({
      callsCount: 2,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });

    result1 = await getCallInfo(testUid, testShiftId, testOrderId1);
    result2 = await getCallInfo(testUid, testShiftId, testOrderId2);

    expect(result1).toBeTruthy();
    expect(result2).toBeTruthy();
    expect(result1).toEqual({
      callsCount: 2,
      orderId: testOrderId1,
      __typename: 'CallInfo',
    });
    expect(result2).toEqual({
      callsCount: 2,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });

    result2 = await updateCallInfo(testUid, testShiftId, testOrderId2);

    expect(result2).toBeTruthy();
    expect(result2).toEqual({
      callsCount: 3,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });

    result2 = await getCallInfo(testUid, testShiftId, testOrderId2);

    expect(result2).toBeTruthy();
    expect(result2).toEqual({
      callsCount: 3,
      orderId: testOrderId2,
      __typename: 'CallInfo',
    });
  });
});
