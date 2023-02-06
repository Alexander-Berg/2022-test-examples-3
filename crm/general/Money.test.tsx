import React from 'react';
import { render, screen, act, fireEvent } from '@testing-library/react';
import { Money } from './Money';

const currency = [
  {
    code: 'RUB',
    id: 0,
    name: 'руб.',
  },
  {
    code: 'TNG',
    id: 1,
    name: 'тенге',
  },
  {
    code: 'USD',
    id: 2,
    name: 'дол.',
  },
];

describe('Money', () => {
  describe('display value', () => {
    it('renders empty value', () => {
      render(<Money currency={currency} />);

      expect(screen.queryByDisplayValue('')).toBeInTheDocument();
      expect(screen.queryByText('—')).toBeInTheDocument();
    });

    it('renders value', () => {
      render(<Money value={{ value: 100, currency: currency[0] }} currency={currency} />);

      expect(screen.queryByDisplayValue(100)).toBeInTheDocument();
      expect(screen.queryByText('руб.')).toBeInTheDocument();
    });
  });

  describe('handle change', () => {
    const handleChange = jest.fn();

    afterEach(() => {
      handleChange.mockClear();
    });

    describe('empty value', () => {
      it('dispatches on textinput change', () => {
        render(<Money currency={currency} onChange={handleChange} />);

        act(() => {
          fireEvent.change(screen.getByDisplayValue(''), { target: { value: '100' } });
        });

        expect(handleChange).toBeCalledTimes(1);
        expect(handleChange).toBeCalledWith({ value: 100 });
      });

      /* TODO: хз почему этот тест выбирает рубли, а не доллар.*/
      // eslint-disable-next-line mocha/no-skipped-tests
      it.skip('dispatches on select change', () => {
        render(<Money currency={currency} onChange={handleChange} />);

        act(() => {
          fireEvent.click(screen.getByText('—'));
        });

        act(() => {
          fireEvent.click(screen.getByText('дол.'));
        });

        expect(handleChange).toBeCalledTimes(1);
        expect(handleChange).toBeCalledWith({ currency: currency[2] });
      });
    });

    describe('has value', () => {
      it('dispatches on textinput change', () => {
        render(
          <Money
            value={{ value: 100, currency: currency[0] }}
            currency={currency}
            onChange={handleChange}
          />,
        );

        act(() => {
          fireEvent.change(screen.getByDisplayValue('100'), { target: { value: '200' } });
        });

        expect(handleChange).toBeCalledTimes(1);
        expect(handleChange).toBeCalledWith({ value: 200, currency: currency[0] });
      });
    });
  });
});
