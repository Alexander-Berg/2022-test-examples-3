import { createMemoryHistory } from 'history';
import { Route, Router, useHistory } from 'react-router-dom';
import React, { useEffect } from 'react';
import { act, render } from '@testing-library/react';
import { ForceChangeLocationFlag } from './ForceChangeLocationFlag';

const forceChangeLocationFlagLogValue = jest.fn();
const history = createMemoryHistory();
const forceChangeLocationFlag = new ForceChangeLocationFlag(history);
const Component = () => {
  const history = useHistory();

  useEffect(() => {
    return () => {
      forceChangeLocationFlagLogValue(forceChangeLocationFlag.current);
    };
  }, [history]);

  return null;
};

describe('ForceChangeLocationFlag', () => {
  beforeEach(() => {
    forceChangeLocationFlagLogValue.mockClear();
  });

  describe('after force push', () => {
    it('is true', () => {
      history.push('/path');
      render(
        <Router history={history}>
          <Route path="/path" exact>
            <Component />
          </Route>
        </Router>,
      );

      act(() => {
        forceChangeLocationFlag.setFlagForNextUpdate(true);
        history.push('/');
      });

      expect(forceChangeLocationFlagLogValue).toBeCalledWith(true);
    });
  });

  describe('after normal push', () => {
    it('is false', () => {
      history.push('/');
      render(
        <Router history={history}>
          <Route path="/path" exact>
            <Component />
          </Route>
        </Router>,
      );

      act(() => {
        forceChangeLocationFlag.setFlagForNextUpdate(true);
        history.push('/path0');
      });

      act(() => {
        history.push('/path');
      });

      act(() => {
        history.push('/path0');
      });

      expect(forceChangeLocationFlagLogValue).toBeCalledWith(false);
    });
  });
});
