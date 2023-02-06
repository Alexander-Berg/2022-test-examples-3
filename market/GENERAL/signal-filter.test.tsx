import React from 'react';
import { render } from '@testing-library/react';

import { history, Wrapper } from '@/test-utils';
import { SignalFilter } from '@/pages/signals/components/signal-filter/signal-filter';
import { SignalTypeDto } from '@/dto';
import { DocumentType } from '@/pages/create-document/models';

describe('<SignalFilter />', () => {
  it('render selects with init values from url', () => {
    let signalTypes: SignalTypeDto[] = [];
    let documentTypes: DocumentType[] = [];
    const users = {};
    history.location.search = 'data_field_pageType=Doc2&signalType=testik&signalType=Qwerty';

    // imitate loading (data is empty)
    const app = render(
      <Wrapper>
        <SignalFilter signalTypes={signalTypes} documentTypes={documentTypes} users={users} />
      </Wrapper>
    );

    signalTypes = [
      { type: 'testik', name: 'Testik' },
      { type: 'Qwerty', name: 'Qwertievna' },
    ];

    documentTypes = [
      { id: 'doc1', label: 'Doc1', namespace: 'Proj1' },
      { id: 'doc2', label: 'Doc2', namespace: 'Proj2' },
    ];

    // data has been loaded
    app.rerender(
      <Wrapper>
        <SignalFilter signalTypes={signalTypes} documentTypes={documentTypes} users={users} />
      </Wrapper>
    );

    const a = app.getAllByText('Выбрано 1');
    const b = app.getAllByText('Выбрано 2');
    expect(a).toHaveLength(1);
    expect(b).toHaveLength(1);
  });
});
