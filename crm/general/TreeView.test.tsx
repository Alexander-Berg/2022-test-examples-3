import {
  act,
  cleanup,
  queryAllByText,
  queryByTestId,
  queryByText,
  render,
  screen,
  waitFor,
} from '@testing-library/react/pure';
import React from 'react';
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';
import { testData } from './stubs/testData';
import { TreeView } from './TreeView';
import { ButtonsClearAll } from './Buttons/ButtonsClear';

describe('TreeView', () => {
  // For Autosizer
  const originalOffsetHeight = Object.getOwnPropertyDescriptor(
    HTMLElement.prototype,
    'offsetHeight',
  );
  const originalOffsetWidth = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'offsetWidth');

  beforeAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'offsetHeight', { configurable: true, value: 50 });
    Object.defineProperty(HTMLElement.prototype, 'offsetWidth', { configurable: true, value: 50 });
  });

  afterAll(() => {
    Object.defineProperty(HTMLElement.prototype, 'offsetHeight', originalOffsetHeight!);
    Object.defineProperty(HTMLElement.prototype, 'offsetWidth', originalOffsetWidth!);
  });

  it('renders simple tree', () => {
    render(<TreeView items={testData} />);

    const tree = screen.queryByTestId('tree-view');
    expect(tree).toBeVisible();

    cleanup();
  });

  it('renders 3 visible items when all collapsed', () => {
    render(<TreeView items={testData} />);
    const items = screen.queryAllByTestId('tree-view-item');

    expect(items).toHaveLength(3);

    cleanup();
  });

  it('renders 5 visible items when first node expanded', () => {
    render(<TreeView items={testData} />);
    let items = screen.queryAllByTestId('tree-view-item');
    const expandIcon = queryByTestId(items[0], 'tree-view-item-expand');

    userEvent.click(expandIcon!);

    expect(screen.queryAllByTestId('tree-view-item')).toHaveLength(5);

    cleanup();
  });

  it('renders all blocks', () => {
    render(<TreeView items={testData} showSelectedBlock showSearch showButtons />);
    let items = screen.queryAllByTestId('tree-view-item');
    const expandIcon = queryByTestId(items[0], 'tree-view-item-expand');

    userEvent.click(expandIcon!);

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);

    const selectedBlock = screen.queryByTestId('tree-view-selected-block');
    expect(selectedBlock).toBeVisible();

    const buttons = screen.queryByTestId('tree-view-buttons');
    expect(buttons).toBeVisible();

    const search = screen.queryByTestId('tree-view-search');
    expect(search).toBeVisible();

    cleanup();
  });

  it('selects items on click', () => {
    render(<TreeView items={testData} showSelectedBlock />);
    let items = screen.queryAllByTestId('tree-view-item');

    expect(screen.queryByTestId('tree-view-selected-block')).toBeNull();

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);

    expect(screen.queryByTestId('tree-view-selected-block')).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeNull();

    userEvent.click(queryByTestId(items[1], 'tree-view-item-label')!);
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeVisible();

    userEvent.click(queryByTestId(items[1], 'tree-view-item-label')!);
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeNull();

    cleanup();
  });

  it('`reset` behavior works', () => {
    render(
      <TreeView items={testData} showButtons showSelectedBlock defaultSelected={['1', '2']} />,
    );
    const items = screen.queryAllByTestId('tree-view-item');

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Автотранспорт'),
    ).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeNull();

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);
    userEvent.click(queryByTestId(items[1], 'tree-view-item-label')!);

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeNull();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeVisible();

    userEvent.click(screen.queryByText('Отменить')!);

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Автотранспорт'),
    ).toBeVisible();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeNull();

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);
    userEvent.click(queryByTestId(items[1], 'tree-view-item-label')!);

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeNull();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeVisible();

    userEvent.click(screen.queryByText('Сохранить')!);
    userEvent.click(screen.queryByText('Отменить')!);

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'),
    ).toBeNull();
    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeVisible();
  });

  it('`clear all` behavior works', async () => {
    render(
      <TreeView
        items={testData}
        showButtons
        Buttons={ButtonsClearAll}
        showSelectedBlock
        defaultSelected={['1', '2']}
      />,
    );

    const items = screen.queryAllByTestId('tree-view-item');

    expect(screen.queryByTestId('tree-view-selected-block')).toBeVisible();

    userEvent.click(screen.queryByText('Сбросить')!);

    expect(screen.queryByTestId('tree-view-selected-block')).toBeNull();

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);
    userEvent.click(queryByTestId(items[1], 'tree-view-item-label')!);
    userEvent.click(screen.queryByText('Применить')!);

    expect(queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Авто: товары'));

    expect(
      queryByText(screen.queryByTestId('tree-view-selected-block')!, 'Общая информация'),
    ).toBeVisible();

    userEvent.click(screen.queryByText('Сбросить')!);

    expect(screen.queryByTestId('tree-view-selected-block')).toBeNull();
  });

  describe('search', () => {
    it('works in general', async () => {
      render(
        <TreeView
          items={testData}
          showButtons
          Buttons={ButtonsClearAll}
          showSelectedBlock
          showSearch
        />,
      );

      const textbox = screen.getByRole('textbox');

      expect(screen.queryByText('Общая информация')).toBeVisible();

      await act(async () => {
        fireEvent.change(textbox, { target: { value: 'Дилер' } });
      });

      await waitFor(() => expect(screen.queryByText('Общая информация')).toBeNull(), {
        timeout: 200,
      });
      await waitFor(() =>
        expect(queryAllByText(screen.queryByTestId('tree-view-list')!, 'Дилер')).toHaveLength(2),
      );
      await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(5));

      cleanup();
    });

    describe('when `keep expanded` behavior', () => {
      it('expand is blocked', async () => {
        render(
          <TreeView
            items={testData}
            showButtons
            Buttons={ButtonsClearAll}
            showSelectedBlock
            showSearch
          />,
        );

        const textbox = screen.getByRole('textbox');

        await act(async () => {
          fireEvent.change(textbox, { target: { value: 'Дилер' } });
        });

        await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(5));

        const expandIcon = screen.queryAllByTestId('tree-view-item-expand')[0];

        userEvent.click(expandIcon!);

        await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(5));

        cleanup();
      });
    });

    describe('when `expand once` behavior', () => {
      it('expand is not blocked', async () => {
        render(
          <TreeView
            items={testData}
            showButtons
            Buttons={ButtonsClearAll}
            showSelectedBlock
            showSearch
            searchExpandBehavior="expandOnce"
          />,
        );

        const textbox = screen.getByRole('textbox');

        await act(async () => {
          fireEvent.change(textbox, { target: { value: 'Дилер' } });
        });

        await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(5));

        const expandIcon = screen.queryAllByTestId('tree-view-item-expand')[0];

        userEvent.click(expandIcon!);

        await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(1));

        cleanup();
      });
    });

    describe('when `none` behavior', () => {
      it('does not expand', async () => {
        render(
          <TreeView
            items={testData}
            showButtons
            Buttons={ButtonsClearAll}
            showSelectedBlock
            showSearch
            searchExpandBehavior="none"
          />,
        );

        const textbox = screen.getByRole('textbox');

        await act(async () => {
          fireEvent.change(textbox, { target: { value: 'Дилер' } });
        });

        await waitFor(() => expect(screen.queryAllByTestId('tree-view-item')!).toHaveLength(1));

        cleanup();
      });
    });
  });

  it('disableItemSelectionPredicate works', () => {
    render(
      <TreeView items={testData} showSelectedBlock disableItemSelectionPredicate={() => true} />,
    );

    expect(screen.queryByTestId('tree-view-selected-block')).toBeNull();

    userEvent.click(screen.queryAllByTestId('tree-view-item-label')[0]);

    expect(screen.queryByTestId('tree-view-selected-block')).toBeNull();

    cleanup();
  });

  it('onChange function is called with correct args', async () => {
    const onChange = jest.fn();

    render(<TreeView items={testData} onChange={onChange} />);

    let items = screen.queryAllByTestId('tree-view-item');

    userEvent.click(queryByTestId(items[0], 'tree-view-item-label')!);

    expect(onChange).toBeCalledWith(['1']);
  });

  it('onSave function is called with correct args', () => {
    const onSave = jest.fn();

    render(<TreeView items={testData} showButtons onSave={onSave} />);

    userEvent.click(screen.queryAllByTestId('tree-view-item-label')[0]);

    userEvent.click(screen.queryByText('Сохранить')!);

    expect(onSave).toBeCalledWith(['1']);
  });

  it('onExpand function is called with correct args', () => {
    const onExpand = jest.fn();

    render(<TreeView items={testData} onExpand={onExpand} />);

    const expandIcon = screen.queryAllByTestId('tree-view-item-expand')[0];

    userEvent.click(expandIcon);

    expect(onExpand).toBeCalledWith(['1']);

    cleanup();
  });
});
