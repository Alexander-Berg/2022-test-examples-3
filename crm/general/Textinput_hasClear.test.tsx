import React, { useState } from 'react';
import { render, screen, act } from '@testing-library/react';
import { Textinput } from '@yandex-lego/components/Textinput/desktop/bundle';
import userEvent from '@testing-library/user-event';
import { ITextinputProps, ITextinputHasClearProps } from '@yandex-lego/components/Textinput';
import { withHasClear } from './Textinput_hasClear';

const TextinputWithClear = withHasClear(Textinput);
const TestComponent = ({ onChange, ...props }: ITextinputProps & ITextinputHasClearProps) => {
  const [value, setValue] = useState();

  const handleChange = (event) => {
    onChange?.(event);
    setValue(event.target.value);
  };

  return <TextinputWithClear {...props} value={value} onChange={handleChange} />;
};

describe('Textinput_hasClear', () => {
  describe('props.hasClear', () => {
    describe('when true', () => {
      it('renders clear button instead of iconRight', () => {
        const { rerender, container } = render(
          <TestComponent hasClear iconRight={<div>iconRight</div>} />,
        );
        expect(screen.getByText('iconRight')).toBeInTheDocument();
        userEvent.type(screen.getByRole('textbox'), 'test');

        rerender(<TestComponent hasClear />);

        expect(screen.queryByText('iconRight')).not.toBeInTheDocument();
        expect(container.getElementsByClassName('Icon')[0]).toBeInTheDocument();
      });

      it('clears text on click', async () => {
        const onChange = jest.fn();
        const { rerender, container } = render(<TestComponent hasClear onChange={onChange} />);

        userEvent.type(screen.getByRole('textbox'), 'test');

        rerender(<TestComponent hasClear onChange={onChange} />);

        expect(screen.getByRole('textbox')).toHaveValue('test');

        const clearIcon = container.getElementsByClassName('Icon')[0];
        act(() => {
          userEvent.click(clearIcon);
        });

        expect(screen.getByRole('textbox')).toHaveValue('');
        expect(clearIcon).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onChange', () => {
    describe('when defined', () => {
      it('calls on clear button click', () => {
        const onChange = jest.fn();
        const { rerender, container } = render(<TestComponent hasClear onChange={onChange} />);

        userEvent.type(screen.getByRole('textbox'), 'test');

        rerender(<TestComponent hasClear onChange={onChange} />);

        expect(screen.getByRole('textbox')).toHaveValue('test');

        act(() => {
          userEvent.click(container.getElementsByClassName('Icon')[0]);
        });
        expect(onChange).toBeCalled();
      });
    });
  });

  describe('props.onClearClick', () => {
    describe('when defined', () => {
      it('calls on clear button click', () => {
        const onClearClick = jest.fn();
        const { rerender, container } = render(
          <TestComponent onClearClick={onClearClick} hasClear />,
        );

        userEvent.type(screen.getByRole('textbox'), 'test');

        rerender(<TestComponent onClearClick={onClearClick} hasClear />);

        act(() => {
          userEvent.click(container.getElementsByClassName('Icon')[0]);
        });

        expect(onClearClick).toBeCalled();
      });
    });
  });
});
