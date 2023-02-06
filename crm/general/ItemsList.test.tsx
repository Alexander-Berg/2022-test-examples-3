import React, { FC } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ItemsList } from './ItemsList';
import { RenderProps } from '../Group.types';

const renderHoveredId: FC<RenderProps> = (props) => {
  if (!props.isHovered) {
    return null;
  }

  const item = props.item as { id: number };

  return <div onClick={props.onClick}>{item.id}</div>;
};

const items = [
  {
    id: 1,
  },
  {
    id: 2,
  },
  {
    id: 3,
  },
];

describe('design/Group/ItemsList', () => {
  it('allows to navigate using arrow down', async () => {
    render(<ItemsList items={items} isFocused renderItem={renderHoveredId} />);

    expect(screen.getByText('1')).toBeInTheDocument();

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('allows to navigate using arrow up', async () => {
    render(<ItemsList items={items} isFocused renderItem={renderHoveredId} />);

    expect(screen.getByText('1')).toBeInTheDocument();

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('allows different sequence of arrows', async () => {
    render(<ItemsList items={items} isFocused renderItem={renderHoveredId} />);

    expect(screen.getByText('1')).toBeInTheDocument();

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowup}');
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('allows to change using enter key', async () => {
    const handleChange = jest.fn();
    render(
      <ItemsList items={items} isFocused renderItem={renderHoveredId} onChange={handleChange} />,
    );

    expect(screen.getByText('1')).toBeInTheDocument();

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{enter}');
    await waitFor(() => {
      expect(handleChange).toBeCalledWith(items.find((item) => item.id === 2));
    });

    userEvent.type(document.body, '{arrowdown}');
    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    userEvent.type(document.body, '{enter}');
    await waitFor(() => {
      expect(handleChange).toBeCalledWith(items.find((item) => item.id === 3));
    });
  });

  it('allows to change using mouse click', async () => {
    const handleChange = jest.fn();
    render(
      <ItemsList items={items} isFocused renderItem={renderHoveredId} onChange={handleChange} />,
    );

    userEvent.click(screen.getByText('1'));

    await waitFor(() => {
      expect(handleChange).toBeCalledWith(items.find((item) => item.id === 1));
    });
  });

  describe('props.isFocused', () => {
    describe('when true', () => {
      it('adds keydown event listener on mount', () => {
        document.addEventListener = jest.fn();

        render(<ItemsList isFocused renderItem={renderHoveredId} />);

        expect(document.addEventListener).toBeCalledWith('keydown', expect.any(Function));
      });

      it('removes keydown event listener on unmount', () => {
        document.removeEventListener = jest.fn();

        const { unmount } = render(<ItemsList isFocused renderItem={renderHoveredId} />);
        unmount();

        expect(document.removeEventListener).toBeCalledWith('keydown', expect.any(Function));
      });
    });

    describe('when false', () => {
      it(`doesn't add keydown event listener on mount`, () => {
        document.addEventListener = jest.fn();

        render(<ItemsList isFocused={false} renderItem={renderHoveredId} />);

        expect(document.addEventListener).not.toBeCalled();
      });

      it(`doesn't remove keydown event listener on unmount`, () => {
        document.removeEventListener = jest.fn();

        const { unmount } = render(<ItemsList isFocused={false} renderItem={renderHoveredId} />);
        unmount();

        expect(document.removeEventListener).not.toBeCalled();
      });
    });

    describe('when changes from false to true', () => {
      it('adds keydown event listener', () => {
        document.addEventListener = jest.fn();

        const { rerender } = render(<ItemsList isFocused={false} renderItem={renderHoveredId} />);
        rerender(<ItemsList isFocused renderItem={renderHoveredId} />);

        expect(document.addEventListener).toBeCalledWith('keydown', expect.any(Function));
      });
    });

    describe('when changes from true to false', () => {
      it('removes keydown event listener', () => {
        document.removeEventListener = jest.fn();

        const { rerender } = render(<ItemsList isFocused renderItem={renderHoveredId} />);
        rerender(<ItemsList isFocused={false} renderItem={renderHoveredId} />);

        expect(document.removeEventListener).toBeCalledWith('keydown', expect.any(Function));
      });
    });
  });
});
