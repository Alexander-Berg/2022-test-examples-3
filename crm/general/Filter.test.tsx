import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Filter } from './Filter';

describe('design/Filter', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('props.label', () => {
    it('renders label', () => {
      render(<Filter label="test label" value={1} />);

      expect(screen.getByText('test label')).toBeInTheDocument();
    });
  });

  describe('props.value', () => {
    it('renders value', () => {
      render(<Filter label="test label" value={321} />);

      expect(screen.getByText('321')).toBeInTheDocument();
    });

    it('not renders nullish value', () => {
      render(<Filter label="test label" value={undefined} />);

      expect(screen.getByLabelText('right').textContent).toBe('');
    });
  });

  describe('props.depth', () => {
    it('adds padding for depth = 1 by default', () => {
      render(<Filter label="label" value={1} />);

      expect(screen.getByLabelText('left').style).toEqual(
        expect.objectContaining({
          paddingLeft: '16px',
        }),
      );
    });

    it('adds padding for depth = 2', () => {
      render(<Filter label="label" value={1} depth={2} />);

      expect(screen.getByLabelText('left').style).toEqual(
        expect.objectContaining({
          paddingLeft: '32px',
        }),
      );
    });

    it('adds padding for depth = 3', () => {
      render(<Filter label="label" value={1} depth={3} />);

      expect(screen.getByLabelText('left').style).toEqual(
        expect.objectContaining({
          paddingLeft: '48px',
        }),
      );
    });
  });

  describe('props.onClick', () => {
    it('calls on box click', () => {
      const handleClick = jest.fn();
      render(<Filter label="test label" value={1} onClick={handleClick} />);

      userEvent.click(screen.getByText('test label'));

      expect(handleClick).toBeCalledTimes(1);
    });

    it('calls on value click', () => {
      const handleClick = jest.fn();
      render(<Filter label="test label" value={1} onClick={handleClick} />);

      userEvent.click(screen.getByText('1'));

      expect(handleClick).toBeCalledTimes(1);
    });

    describe('if disabled', () => {
      it(`doesn't call on click`, () => {
        const handleClick = jest.fn();
        render(<Filter label="test label" value={1} isDisabled onClick={handleClick} />);

        userEvent.click(screen.getByText('test label'));

        expect(handleClick).not.toBeCalled();
      });
    });
  });

  describe('props.onExpand', () => {
    it('calls on value click', () => {
      const handleClick = jest.fn();
      render(<Filter label="test label" canExpand value={1} onClick={handleClick} />);

      userEvent.click(screen.getByText('1'));

      expect(handleClick).toBeCalledTimes(1);
    });

    describe(`when can't expand`, () => {
      it(`doesn't call on value click`, () => {
        const handleClick = jest.fn();
        render(<Filter label="test label" value={1} onClick={handleClick} />);

        userEvent.click(screen.getByText('1'));

        expect(handleClick).toBeCalledTimes(1);
      });
    });
  });
});
