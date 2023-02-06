import { cleanup, render, screen } from '@testing-library/react/pure';
import React, { FC } from 'react';
import { fireEvent } from '@testing-library/react';
import { Grid } from '@crm/components/dist/Attribute2/components/Grid';
import { CheckboxAttribute } from './Checkbox';

const onChange = jest.fn();

export const CheckboxAttributeWrapper: FC<{ value?: boolean; label?: string }> = ({
  value,
  label,
}) => {
  return (
    <Grid>
      <CheckboxAttribute value={value} onChange={onChange} label={label} />
    </Grid>
  );
};

describe('Checkbox', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  describe('props.label', () => {
    describe('when defined', () => {
      it('renders checkbox with correct label', () => {
        render(<CheckboxAttributeWrapper label={'Чекбокс'} />);

        const label = screen.getByTestId('attribute-checkbox-label');

        expect(label).toBeVisible();
        expect(label).toHaveTextContent('Чекбокс');
      });
    });
  });

  describe('props.value', () => {
    describe('when false', () => {
      it('renders unchecked checkbox', () => {
        render(<CheckboxAttributeWrapper value={false} />);

        expect(screen.getByTestId('attribute-checkbox')).not.toBeChecked();
      });
    });

    describe('when true', () => {
      it('renders checked checkbox', () => {
        render(<CheckboxAttributeWrapper value />);

        expect(screen.getByTestId('attribute-checkbox')).toBeChecked();
      });
    });
  });

  describe('props.onChange', () => {
    describe('when defined', () => {
      it('is called when checkbox value changes', async () => {
        render(<CheckboxAttributeWrapper />);

        fireEvent.click(screen.getByTestId('attribute-checkbox'));

        expect(onChange).toBeCalledTimes(1);
        expect(onChange).toBeCalledWith(true);
      });
    });
  });
});
