import { IQueryConfig, QueryTypes } from '@yandex-market/typesafe-query';
import { mount, ReactWrapper } from 'enzyme';
import { createMemoryHistory, MemoryHistoryBuildOptions } from 'history';
import React, { ReactElement } from 'react';
import { act } from 'react-dom/test-utils';

import { QueryParamsProvider, withQueryParams } from '..';

let wrapper: ReactWrapper;
const setup = (comp: ReactElement, options?: MemoryHistoryBuildOptions) => {
  const history = createMemoryHistory(options);

  wrapper = mount(<QueryParamsProvider history={history}>{comp}</QueryParamsProvider>);

  return { history };
};

describe('WithQueryParams', () => {
  it('provides { queryParams, onChangeQueryParams } props', () => {
    let props: any;
    const PropsChecker = withQueryParams()(p => {
      props = p;

      return null;
    });

    setup(<PropsChecker />);

    expect(typeof props).toBe('object');
    expect(typeof props.queryParams).toBe('object');
    expect(typeof props.onChangeQueryParams).toBe('function');

    wrapper.unmount();
  });

  describe('onChangeQueryParams', () => {
    describe('without config', () => {
      let props: any;
      let history: any;

      beforeEach(() => {
        const TestComp = withQueryParams()(p => {
          props = p;

          return null;
        });

        const params = setup(<TestComp />, {
          initialEntries: ['/?foo=bar'],
        });

        history = params.history;
      });

      afterEach(() => {
        wrapper.unmount();
      });

      it('should pass an object with current query parameters', () => {
        expect(props.queryParams).toEqual({ foo: 'bar' });
      });

      it('should update all query parameters and increment history length', () => {
        act(() => props.onChangeQueryParams({ some: 'value' }, 'push'));

        expect(props.queryParams).toEqual({ some: 'value' });
        expect(history.location.search).toBe('?some=value');
        expect(history).toHaveLength(2);
      });

      it("should update all query parameters and don't change history length", () => {
        act(() => props.onChangeQueryParams({ some: 'value' }, 'replace'));

        expect(props.queryParams).toEqual({ some: 'value' });
        expect(history.location.search).toBe('?some=value');
        expect(history).toHaveLength(1);
      });

      it('should update part of query parameters and increment history length', () => {
        act(() => props.onChangeQueryParams({ some: 'value' }, 'pushIn'));

        expect(props.queryParams).toEqual({ foo: 'bar', some: 'value' });
        expect(history.location.search).toBe('?foo=bar&some=value');
        expect(history).toHaveLength(2);
      });

      it("should update part of query parameters and don't change history length", () => {
        act(() => props.onChangeQueryParams({ some: 'value' }, 'replaceIn'));

        expect(props.queryParams).toEqual({ some: 'value', foo: 'bar' });
        expect(history.location.search).toBe('?foo=bar&some=value');
        expect(history).toHaveLength(1);
      });

      it('should clean query parameters', () => {
        act(() => props.onChangeQueryParams({}, 'push'));

        expect(props.queryParams).toEqual({});
        expect(history.location.search).toBe('');
        expect(history).toHaveLength(2);
      });
    });

    describe('with config', () => {
      let props: any;
      let history: any;

      beforeEach(() => {
        interface TestQueryParams {
          a?: string;
          b?: number;
        }

        const config: IQueryConfig<TestQueryParams> = {
          a: {
            type: QueryTypes.string,
          },
          b: {
            type: QueryTypes.number,
          },
        };

        const TestComp = withQueryParams(config)(p => {
          props = p;

          return null;
        });

        const params = setup(<TestComp />, {
          initialEntries: ['?foo=bar&a=some'],
        });

        history = params.history;
      });

      afterEach(() => {
        wrapper.unmount();
      });

      it('should pass an object with current query parameters of config', () => {
        expect(props.queryParams).toEqual({ a: 'some' });
      });

      it('should update query parameters and increment history length', () => {
        act(() => props.onChangeQueryParams({ b: 100 }, 'push'));

        expect(props.queryParams).toEqual({ b: 100 });
        expect(history.location.search).toBe('?b=100&foo=bar');
        expect(history).toHaveLength(2);
      });

      it("should update query parameters and don't change history length", () => {
        act(() => props.onChangeQueryParams({ b: 100 }, 'replace'));

        expect(props.queryParams).toEqual({ b: 100 });
        expect(history.location.search).toBe('?b=100&foo=bar');
        expect(history).toHaveLength(1);
      });

      it('should update part of query parameters and increment history length', () => {
        act(() => props.onChangeQueryParams({ b: 100 }, 'pushIn'));

        expect(props.queryParams).toEqual({ a: 'some', b: 100 });
        expect(history.location.search).toBe('?a=some&b=100&foo=bar');
        expect(history).toHaveLength(2);
      });

      it("should update part of query parameters and don't change history length", () => {
        act(() => props.onChangeQueryParams({ b: 100 }, 'replaceIn'));

        expect(props.queryParams).toEqual({ a: 'some', b: 100 });
        expect(history.location.search).toBe('?a=some&b=100&foo=bar');
        expect(history).toHaveLength(1);
      });

      it('should clean part of query parameters', () => {
        act(() => props.onChangeQueryParams({}, 'push'));

        expect(props.queryParams).toEqual({});
        expect(history.location.search).toBe('?foo=bar');
        expect(history).toHaveLength(2);
      });
    });

    describe('few components', () => {
      let props1: any;
      let props2: any;
      let props3: any;

      beforeEach(() => {
        const enhancer = withQueryParams();

        const TestComp1 = enhancer(p => {
          props1 = p;

          return null;
        });

        const TestComp2 = enhancer(p => {
          props2 = p;

          return null;
        });

        const TestComp3 = withQueryParams<{ foo?: string }>({ foo: { type: QueryTypes.string } })(p => {
          props3 = p;

          return null;
        });

        setup(
          <>
            <TestComp1 />
            <TestComp2 />
            <TestComp3 />
          </>,
          {
            initialEntries: ['?foo=bar'],
          }
        );
      });

      afterEach(() => {
        wrapper.unmount();
      });

      it('should pass an object with current query parameters to all components', () => {
        expect(props1.queryParams).toEqual({ foo: 'bar' });
        expect(props2.queryParams).toEqual({ foo: 'bar' });
        expect(props3.queryParams).toEqual({ foo: 'bar' });
      });

      it('should update query parameters for all components', () => {
        act(() => props1.onChangeQueryParams({ some: 'value' }, 'push'));

        expect(props1.queryParams).toEqual({ some: 'value' });
        expect(props2.queryParams).toEqual({ some: 'value' });
        expect(props3.queryParams).toEqual({});

        act(() => props2.onChangeQueryParams({ foo: 'bar' }, 'pushIn'));

        expect(props1.queryParams).toEqual({ foo: 'bar', some: 'value' });
        expect(props2.queryParams).toEqual({ foo: 'bar', some: 'value' });
        expect(props3.queryParams).toEqual({ foo: 'bar' });
      });

      it('not rerender component', () => {
        expect(props1.queryParams).toEqual({ foo: 'bar' });
        expect(props2.queryParams).toEqual({ foo: 'bar' });
        expect(props3.queryParams).toEqual({ foo: 'bar' });

        const prev3QueryParams = props3.queryParams;
        act(() => props1.onChangeQueryParams({ some: 'value' }, 'pushIn'));

        expect(props1.queryParams).toEqual({ foo: 'bar', some: 'value' });
        expect(props3.queryParams).toBe(prev3QueryParams);

        const prevQueryParams = props2.queryParams;
        act(() => props2.onChangeQueryParams({ some: 'value' }, 'pushIn'));

        expect(props1.queryParams).toBe(prevQueryParams);
        expect(props2.queryParams).toBe(prevQueryParams);
        expect(props1.queryParams).toBe(props2.queryParams);
      });
    });
  });
});
