import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent, { specialChars } from '@testing-library/user-event';
import { Textinput } from './Textinput';

describe('ReadEdit/Textinput', () => {
  const requiredProps = {
    label: '',
    access: 3,
    onEditingStart: jest.fn(),
    onEditingStop: jest.fn(),
  };

  describe('props.label', () => {
    it('renders label', () => {
      render(<Textinput {...requiredProps} label="test label" />);

      expect(screen.getByText('test label')).toBeInTheDocument();
    });
  });

  describe('props.access', () => {
    describe('when has edit bit', () => {
      it('enables editing', () => {
        const handleEditingStart = jest.fn();
        render(
          <Textinput
            {...requiredProps}
            label="test"
            onEditingStart={handleEditingStart}
            access={2}
          />,
        );

        userEvent.click(screen.getByText('test'));

        expect(handleEditingStart).toBeCalledTimes(1);
      });
    });

    describe(`when doesn't have edit bit`, () => {
      it('disables editing', () => {
        const handleEditingStart = jest.fn();
        render(
          <Textinput
            {...requiredProps}
            label="test"
            onEditingStart={handleEditingStart}
            access={1}
          />,
        );

        userEvent.click(screen.getByText('test'));

        expect(handleEditingStart).not.toBeCalled();
      });
    });
  });

  describe('props.value', () => {
    describe('when 0 number', () => {
      it('renders value', () => {
        render(<Textinput {...requiredProps} value={0} />);

        expect(screen.getByText('0')).toBeInTheDocument();
      });
    });

    describe('when empty string', () => {
      it('renders dash', () => {
        render(<Textinput {...requiredProps} value="" />);

        expect(screen.getByText('—')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it('renders dash', () => {
        render(<Textinput {...requiredProps} value={undefined} />);

        expect(screen.getByText('—')).toBeInTheDocument();
      });
    });

    describe('when defined', () => {
      it('renders value', () => {
        render(<Textinput {...requiredProps} value="test value" />);

        expect(screen.getByText('test value')).toBeInTheDocument();
      });
    });

    describe('when changes', () => {
      it('resets text by value', () => {
        const { rerender } = render(<Textinput {...requiredProps} isEditing value="test value" />);
        rerender(<Textinput {...requiredProps} isEditing value="test" />);

        expect(screen.getByDisplayValue('test')).toBeInTheDocument();
      });
    });
  });

  describe('props.onChange', () => {
    describe('on outside click', () => {
      describe('if value was changed', () => {
        it('calls with new value', () => {
          const handleChange = jest.fn();
          render(
            <>
              <Textinput {...requiredProps} onChange={handleChange} isEditing value="test" />
              <div>outside click</div>
            </>,
          );

          userEvent.type(screen.getByRole('textbox'), ' typing text...');
          userEvent.click(screen.getByText('outside click'));

          expect(handleChange).toBeCalledWith('test typing text...');
        });
      });

      describe('if value stays the same', () => {
        it(`doesn't call`, () => {
          const handleChange = jest.fn();
          render(
            <>
              <Textinput {...requiredProps} onChange={handleChange} isEditing value="test" />
              <div>outside click</div>
            </>,
          );

          userEvent.type(screen.getByRole('textbox'), `text${specialChars.backspace.repeat(4)}`);
          userEvent.click(screen.getByText('outside click'));

          expect(handleChange).not.toBeCalled();
        });
      });
    });

    describe('on enter press', () => {
      describe('if value was changed', () => {
        it('calls with new value', () => {
          const handleChange = jest.fn();
          render(<Textinput {...requiredProps} onChange={handleChange} isEditing value="test" />);

          userEvent.type(screen.getByRole('textbox'), ' typing text...');
          userEvent.type(screen.getByRole('textbox'), '{enter}');

          expect(handleChange).toBeCalledWith('test typing text...');
        });
      });

      describe('if value stays the same', () => {
        it(`doesn't call`, () => {
          const handleChange = jest.fn();
          render(<Textinput {...requiredProps} onChange={handleChange} isEditing value="test" />);

          userEvent.type(screen.getByRole('textbox'), `text${specialChars.backspace.repeat(4)}`);
          userEvent.type(screen.getByRole('textbox'), '{enter}');

          expect(handleChange).not.toBeCalled();
        });
      });
    });

    it(`doesn't call when input value changes`, () => {
      const handleChange = jest.fn();
      render(<Textinput {...requiredProps} onChange={handleChange} isEditing value="test" />);

      userEvent.type(screen.getByRole('textbox'), ' text');

      expect(handleChange).not.toBeCalled();
    });
  });

  describe('props.isReadLoading', () => {
    describe('when true', () => {
      it('shows text overlay', () => {
        render(<Textinput {...requiredProps} isReadLoading value="test" />);

        expect(screen.getByText('Сохранение изменений...')).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it(`doesn't show text overlay`, () => {
        render(<Textinput {...requiredProps} isReadLoading={false} value="test" />);

        expect(screen.queryByText('Сохранение изменений...')).not.toBeInTheDocument();
      });
    });

    describe('on click', () => {
      it(`doesn't call onEditingStart`, () => {
        const handleEditingStart = jest.fn();
        render(
          <Textinput
            {...requiredProps}
            isReadLoading
            onEditingStart={handleEditingStart}
            label="test label"
          />,
        );

        userEvent.click(screen.getByText('test label'));

        expect(handleEditingStart).not.toBeCalled();
      });
    });
  });

  describe('props.isEditing', () => {
    describe('when true', () => {
      it('shows textinput', () => {
        render(<Textinput {...requiredProps} isEditing label="test label" />);

        expect(screen.getByRole('textbox')).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it(`doesn't show textinput`, () => {
        render(<Textinput {...requiredProps} isEditing={false} label="test label" />);

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
      });
    });

    describe('when changes', () => {
      it('resets text by value', () => {
        const { rerender } = render(<Textinput {...requiredProps} isEditing label="test label" />);
        userEvent.type(screen.getByRole('textbox'), 'text');
        rerender(<Textinput {...requiredProps} isEditing={false} label="test label" />);
        rerender(<Textinput {...requiredProps} isEditing label="test label" />);

        expect(screen.queryByDisplayValue('text')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onEditingStart', () => {
    it('calls on read click', () => {
      const handleEditingStart = jest.fn();
      render(
        <Textinput {...requiredProps} onEditingStart={handleEditingStart} label="test label" />,
      );

      userEvent.click(screen.getByText('test label'));

      expect(handleEditingStart).toBeCalled();
    });

    it(`doesn't call on textinput click`, () => {
      const handleEditingStart = jest.fn();
      render(
        <Textinput
          {...requiredProps}
          onEditingStart={handleEditingStart}
          isEditing
          label="test label"
        />,
      );

      userEvent.click(screen.getByRole('textbox'));

      expect(handleEditingStart).not.toBeCalled();
    });
  });

  describe('props.onEditingStop', () => {
    it('calls on outside click', () => {
      const handleEditingStop = jest.fn();
      render(
        <>
          <Textinput
            {...requiredProps}
            onEditingStop={handleEditingStop}
            isEditing
            label="test label"
          />
          <div>outside click</div>
        </>,
      );

      userEvent.click(screen.getByText('outside click'));

      expect(handleEditingStop).toBeCalled();
    });

    it('it calls on enter press', () => {
      const handleEditingStop = jest.fn();
      render(
        <Textinput
          {...requiredProps}
          onEditingStop={handleEditingStop}
          isEditing
          label="test label"
        />,
      );

      userEvent.type(screen.getByRole('textbox'), '{enter}');

      expect(handleEditingStop).toBeCalled();
    });
  });
});
