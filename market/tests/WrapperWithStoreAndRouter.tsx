import React from 'react';
import { Provider } from 'react-redux';
import { Store } from 'redux';
import { AnyAction } from 'typescript-fsa';
import { MemoryRouter } from 'react-router-dom';

interface Props {
  store: Store<any, AnyAction>;
  children: React.ReactNode;
}
export const WrapperWithStoreAndRouter = ({ store, children }: Props) => {
  return (
    <MemoryRouter>
      <Provider store={store}>{children}</Provider>;
    </MemoryRouter>
  );
};
