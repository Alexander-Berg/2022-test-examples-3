import React from 'react';

import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import { Router } from 'react-router-dom';
import { createMemoryHistory } from 'history';
import { setupServer } from 'msw/node';
import intersectionObserverMock from 'services/IntersectionWatcher/__mock__/intersectionObserverMock';
import { handlers } from './__mocks__/handlers';
import { tabsData } from './__mocks__/tabsData';
import { Tabs } from './Tabs';
jest.mock('utils/hooks/useBunker', () => ({
  useBunker: () => ({ isFetched: true, data: undefined }),
}));
window.IntersectionObserver = intersectionObserverMock;

const server = setupServer(...handlers);

const history = createMemoryHistory();
history.push('/lift');

describe('Tabs(DTSLift)', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  describe('when has data', () => {
    beforeEach(() => {
      render(
        <Router history={history}>
          <Tabs tabs={tabsData} />
        </Router>,
      );
    });

    afterEach(() => {
      cleanup();
    });

    it('renders tabs', async () => {
      await waitFor(() => {
        expect(screen.getByText('TestCaption1')).toBeInTheDocument();
        expect(screen.getByText('TestCaption2')).toBeInTheDocument();
      });
    });

    it('shows active tab1', async () => {
      await waitFor(() => {
        expect(screen.getByText('Text Value 1')).toBeInTheDocument();
      });
    });

    it('changes active tab1 to tab2', async () => {
      fireEvent.click(screen.getByText('TestCaption2'));
      await waitFor(() => {
        expect(screen.getByText('Text Value 2')).toBeInTheDocument();
      });
    });
  });
  describe('when has not data', () => {
    it('does not throw error', async () => {
      expect(() =>
        render(
          <Router history={history}>
            <Tabs />
          </Router>,
        ),
      ).not.toThrow();
    });
  });
});
