import React from 'react';
import { createMemoryHistory } from 'history';
import { Router } from 'react-router-dom';
import { render, screen, fireEvent } from '@testing-library/react';
import { LinkTabs } from '../LinkTabs';
import { LinkTab } from '../LinkTab';

const withDefaultRouter = (content, history = createMemoryHistory()) => {
  return <Router history={history}>{content}</Router>;
};

describe('LinkTabs', () => {
  it('should support headerLeft prop', () => {
    render(
      withDefaultRouter(
        <LinkTabs headerLeft="headerLeft">
          <LinkTab path="/" title="Tab 1">
            Tab 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Tab 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    screen.getByText('headerLeft');
  });

  it('should support title prop', () => {
    render(
      withDefaultRouter(
        <LinkTabs title="title">
          <LinkTab path="/" title="Tab 1">
            Tab 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Tab 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    screen.getByText('title');
  });

  it('should support content prop', () => {
    render(
      withDefaultRouter(
        <LinkTabs content="content">
          <LinkTab path="/" title="Tab 1">
            Tab 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Tab 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    screen.getByText('content');
  });

  it('should render two tab links', () => {
    render(
      withDefaultRouter(
        <LinkTabs>
          <LinkTab path="/" title="Tab 1">
            Content 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Content 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    const buttons = screen.getAllByRole('link');

    expect(buttons).toHaveLength(2);
    expect(buttons[0]).toHaveTextContent('Tab 1');
    expect(buttons[1]).toHaveTextContent('Tab 2');
  });

  it('should display first tab by default', () => {
    render(
      withDefaultRouter(
        <LinkTabs>
          <LinkTab path="/" title="Tab 1">
            Content 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Content 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    screen.getByText('Content 1');
  });

  it('should support switch tab', () => {
    render(
      withDefaultRouter(
        <LinkTabs>
          <LinkTab path="/" title="Tab 1">
            Content 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Content 2
          </LinkTab>
        </LinkTabs>,
      ),
    );

    fireEvent.click(screen.getByText('Tab 2'));
    screen.getByText('Content 2');
  });

  it('should support redirect url', () => {
    const history = createMemoryHistory();

    history.push('/404');

    render(
      withDefaultRouter(
        <LinkTabs redirectUrl="/2">
          <LinkTab path="/" title="Tab 1">
            Content 1
          </LinkTab>
          <LinkTab path="/2" title="Tab 2">
            Content 2
          </LinkTab>
        </LinkTabs>,
        history,
      ),
    );

    screen.getByText('Content 2');
  });
});
