import { OperationStatusType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { getImageValidationMessage } from './getImageValidationMessage';

describe('getImageValidationMessage', () => {
  it('works with invalid data', () => {
    expect(getImageValidationMessage()).toEqual(undefined);
    expect(getImageValidationMessage({})).toEqual(undefined);
    expect(getImageValidationMessage({ status: {} as any })).toEqual(undefined);
    expect(getImageValidationMessage({ status: { status: OperationStatusType.INTERNAL_ERROR } as any })).toEqual(
      undefined
    );
  });
  it('works with VALIDATION_ERROR without valid validation_error', () => {
    expect(getImageValidationMessage({ status: { status: OperationStatusType.VALIDATION_ERROR } as any })).toEqual(
      undefined
    );
    expect(
      getImageValidationMessage({
        status: { status: OperationStatusType.VALIDATION_ERROR, validation_error: [] } as any,
      })
    ).toEqual(undefined);
    expect(
      getImageValidationMessage({
        status: { status: OperationStatusType.VALIDATION_ERROR, validation_error: [{ named_param: [] }] } as any,
      })
    ).toEqual(undefined);
    expect(
      getImageValidationMessage({
        status: { status: OperationStatusType.VALIDATION_ERROR, validation_error: [{ named_param: [{}] }] } as any,
      })
    ).toEqual(undefined);
    expect(
      getImageValidationMessage({
        status: {
          status: OperationStatusType.VALIDATION_ERROR,
          validation_error: [{ named_param: [{ name: 'test' }] }],
        } as any,
      })
    ).toEqual(undefined);
    expect(
      getImageValidationMessage({
        status: {
          status: OperationStatusType.VALIDATION_ERROR,
          validation_error: [{ named_param: [{ name: 'CAUSE' }] }],
        } as any,
      })
    ).toEqual(undefined);
  });

  it('works with VALIDATION_ERROR with valid validation_error', () => {
    expect(
      getImageValidationMessage({
        status: {
          status: OperationStatusType.VALIDATION_ERROR,
          validation_error: [{ named_param: [{ name: 'CAUSE' }] }],
        } as any,
      })
    ).toEqual(undefined);
    expect(
      getImageValidationMessage({
        status: {
          status: OperationStatusType.VALIDATION_ERROR,
          validation_error: [{ named_param: [{ name: 'CAUSE', value: 'testValue' }] }],
        } as any,
      })
    ).toEqual('testValue');
  });
});
