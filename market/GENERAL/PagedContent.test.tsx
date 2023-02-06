import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';

import { PagedContent } from 'src/components/PagedContent';

describe('Paged Content', () => {
  const content = (wrapper: ReactWrapper) => wrapper.find('#content');
  const expectButtonsText = (wrapper: ReactWrapper) => expect(wrapper.find('button span').map(w => w.text()));
  const items = Array.from(Array(23).keys());

  const getInitialWrapper = (): ReactWrapper =>
    mount(
      <PagedContent totalCount={items.length} pageSize={10}>
        {shown => <div id="content">{shown}</div>}
      </PagedContent>
    );

  const expectState = (wrapper: ReactWrapper, contentText: string, statusText: string, buttons: string[]) => {
    expect(content(wrapper)).toHaveLength(1);
    expect(content(wrapper).text()).toEqual(contentText);
    expect(wrapper.html()).toContain(statusText);
    expectButtonsText(wrapper).toEqual(buttons);
  };

  const expectInitialState = (wrapper: ReactWrapper) => {
    expectState(wrapper, '10', 'Показано 10 из 23', ['Показать ещё 10', 'Показать все 23']);
  };

  const expectFinalState = (wrapper: ReactWrapper) => {
    expectState(wrapper, '23', 'Показано 23 из 23', []);
  };

  const clickNavButton = (wrapper: ReactWrapper, index: number) => {
    act(() => wrapper.find('button').get(index).props.onClick());
    wrapper.update();
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
