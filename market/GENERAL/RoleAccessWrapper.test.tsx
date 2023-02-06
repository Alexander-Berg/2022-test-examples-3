import React from 'react';
import { render } from '@testing-library/react';

import { getAllAbsent, hasAnyOf } from '.';
import { RoleAccessWrapper } from './RoleAccessWrapper';
import { ConfigContext } from 'src/context/ConfigContext';
import { CurrentUserContext } from 'src/context/CurrentUserContext';
import { testFrontendConfig } from 'test/data/frontendConfig';

describe('Role array intersection test', () => {
  it('should get absent from "all of"', () => {
    expect(getAllAbsent(['1', '2', '3'], ['5'])).toEqual(['5']);

    expect(getAllAbsent(['1', '2', '3'], ['8'])).toEqual(['8']);

    expect(getAllAbsent(['1', '2', '3'], ['2'])).toEqual([]);

    expect(getAllAbsent(['1', '2', '3'], ['3', '1'])).toEqual([]);
  });

  it('should get all absent from "any of"', () => {
    expect(hasAnyOf(['1', '2', '3'], ['5', '6'])).toBe(false);

    expect(hasAnyOf(['1', '2', '3'], ['5'])).toBe(false);

    expect(hasAnyOf(['1', '2', '3'], ['2', '6'])).toBe(true);

    expect(hasAnyOf(['1', '2', '3'], ['1'])).toBe(true);
  });

  it('renders without errors', () => {
    // eslint-disable-next-line no-console
    console.error = jest.fn();
    const children = jest.fn(disabled => disabled);
    expect(() => {
      render(
        <ConfigContext.Provider value={testFrontendConfig()}>
          <CurrentUserContext.Provider value={{ login: 'testik', roles: [] }}>
            <RoleAccessWrapper>{children}</RoleAccessWrapper>
          </CurrentUserContext.Provider>
        </ConfigContext.Provider>
      );
    }).toThrow('Не указан набор ролей');

    render(
      <ConfigContext.Provider value={testFrontendConfig()}>
        <CurrentUserContext.Provider value={{ login: 'testik', roles: ['INTERNAL_PROCESS'] }}>
          <RoleAccessWrapper anyOf={['MANAGE_SEASONS', 'VIEWER']}>{children}</RoleAccessWrapper>
        </CurrentUserContext.Provider>
      </ConfigContext.Provider>
    );
    expect(children).toHaveLastReturnedWith(true);

    render(
      <ConfigContext.Provider value={testFrontendConfig()}>
        <CurrentUserContext.Provider value={{ login: 'testik', roles: ['VIEWER'] }}>
          <RoleAccessWrapper anyOf={['MANAGE_SEASONS', 'VIEWER']}>{children}</RoleAccessWrapper>
        </CurrentUserContext.Provider>
      </ConfigContext.Provider>
    );
    expect(children).toHaveLastReturnedWith(false);
  });
});
