import React from 'react';
import { Provider } from 'react-redux';
import { Store } from 'redux';
import { AnyAction } from 'typescript-fsa';

interface Props {
  store: Store<any, AnyAction>;
  children: React.ReactNode;
}
export const WrapperWithStore = ({ store, children }: Props) => {
  return <Provider store={store}>{children}</Provider>;
};
