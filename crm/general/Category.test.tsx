import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Category } from './Category';
import {
  categoryTestId,
  checkboxTestId,
  expandingArrowTestId,
  transitionButtonTestId,
} from './Category.constants';

describe('components/design/Category', () => {
  const content = 'text';

  describe('props.canClick', () => {
    describe('is undefined', () => {
      it('calls props.onClick callback on category click', () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} />);
        const category = screen.getByTestId(categoryTestId);

        fireEvent.click(category);

        expect(handleClick).toBeCalledTimes(1);
      });

      it('calls props.onClick callback on checkbox click', () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} />);
        const checkbox = screen.getByTestId(checkboxTestId);

        fireEvent.click(checkbox);

        expect(handleClick).toBeCalledTimes(1);
      });
    });

    describe('is true', () => {
      it('calls props.onClick callback on category click', () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} canClick />);
        const category = screen.getByTestId(categoryTestId);

        fireEvent.click(category);

        expect(handleClick).toBeCalledTimes(1);
      });

      it('calls props.onClick callback on checkbox click', () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} canClick />);
        const checkbox = screen.getByTestId(checkboxTestId);

        fireEvent.click(checkbox);

        expect(handleClick).toBeCalledTimes(1);
      });
    });

    describe('is false', () => {
      it(`doesn't call props.onClick callback on category click`, () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} canClick={false} />);
        const category = screen.getByTestId(categoryTestId);

        fireEvent.click(category);

        expect(handleClick).not.toBeCalled();
      });

      it(`doesn't call props.onClick callback on checkbox click`, () => {
        const handleClick = jest.fn();
        render(<Category content={content} onClick={handleClick} canClick={false} />);
        const checkbox = screen.getByTestId(checkboxTestId);

        fireEvent.click(checkbox);

        expect(handleClick).not.toBeCalled();
      });
    });
  });

  describe('props.canCheck', () => {
    describe('is undefined', () => {
      it('renders checkbox', () => {
        render(<Category content={content} />);
        const checkbox = screen.getByTestId(checkboxTestId);

        expect(checkbox).toBeInTheDocument();
      });
    });

    describe('is true', () => {
      it('renders checkbox', () => {
        render(<Category content={content} canCheck />);
        const checkbox = screen.getByTestId(checkboxTestId);

        expect(checkbox).toBeInTheDocument();
      });
    });

    describe('is false', () => {
      it(`doesn't render checkbox`, () => {
        render(<Category content={content} canCheck={false} />);
        const checkbox = screen.queryByTestId(checkboxTestId);

        expect(checkbox).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onCheck', () => {
    it('calls props.onCheck callback', () => {
      const handleCheck = jest.fn();
      render(<Category content={content} onCheck={handleCheck} />);
      const checkbox = screen.getByTestId(checkboxTestId);

      fireEvent.click(checkbox);

      expect(handleCheck).toBeCalledTimes(1);
      expect(handleCheck).toBeCalledWith(expect.any(Boolean));
    });
  });

  describe('props.isChecked', () => {
    describe('is true', () => {
      it('sets true value to checkbox', () => {
        render(<Category content={content} isChecked />);
        const checkbox = screen.getByTestId(checkboxTestId);

        expect(checkbox).toBeChecked();
      });
    });

    describe('is false', () => {
      it('sets false value to checkbox', () => {
        render(<Category content={content} isChecked={false} />);
        const checkbox = screen.getByTestId(checkboxTestId);

        expect(checkbox).not.toBeChecked();
      });
    });
  });

  describe('props.canDoubleClickChecked', () => {
    describe('is true', () => {
      it('calls props.onCheck after double click', () => {
        const handleCheck = jest.fn();
        render(<Category content={content} onCheck={handleCheck} canDoubleClickChecked />);
        const category = screen.getByTestId(categoryTestId);

        userEvent.dblClick(category);

        expect(handleCheck).toBeCalledTimes(1);
      });
    });

    describe('is false', () => {
      it(`doesn't call props.onCheck after double click`, () => {
        const handleCheck = jest.fn();
        render(<Category content={content} onCheck={handleCheck} canDoubleClickChecked={false} />);
        const category = screen.getByTestId(categoryTestId);

        userEvent.dblClick(category);

        expect(handleCheck).not.toBeCalled();
      });
    });
  });

  describe('props.hasExpandingArrow', () => {
    describe('is true', () => {
      it('renders expanding arrow', () => {
        render(<Category content={content} hasExpandingArrow />);
        const expandingArrow = screen.getByTestId(expandingArrowTestId);

        expect(expandingArrow).toBeInTheDocument();
      });
    });

    describe('is false', () => {
      it(`doesn't render expanding arrow`, () => {
        render(<Category content={content} hasExpandingArrow={false} />);
        const expandingArrow = screen.queryByTestId(expandingArrowTestId);

        expect(expandingArrow).not.toBeInTheDocument();
      });
    });
  });

  describe('props.hasTransitionButton', () => {
    describe('is true', () => {
      it('renders transition button', () => {
        render(<Category content={content} hasTransitionButton />);
        const transitionButton = screen.getByTestId(transitionButtonTestId);

        expect(transitionButton).toBeInTheDocument();
      });
    });

    describe('is false', () => {
      it(`doesn't render transition button`, () => {
        render(<Category content={content} hasTransitionButton={false} />);
        const transitionButton = screen.queryByTestId(transitionButtonTestId);

        expect(transitionButton).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onTransitionClick', () => {
    it('calls on transition button click', () => {
      const handleTransitionClick = jest.fn();
      render(
        <Category
          content={content}
          hasTransitionButton
          onTransitionClick={handleTransitionClick}
        />,
      );
      const transitionButton = screen.getByTestId(transitionButtonTestId);

      userEvent.click(transitionButton);

      expect(handleTransitionClick).toBeCalledTimes(1);
    });
  });
});
