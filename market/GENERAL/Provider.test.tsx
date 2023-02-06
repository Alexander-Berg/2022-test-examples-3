import { mount, ReactWrapper } from 'enzyme';
import { createMemoryHistory, MemoryHistory, MemoryHistoryBuildOptions } from 'history';
import React from 'react';
import { act } from 'react-dom/test-utils';

import { IQueryParamsComponentProps, IQueryParamsProviderProps, QueryParamsProvider, withQueryParams } from '..';

let wrapper: ReactWrapper;
let componentProps: IQueryParamsComponentProps;
let history: MemoryHistory<any>;

const setup = (
  options?: MemoryHistoryBuildOptions,
  providerProps?: Pick<IQueryParamsProviderProps, 'parseOptions' | 'stringifyOptions'>
) => {
  history = createMemoryHistory(options);

  const TestComp = withQueryParams()(p => {
    componentProps = p;

    return null;
  });

  wrapper = mount(
    <QueryParamsProvider history={history} {...providerProps}>
      <TestComp />
    </QueryParamsProvider>
  );
};

afterEach(() => {
  if (wrapper) {
    wrapper.unmount();
  }

  componentProps = undefined as any;
  history = undefined as any;
});

describe('<QueryParamsProvider />', () => {
  it('should correctly parse array with none format option', () => {
    setup({ initialEntries: ['/?a=2&a=3&a=1'] }, { parseOptions: { arrayFormat: 'none' } });

    expect(componentProps.queryParams).toEqual({ a: ['2', '3', '1'] });
  });

  it('should correctly parse array with bracket format option', () => {
    setup({ initialEntries: ['/?a[]=2&a[]=3&a[]=1'] }, { parseOptions: { arrayFormat: 'bracket' } });

    expect(componentProps.queryParams).toEqual({ a: ['2', '3', '1'] });
  });

  it('should correctly parse array with indexed format option', () => {
    setup({ initialEntries: ['/?a[0]=2&a[1]=3&a[2]=1'] }, { parseOptions: { arrayFormat: 'index' } });

    expect(componentProps.queryParams).toEqual({ a: ['2', '3', '1'] });
  });

  it('should correctly parse array with comma format option', () => {
    setup({ initialEntries: ['/?a=2,3,1'] }, { parseOptions: { arrayFormat: 'comma' } });

    expect(componentProps.queryParams).toEqual({ a: ['2', '3', '1'] });
  });

  it('should correctly stringify array with none format option', () => {
    setup({ initialEntries: ['/'] }, { stringifyOptions: { arrayFormat: 'none' } });

    act(() => componentProps.onChangeQueryParams({ a: ['2', '3', '1'] }));

    expect(history.location.search).toBe('?a=2&a=3&a=1');
  });

  it('should correctly stringify array with bracket format option', () => {
    setup({ initialEntries: ['/'] }, { stringifyOptions: { arrayFormat: 'bracket' } });

    act(() => componentProps.onChangeQueryParams({ a: ['2', '3', '1'] }));

    expect(history.location.search).toBe('?a[]=2&a[]=3&a[]=1');
  });

  it('should correctly stringify array with indexed format option', () => {
    setup({ initialEntries: ['/'] }, { stringifyOptions: { arrayFormat: 'index' } });

    act(() => componentProps.onChangeQueryParams({ a: ['2', '3', '1'] }));

    expect(history.location.search).toBe('?a[0]=2&a[1]=3&a[2]=1');
  });

  it('should correctly stringify array with comma format option', () => {
    setup({ initialEntries: ['/'] }, { stringifyOptions: { arrayFormat: 'comma' } });

    act(() => componentProps.onChangeQueryParams({ a: ['2', '3', '1'] }));

    expect(history.location.search).toBe('?a=2,3,1');
  });
});
