import { useControlled } from 'utils/hooks/useControlled';
import { render } from '@testing-library/react/pure';
import React from 'react';

const TestComponent = (props) => {
  const [value, setValue] = useControlled({
    value: props.value,
    defaultValue: props.defaultValue,
  });

  return props.children(value, setValue);
};

describe('hooks/useControlled', () => {
  describe('when is not controlled', () => {
    it('works correctly', () => {
      let valueNotControlled;
      let setValueNotControlled;

      render(
        <TestComponent defaultValue={1}>
          {(value, setValue) => {
            // save link to outer variable to have access
            valueNotControlled = value;
            setValueNotControlled = setValue;

            return null;
          }}
        </TestComponent>,
      );

      expect(valueNotControlled).toEqual(1);

      setValueNotControlled(2);

      expect(valueNotControlled).toEqual(2);
    });
  });
  describe('when is controlled', () => {
    it('works correctly', () => {
      let valueControlled;
      let setValueControlled;

      render(
        <TestComponent value={1}>
          {(value, setValue) => {
            // save link to outer variable to have access
            valueControlled = value;
            setValueControlled = setValue;

            return null;
          }}
        </TestComponent>,
      );

      expect(valueControlled).toEqual(1);

      setValueControlled(2); // does nothing

      expect(valueControlled).toEqual(1);
    });
  });
});
