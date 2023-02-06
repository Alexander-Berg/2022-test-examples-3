import React, { useEffect } from 'react';
import { act, create, ReactTestRenderer } from 'react-test-renderer';
import { renderHook } from 'neo/tests/renderHook';
import { EFlagType, createUseFlag } from 'neo/hooks/useFlag';
import { IApplicationCtx } from 'neo/types/contexts';

jest.mock('neo/hooks/contexts/useApplicationCtx', () => ({
  useApplicationCtx: (): IApplicationCtx => ({
    neo: {
      generateId: (): string => '',
      flags: {
        'yxneo_boolean-test': '0',
        'yxneo_string-test': 'string test',
        'yxneo_number-test': '42',
        'yxneo_string-array-test': 'string1,string2,string3',
        'yxneo_number-array-test': '1,42,3',
      },
    },
  }),
}));

type TFlagsDeclaration = [
  [
    'yxneo_boolean-test',
    {
      description: 'используется в тестах useFlag для проверки boolean-значений',
      values: '0|1',
      task: '',
      type: boolean,
    },
  ],
  [
    'yxneo_string-test',
    {
      description: 'используется в тестах useFlag для проверки string-значений',
      values: 'string',
      task: '',
      type: string,
    },
  ],
  [
    'yxneo_number-test',
    {
      description: 'используется в тестах useFlag для проверки number-значений',
      values: 'number',
      task: '',
      type: number,
    },
  ],
  [
    'yxneo_string-array-test',
    {
      description: 'используется в тестах useFlag для проверки string[]-значений',
      values: 'string[]',
      task: '',
      type: string[],
    },
  ],
  [
    'yxneo_number-array-test',
    {
      description: 'используется в тестах useFlag для проверки number[]-значений',
      values: 'number[]',
      task: '',
      type: number[],
    },
  ],
];

const useFlag = createUseFlag<TFlagsDeclaration>();

describe('useFlag', () => {
  it('должно вернуть значение флага', () => {
    const booleanValue = renderHook(() => useFlag('yxneo_boolean-test', EFlagType.BOOL))();
    const stringValue = renderHook(() => useFlag('yxneo_string-test'))();
    const numberValue = renderHook(() => useFlag('yxneo_number-test', EFlagType.NUMBER))();
    const arrayStringValue = renderHook(() => useFlag('yxneo_string-array-test', EFlagType.STRING_ARRAY))();
    const arrayNumberValue = renderHook(() => useFlag('yxneo_number-array-test', EFlagType.NUMBER_ARRAY))();
    const arrayOptionalStringValue = renderHook(() => useFlag('yxneo_string-array-test', EFlagType.STRING_OPTIONAL_ARRAY))();
    const arrayOptionalNumberValue = renderHook(() => useFlag('yxneo_number-array-test', EFlagType.NUMBER_OPTIONAL_ARRAY))();

    expect(booleanValue).toBe(false);
    expect(stringValue).toBe('string test');
    expect(numberValue).toBe(42);
    expect(arrayStringValue).toEqual(['string1', 'string2', 'string3']);
    expect(arrayNumberValue).toEqual([1, 42, 3]);
    expect(arrayOptionalStringValue).toEqual(['string1', 'string2', 'string3']);
    expect(arrayOptionalNumberValue).toEqual([1, 42, 3]);
  });

  it('должно возвращать мемоизированное значение для массивов', () => {
    const TestComponent: React.FC<{
      callback: (value: string[]) => void
    }> = (props) => {
      const value = useFlag('yxneo_string-array-test', EFlagType.STRING_ARRAY);

      useEffect(() => {
        props.callback(value);
      });

      return <div>{value[0]}</div>;
    };

    const values: Array<string[]> = [];

    function addToValues(value: string[]) {
      values.push(value);
    }

    let element: ReactTestRenderer;
    act(() => {
      element = create(<TestComponent callback={addToValues} />);
    });
    act(() => {
      element.update(<TestComponent callback={addToValues} />);
    });

    expect(values[0]).toBe(values[1]);
  });
});
