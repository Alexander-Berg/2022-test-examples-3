import React from 'react';
import { createMemoryHistory } from 'history';
import { Router } from 'react-router-dom';
import { render } from '@testing-library/react';
import createLink from 'modules/issues/utils/createLink';
import { LinkToParentIssue } from './LinkToParentIssue';

describe('LinkToParentIssue', () => {
  describe('if issue is not on screen by link', () => {
    it('renders link', () => {
      const history = createMemoryHistory();
      const { container } = render(
        <Router history={history}>
          <LinkToParentIssue issue={{ id: 1, typeId: 2 }} className="className" target="_black" />
        </Router>,
      );

      expect(container).toMatchSnapshot();
    });
  });

  describe('if issue is on screen by link', () => {
    it('renders no link', () => {
      const history = createMemoryHistory();
      history.push(createLink({ id: 1, typeId: 2, hash: false }));

      const { container } = render(
        <Router history={history}>
          <LinkToParentIssue issue={{ id: 1, typeId: 2 }} className="className" target="_black" />
        </Router>,
      );

      expect(container).toMatchSnapshot();
    });
  });
});
