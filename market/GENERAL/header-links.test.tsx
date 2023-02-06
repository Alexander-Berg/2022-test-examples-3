import React from 'react';
import { render } from '@testing-library/react';
import { Menu } from '@yandex-market/cms-editor-classic';

import { HeaderLinks } from '@/components/layouts/header-links/header-links';
import { Wrapper } from '@/test-utils';

describe('<HeaderLinks />', () => {
  it('renders for a document', () => {
    const app = render(
      <Wrapper>
        <Menu>
          <HeaderLinks documentId="testDocId">
            <div>Custom child div</div>
            <div>Custom child div2</div>
          </HeaderLinks>
        </Menu>
      </Wrapper>
    );
    const eventsPageLink = app.getByText('События');
    expect(eventsPageLink.closest('a')?.getAttribute('href')).toBe('/events?data_field_id=testDocId');

    const childElms = app.getAllByText('Custom child div', { exact: false });
    expect(childElms).toHaveLength(2);
  });
});
