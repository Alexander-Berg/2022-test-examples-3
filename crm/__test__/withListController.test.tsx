import React, { createElement } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import withListController from '../Original/withListController';

jest.useFakeTimers();

const List = ({ getScrollNode, classNameScrollNode, ...props }) =>
  createElement('div', { ref: getScrollNode, 'data-wrap-node': true, ...props });

const ListController = withListController()(List);

const ids = [0, 1, 2];

interface ItemProps {
  id?: number;
  selected?: boolean;
}

const Item: React.FC<ItemProps> = ({ id, selected }) => <div data-selected={selected}>{id}</div>;

describe('ListController', () => {
  it('should has no error when no children', () => {
    render(<ListController />);

    fireEvent.keyDown(screen.getByRole('presentation'), { key: 'ArrowDown', code: 'ArrowDown' });
  });

  it('should has ids in children text content after mount', () => {
    render(
      <ListController>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    const childrenIds = Array.from(list.childNodes).map((child) =>
      parseInt(child.textContent || '', 10),
    );

    expect(childrenIds).toEqual(ids);
  });

  it('should not have selected items after render if props.selected undefined', () => {
    render(
      <ListController>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    Array.from(screen.getByRole('presentation').childNodes).forEach((child) => {
      expect(child).toHaveAttribute('data-selected', 'false');
    });
  });

  it('should not have selected items after keydown "ArrowUp"', () => {
    render(
      <ListController>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    fireEvent.keyDown(list, { key: 'ArrowUp', code: 'ArrowUp' });

    Array.from(screen.getByRole('presentation').childNodes).forEach((child) => {
      expect(child).toHaveAttribute('data-selected', 'false');
    });
  });

  it('should has first item selected after keydown "ArrowDown"', () => {
    render(
      <ListController>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    expect(list.firstChild).toHaveAttribute('data-selected', 'true');
  });

  it('should has item with id 2 selected', () => {
    render(
      <ListController selected={2}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    expect(list.childNodes[0]).toHaveAttribute('data-selected', 'false');
    expect(list.childNodes[1]).toHaveAttribute('data-selected', 'false');
    expect(list.childNodes[2]).toHaveAttribute('data-selected', 'true');
  });

  it('should still has last item selected after keydown "ArrowDown"', () => {
    render(
      <ListController selected={2}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    expect(list.lastChild).toHaveAttribute('data-selected', 'true');

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    expect(list.lastChild).toHaveAttribute('data-selected', 'true');
  });

  it('should has last item selected after keydown "ArrowDown"', () => {
    render(
      <ListController selected={1}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    expect(list.childNodes[1]).toHaveAttribute('data-selected', 'true');
    expect(list.childNodes[2]).toHaveAttribute('data-selected', 'false');

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    expect(list.childNodes[1]).toHaveAttribute('data-selected', 'false');
    expect(list.childNodes[2]).toHaveAttribute('data-selected', 'true');
  });

  it('should call onChange', () => {
    const onChange = jest.fn();

    render(
      <ListController selected={1} onChange={onChange} onChangeDelay={0}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    // the onChange is called zero
    expect(onChange).not.toHaveBeenCalled();

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    // the onChange is called 1
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenNthCalledWith(1, 2, { children: 2, id: 2 });

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });
    // the onChange is called still 1
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('should no call onEnter', () => {
    const onEnter = jest.fn();

    render(
      <ListController onEnter={onEnter}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    // the onEnter is called zero
    expect(onEnter).not.toHaveBeenCalled();

    fireEvent.keyDown(list, { key: 'Enter', code: 'Enter' });
    // the onEnter is called 1
    expect(onEnter).not.toHaveBeenCalled();
  });

  it('should call onEnter with 1', () => {
    const onEnter = jest.fn();

    render(
      <ListController selected={1} onEnter={onEnter}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    // the onEnter is called zero
    expect(onEnter).not.toHaveBeenCalled();

    fireEvent.keyDown(list, { key: 'Enter', code: 'Enter' });
    // the onEnter is called 1
    expect(onEnter).toHaveBeenCalledTimes(1);
    expect(onEnter).toHaveBeenNthCalledWith(1, 1);
  });

  it('should call onEscape', () => {
    const onEscape = jest.fn();

    render(
      <ListController selected={0} onEscape={onEscape}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    // the onEscape is called zero
    expect(onEscape).not.toHaveBeenCalled();

    fireEvent.keyDown(list, { key: 'Escape', code: 'Escape' });
    // the onEscape is called 1
    expect(onEscape).toHaveBeenCalledTimes(1);
  });

  it('change children', () => {
    const { rerender } = render(
      <ListController selected={1}>
        {ids.map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      </ListController>,
    );

    const children = [0, 1, 2, 3].map((id) => (
      <Item key={id} id={id}>
        {id}
      </Item>
    ));

    const list = screen.getByRole('presentation');

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    rerender(<ListController selected={1} children={children} />);

    expect(list.childNodes[2]).toHaveAttribute('data-selected', 'true');
  });

  it('should no call onChange if selected prop change', () => {
    const onChange = jest.fn();

    const children = ids.map((id) => (
      <Item key={id} id={id}>
        {id}
      </Item>
    ));

    const { rerender } = render(
      <ListController onChange={onChange} onChangeDelay={0} children={children} />,
    );

    expect(onChange).not.toHaveBeenCalled();

    rerender(
      <ListController selected={1} onChange={onChange} onChangeDelay={0} children={children} />,
    );

    expect(onChange).not.toHaveBeenCalled();
  });

  it('some children without id', () => {
    render(
      <ListController>
        <Item key={0}>0</Item>
        <Item key={1} id={1}>
          1
        </Item>
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });
    expect(list.childNodes[1]).toHaveAttribute('data-selected', 'true');
  });

  it('if no selected id in array in should move to top on event', () => {
    render(
      <ListController selected={10}>
        <Item key={0} id={0}>
          0
        </Item>
        <Item key={1} id={1}>
          1
        </Item>
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    Array.from(list.childNodes).forEach((child) => {
      expect(child).toHaveAttribute('data-selected', 'false');
    });

    fireEvent.keyDown(list, { key: 'ArrowDown', code: 'ArrowDown' });

    expect(list.firstChild).toHaveAttribute('data-selected', 'true');
  });

  it('should auto select first', () => {
    const { rerender } = render(
      <ListController autoSelectFirst>
        <Item key={0} id={0}>
          0
        </Item>
        <Item key={1} id={1}>
          1
        </Item>
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    expect(list.firstChild).toHaveAttribute('data-selected', 'true');

    rerender(
      <ListController
        autoSelectFirst
        children={[3, 4].map((id) => (
          <Item key={id} id={id}>
            {id}
          </Item>
        ))}
      />,
    );

    expect(list.firstChild).toHaveAttribute('data-selected', 'true');
  });

  it('should focus on select first', () => {
    render(
      <ListController autoSelectFirst focusOnSelectFirst>
        <Item key={0} id={0}>
          0
        </Item>
        <Item key={1} id={1}>
          1
        </Item>
      </ListController>,
    );

    const list = screen.getByRole('presentation');

    expect(document.activeElement).toBeInstanceOf(HTMLDivElement);
    expect(document.activeElement).toHaveTextContent('0');
    expect(list).toHaveAttribute('data-wrap-node', 'true');
  });
});
