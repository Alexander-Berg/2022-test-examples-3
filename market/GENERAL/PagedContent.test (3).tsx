import React from 'react';
import { render, RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PagedContent } from 'src/components/PagedContent';

describe('Paged Content', () => {
  const content = (wrapper: RenderResult) => wrapper.getAllByTestId('content');
  const expectButtonsText = (wrapper: RenderResult) =>
    expect(Array.from(wrapper.container.querySelectorAll('button span')).map(w => w.innerHTML));
  const items = Array.from(Array(23).keys());

  const getInitialWrapper = (): RenderResult =>
    render(
      <PagedContent totalCount={items.length} pageSize={10}>
        {shown => <div data-testid="content">{shown}</div>}
      </PagedContent>
    );

  const expectState = (wrapper: RenderResult, contentText: string, statusText: string, buttons: string[]) => {
    expect(content(wrapper)).toHaveLength(1);
    expect(wrapper.getByText(statusText)).toBeTruthy();
    expectButtonsText(wrapper).toEqual(buttons);
  };

  const expectInitialState = (wrapper: RenderResult) => {
    expectState(wrapper, '10', 'Показано 10 из 23', ['Показать ещё 10', 'Показать все 23']);
  };

  const expectFinalState = (wrapper: RenderResult) => {
    expectState(wrapper, '23', 'Показано 23 из 23', []);
  };

  const clickNavButton = (wrapper: RenderResult, index: number) => {
    userEvent.click(wrapper.getAllByRole('button')[index]!);
  };

  it('Paged content show page controls only when it necessary and do it correctly', () => {
    const wrapper = getInitialWrapper();
    expectInitialState(wrapper);

    clickNavButton(wrapper, 0);

    expectState(wrapper, '20', 'Показано 20 из 23', ['Показать ещё 3', 'Показать все 23']);

    clickNavButton(wrapper, 0);

    expectFinalState(wrapper);
  });

  it('Paged content scroll to end appropriately', () => {
    const wrapper = getInitialWrapper();
    expectInitialState(wrapper);

    clickNavButton(wrapper, 1);

    expectFinalState(wrapper);
  });
});
