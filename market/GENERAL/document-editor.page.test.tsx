import React from 'react';
import { createMemoryHistory } from 'history';
import { rest, MockedRequest, ResponseResolver, restContext } from 'msw';
import * as R from 'ramda';
import {
  EntityType as EditorEntityType,
  createEntryType,
  ContentType,
  createReferenceType as createEditorReferenceType,
} from '@yandex-market/cms-editor-core';

import { DocumentEditorPage } from '.';

import {
  EntityType,
  DocumentDto,
  DocumentSchemaDto,
  BranchRevisionDto,
  Reference,
  BranchDto,
  DocumentTypeDto,
  ReferenceTypes,
} from '@/dto';
import { render, screen, waitFor, server } from '@/test-utils';

function createReferenceType<T extends ReferenceTypes>(id: string, referenceType: T): Reference<T> {
  return {
    type: EntityType.Reference,
    id,
    referenceType,
  };
}

const getMockEmptyArrayApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  return res(ctx.status(200), ctx.body({ data: { type: EntityType.Array, items: [] } }));
};

const getMockDocumentApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const entry = createEntryType('ROOT_CONTEXT');
  const data: DocumentDto = {
    type: EntityType.Document,
    id: 1,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    name: 'Example document',
    documentType: 'cms',
    isTemplate: false,
    latestRevisionId: 1,
    revisionId: 1,
    branchRevision: createReferenceType('1', EntityType.BranchRevision),
    entry: createEditorReferenceType(entry.id, EditorEntityType.Entry),
  };

  const includes = {
    [EditorEntityType.Entry]: [entry],
  };

  return res(ctx.status(200), ctx.body({ data, includes }));
};

const getMockDocumentSchemaApiResponse: ResponseResolver<MockedRequest, typeof restContext> = (req, res, ctx) => {
  const branchRef = createReferenceType('1', EntityType.Branch);
  const branchRevisionRef = createReferenceType('1', EntityType.BranchRevision);
  const documentTypeRef = createReferenceType('cms', EntityType.DocumentType);
  const contentTypeRef = createReferenceType('ROOT_CONTEXT', EntityType.ContentType);
  const namespace = createReferenceType('cms', EntityType.Namespace);

  const data: DocumentSchemaDto = {
    type: EntityType.DocumentSchema,
    id: 1,
    branchRevision: branchRevisionRef,
    branch: branchRef,
    contentTypes: [contentTypeRef],
    documentType: documentTypeRef,
    namespace,
  };
  const branchRevision: BranchRevisionDto = {
    type: EntityType.BranchRevision,
    branch: branchRef,
    id: 1,
  };
  const branch: BranchDto = {
    type: EntityType.Branch,
    id: 1,
    name: 'master',
    namespace,
  };
  const documentType: DocumentTypeDto = {
    type: EntityType.DocumentType,
    id: 'cms',
    name: 'Example document type',
    contentType: contentTypeRef,
    exports: [],
  };
  const contentType: ContentType = {
    type: EditorEntityType.ContentType,
    id: 'ROOT_CONTEXT',
    fields: [],
    name: 'ROOT_CONTEXT',
    parentNames: [],
    properties: {},
    selectors: [],
    templates: [],
  };

  const items: unknown[] = [branchRevision, branch, documentType, contentType];
  const includes = R.groupBy(R.prop('type'), items);

  return res(ctx.status(200), ctx.body({ data, includes }));
};

describe('<DocumentEditorPage />', () => {
  const history = createMemoryHistory({
    initialEntries: ['/documents/1/edit'],
  });

  const match = {
    path: '/documents/:documentId([0-9]+)/edit',
    url: '/documents/1/edit',
    isExact: true,
    params: { documentId: '1' },
  };

  beforeEach(() => {
    server.use(
      rest.get('/api/v1/documents/:documentId/revisions', getMockEmptyArrayApiResponse),
      rest.get('/api/v1/users/me/favourite-entries', getMockEmptyArrayApiResponse),
      rest.get('/api/v1/scheduled_actions', getMockEmptyArrayApiResponse),
      rest.get('/api/v1/documents/:documentId', getMockDocumentApiResponse),
      rest.get('/api/v1/documents/:documentId/schemas', getMockDocumentSchemaApiResponse)
    );
  });

  it('should be rendered without errors', async () => {
    expect(() => {
      render(<DocumentEditorPage history={history} location={history.location} match={match} />);
    }).not.toThrow();
  });

  it('should be render a cms editor', async () => {
    render(<DocumentEditorPage history={history} location={history.location} match={match} />);

    await waitFor(() => screen.getByTitle('ROOT_CONTEXT'));

    expect(screen.getByTitle('ROOT_CONTEXT')).toBeInTheDocument();
  });
});
