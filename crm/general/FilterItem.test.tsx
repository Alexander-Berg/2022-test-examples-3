import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FilterItem } from './FilterItem';

const defaultProps = {
  onSelect: jest.fn(),
  onExpand: jest.fn(),
  getIsSelected: jest.fn(),
  id: 'id',
  data: {
    label: 'label',
  },
  isLeaf: false,
  nestingLevel: 0,
};

describe('FilterItem', () => {
  describe('props.data.counter', () => {
    describe('when defined', () => {
      it('renders count number', () => {
        const counterData = { ...defaultProps.data, counter: 15 };
        render(<FilterItem {...defaultProps} data={counterData} />);

        expect(screen.getByText(counterData.counter)).toBeInTheDocument();
      });
    });
  });

  describe('props.data.label', () => {
    describe('when defined', () => {
      it('renders label', () => {
        render(<FilterItem {...defaultProps} />);

        expect(screen.getByText(defaultProps.data.label)).toBeInTheDocument();
      });
    });
  });

  describe('props.isLeaf', () => {
    describe('when true', () => {
      it("doesn't render arrow icon", () => {
        render(<FilterItem {...defaultProps} isLeaf />);

        expect(screen.getByTestId('item-icon')).toHaveAttribute('data-hidden', 'true');
      });
    });

    describe('when false', () => {
      it('renders arrow icon', () => {
        render(<FilterItem {...defaultProps} isLeaf={false} />);

        expect(screen.getByTestId('item-icon')).toHaveAttribute('data-hidden', 'false');
      });
    });
  });

  describe('props.onExpand', () => {
    describe('when defined', () => {
      it('calls on icon click', () => {
        const onExpand = jest.fn();
        render(<FilterItem {...defaultProps} onExpand={onExpand} />);

        userEvent.click(screen.getByTestId('item-icon'));

        expect(onExpand).toBeCalledWith(defaultProps.id);
      });
    });
  });

  describe('props.onSelect', () => {
    describe('when defined', () => {
      it('calls on item click', () => {
        const onSelect = jest.fn();
        render(<FilterItem {...defaultProps} onSelect={onSelect} />);

        userEvent.click(screen.getByText(defaultProps.data.label));

        expect(onSelect).toBeCalledWith(defaultProps.id, defaultProps.isLeaf);
      });
    });
  });

  describe('props.disableItemSelectionPredicate', () => {
    describe('when return true', () => {
      it("doesn't call on item click", () => {
        const onSelect = jest.fn();
        const disableItemSelectionPredicate = jest.fn(() => true);
        render(
          <FilterItem
            {...defaultProps}
            onSelect={onSelect}
            disableItemSelectionPredicate={disableItemSelectionPredicate}
          />,
        );

        userEvent.click(screen.getByText(defaultProps.data.label));

        expect(disableItemSelectionPredicate).toBeCalled();
        expect(onSelect).not.toBeCalled();
      });
    });
  });
});
