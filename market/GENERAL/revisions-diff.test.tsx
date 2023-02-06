import React from 'react';
import { render } from '@testing-library/react';

import { getTestProvider, Wrapper } from '@/test-utils';
import { RevisionsDiff } from '@/widgets/revisions-diff/revisions-diff';
import { loadDocumentRevisionsDiffAction } from '@/pages/document-editor/actions';
import { EntityType, GetDocumentRevisionsDiffResponse } from '@/dto';

describe('<RevisionDiff />', () => {
  it('load data', () => {
    const docId = '123';
    const eventType = '123';
    const revId = 123;

    const loadAction = jest.spyOn(loadDocumentRevisionsDiffAction, 'started');

    expect(() => {
      render(
        <Wrapper>
          <RevisionsDiff documentId={docId} revisionId={revId} eventType={eventType} />
        </Wrapper>
      );
    }).not.toThrow();

    expect(loadAction).toBeCalledTimes(1);
    expect(loadAction).toHaveBeenLastCalledWith({ documentId: docId, eventType, revisionId: revId });
  });

  it('render diff', () => {
    const { Provider, store } = getTestProvider();
    const docId = '123';
    const revId = 123;
    const rev2Id = 456;

    store.dispatch(
      loadDocumentRevisionsDiffAction.done({
        params: { documentId: docId, revisionId: revId, revision2Id: rev2Id },
        result: {
          oldRevision: {
            data: {
              revisionId: 456,
              documentType: 'type 123',
              isTemplate: false,
              createdAt: '123456789',
              publishedAt: '987654321',
              type: EntityType.Document,
              name: 'name',
              updatedAt: '123456789',
            },
            includes: {},
          },
          currentRevision: {
            data: {
              revisionId: 123,
              documentType: 'type 123',
              isTemplate: false,
              createdAt: '123456789',
              publishedAt: '987654321',
              type: EntityType.Document,
              name: 'name',
              updatedAt: '123456789',
            },
          },
        } as GetDocumentRevisionsDiffResponse,
      })
    );

    const app = render(
      <Provider>
        <RevisionsDiff documentId={docId} revisionId={revId} revision2Id={rev2Id} />
      </Provider>
    );

    // there are so many identical lines, that button "show more" appears
    const showMoreButton = app.getByText('Показать', { exact: false });
    expect(showMoreButton).toBeInTheDocument();
  });
});
