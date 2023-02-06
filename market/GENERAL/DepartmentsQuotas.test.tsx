import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { DepartmentsQuotasPage } from './DepartmentsQuotasPage';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('DepartmentsQuotasPage', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<DepartmentsQuotasPage />', () => {
    it('should be render without errors', () => {
      wrapper = mount(
        <Wrapper>
          <DepartmentsQuotasPage />
        </Wrapper>
      );
      expect(() => wrapper).not.toThrow();
    });
  });
});
