import React, { FC } from 'react';
import { Provider } from 'react-redux';
import { createStore, Reducer, Store } from 'redux';

import { AliasMakerContext } from 'src/shared/common-logs/context/AliasMakerContext';
import { AliasMaker } from 'src/shared/services';

export const getLogsWrapper = (aliasMaker: Partial<AliasMaker>, store?: Store) => {
  return ({ children }: any) => (
    <Provider store={store || createStore(jest.fn())}>
      <AliasMakerContext.Provider value={aliasMaker as AliasMaker}>{children}</AliasMakerContext.Provider>
    </Provider>
  );
};

interface WrapperProps {
  aliasMaker?: Partial<AliasMaker>;
  initialState?: any;
  reducer?: Reducer<any>;
}

export const Wrapper: FC<WrapperProps> = ({
  aliasMaker = {},
  reducer = state => state,
  initialState = {},
  children,
}) => {
  return (
    <Provider store={createStore(reducer, initialState)}>
      <AliasMakerContext.Provider value={aliasMaker as AliasMaker}>{children}</AliasMakerContext.Provider>
    </Provider>
  );
};

export const getWrapper = (props: WrapperProps) => {
  return ({ children }: any) => <Wrapper {...props}>{children}</Wrapper>;
};
