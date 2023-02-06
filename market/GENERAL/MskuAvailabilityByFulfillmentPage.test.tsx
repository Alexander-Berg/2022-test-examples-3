import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { MskuAvailabilityByFulfillmentPage } from '.';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('MskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<MskuAvailabilityByFulfillmentPage />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(
          <Wrapper>
            <MskuAvailabilityByFulfillmentPage />
          </Wrapper>
        );
      }).not.toThrow();
    });
  });
});
