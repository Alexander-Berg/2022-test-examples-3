import { IQueryConfig, numberType, stringType } from '@yandex-market/typesafe-query';
import { mount, ReactWrapper } from 'enzyme';
import { createMemoryHistory, MemoryHistoryBuildOptions } from 'history';
import React, { FC, ReactElement, useEffect } from 'react';
import { act } from 'react-dom/test-utils';

import { QueryParamsProvider, useQueryParams } from '..';

let wrapper: ReactWrapper | null = null;
const setup = (comp: ReactElement, options?: MemoryHistoryBuildOptions) => {
  const history = createMemoryHistory(options);

  wrapper = mount(<QueryParamsProvider history={history}>{comp}</QueryParamsProvider>);

  return { history };
};

let hook: any;

describe('useQueryParams', () => {
  beforeEach(() => {
    hook = [null, null];
  });

  afterEach(() => {
    if (wrapper !== null) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('Provides spec data and updater', () => {
    const Component: FC = () => {
      hook = useQueryParams({});

      return null;
    };
    setup(<Component />);

    expect(typeof hook[0]).toBe('object');
    expect(typeof hook[1]).toBe('function');
  });

  describe('Should work with config', () => {
    interface Params {
      a?: string;
      b?: number;
    }

    const config: IQueryConfig<Params> = {
      a: stringType(),
      b: numberType(),
    };

    const init = (url: string) => {
      const renderNum = { last: 0 };
      const Component: FC = () => {
        hook = useQueryParams(config);
        renderNum.last += 1;

        return null;
      };
      const { history } = setup(<Component />, { initialEntries: [url] });

      return { renderNum, Component, history };
    };

    it('Should init properly for empty url', () => {
      init('');
      expect(hook[0]).toEqual({});
    });

    it('Should init properly for non-empty url', () => {
      init('/?a=test&b=123&c=nop');
      expect(hook[0]).toEqual({ a: 'test', b: 123 });
    });

    it("Shouldn't change params when required params aren't changed", () => {
      const { history, renderNum } = init('/?a=test&b=123&c=nop');
      const lastRender = renderNum.last;
      const hookBefore = [...hook];

      act(() => history.push({ search: 'a=test&b=123&c=other' }));

      expect(renderNum.last).toEqual(lastRender + 1);
      expect(hook[0]).toBe(hookBefore[0]);
      expect(hook[1]).toBe(hookBefore[1]);
    });

    it('Should replace params in query', () => {
      const { history } = init('/?a=test&b=123&c=nop');
      expect(hook[0]).toEqual({ a: 'test', b: 123 });

      act(() => hook[1]({ a: 'something' }));

      expect(history.location.search).toEqual('?a=something&c=nop');
      expect(hook[0]).toEqual({ a: 'something' });
    });

    it('should correct batch update with difference configs', () => {
      const config1 = { a: stringType() };
      const config2 = { b: numberType() };

      let update: () => void;
      let paramsA: any;
      let paramsB: any;

      const Component: FC = () => {
        const [pA, setA] = useQueryParams(config1);
        const [pB, setB] = useQueryParams(config2);

        paramsA = pA;
        paramsB = pB;
        update = () => {
          setA({ a: 'cba' });
          setB({ b: 321 });
        };

        return null;
      };

      const { history } = setup(<Component />, { initialEntries: ['/?a=abc&b=123'] });

      act(() => update!());

      expect(paramsA).toEqual({ a: 'cba' });
      expect(paramsB).toEqual({ b: 321 });
      expect(history.location.search).toBe('?a=cba&b=321');
    });

    it('should correct batch update with same config', () => {
      const conf = { a: stringType(), b: numberType() };

      let update: () => void;
      let params: any;

      const Component: FC = () => {
        const [p, set] = useQueryParams(conf);
        params = p;
        update = () => {
          set({ a: 'cba' }, 'pushIn');
          set({ b: 321 }, 'pushIn');
        };

        return null;
      };

      const { history } = setup(<Component />, { initialEntries: ['/?a=abc&b=123'] });

      act(() => update!());

      expect(params).toEqual({ a: 'cba', b: 321 });
      expect(history.location.search).toBe('?a=cba&b=321');
    });

    it('should update params on first call useEffect', () => {
      let params: Params;
      const Component: FC = () => {
        const [queryParams, setQueryParams] = useQueryParams(config);
        params = queryParams;

        useEffect(() => {
          setQueryParams({ a: '123' });
        }, [setQueryParams]);

        return null;
      };
      const { history } = setup(<Component />, { initialEntries: ['/'] });

      expect(history.location.search).toBe('?a=123');
      expect(params!).toHaveProperty('a', '123');
    });
  });
});
