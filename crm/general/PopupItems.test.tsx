import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PopupItems } from './PopupItems';

describe('PopupItems', () => {
  describe('props.isEditing', () => {
    describe('when is true', () => {
      it('renders popup', () => {
        render(<PopupItems items={[]} onClose={jest.fn()} isEditing />);

        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render popup`, () => {
        render(<PopupItems items={[]} onClose={jest.fn()} isEditing={false} />);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.value', () => {
    it('forwards props.isSelected in props.renderItem', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
        {
          value: 2,
          name: '2',
        },
        {
          value: 3,
          name: '3',
        },
      ];
      const renderOnlySelected = ({ item, isSelected }) =>
        isSelected && <span data-testid="test item">{item.name}</span>;
      render(
        <PopupItems
          value={2}
          items={items}
          onClose={jest.fn()}
          isEditing
          renderItem={renderOnlySelected}
        />,
      );

      expect(screen.getAllByTestId('test item')).toHaveLength(1);
      expect(screen.getByTestId('test item')).toHaveTextContent('2');
    });
  });

  describe('props.renderItem', () => {
    it('renders item using it', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
        {
          value: 2,
          name: '2',
        },
      ];
      const renderItem = ({ item }) => <span data-testid="test item">{item.name}</span>;
      render(<PopupItems items={items} onClose={jest.fn()} isEditing renderItem={renderItem} />);

      expect(screen.getAllByTestId('test item')).toHaveLength(2);
    });
  });

  describe('props.getValue', () => {
    it('allows map any item to needed value', () => {
      const items = [
        {
          notStandardValueField: 1,
          name: '1',
        },
        {
          notStandardValueField: 2,
          name: '2',
        },
        {
          notStandardValueField: 3,
          name: '3',
        },
      ];
      const renderOnlySelected = ({ item, isSelected }) =>
        isSelected && <span data-testid="test item">{item.name}</span>;
      render(
        <PopupItems
          getValue={(item) => (item as { notStandardValueField: number }).notStandardValueField}
          value={2}
          items={items}
          onClose={jest.fn()}
          isEditing
          renderItem={renderOnlySelected}
        />,
      );

      expect(screen.getAllByTestId('test item')).toHaveLength(1);
    });
  });

  describe('props.onChange', () => {
    it('calls on item change', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
        {
          value: 2,
          name: '2',
        },
        {
          value: 3,
          name: '3',
        },
      ];
      const renderItem = (props: { item: unknown; onClick?: () => void }) => {
        return (
          <span data-testid="test item" onClick={props.onClick}>
            {(props.item as { name: string }).name}
          </span>
        );
      };
      const handleChange = jest.fn();
      render(
        <PopupItems
          value={2}
          onChange={handleChange}
          items={items}
          onClose={jest.fn()}
          isEditing
          renderItem={renderItem}
        />,
      );

      userEvent.click(screen.getByText('2'));

      expect(handleChange).toHaveBeenCalledWith(2);
    });
  });

  describe('props.isLoading', () => {
    it('renders spinner if true', () => {
      render(<PopupItems items={[]} onClose={jest.fn()} isEditing isLoading />);

      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it(`doesn't render spinner if false`, () => {
      render(<PopupItems items={[]} onClose={jest.fn()} isEditing isLoading={false} />);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('props.onClose', () => {
    it('calls on outside click', () => {
      const handleClose = jest.fn();
      render(
        <div>
          <PopupItems items={[]} onClose={handleClose} isEditing />
          <div>outside</div>
        </div>,
      );

      userEvent.click(screen.getByText('outside'));

      expect(handleClose).toBeCalledTimes(1);
    });
  });
});
