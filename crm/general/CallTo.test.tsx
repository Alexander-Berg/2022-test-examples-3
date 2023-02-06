import React from 'react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { render, screen, fireEvent } from '@testing-library/react';
import { fixGlobalHandleErrorEventsForTests } from 'utils/fixGlobalHandleErrorEventsForTests';
import { CallTo } from './CallTo';
import { errors } from './CallTo.config';

const callAvailableService = {
  canCall: () => Promise.resolve({ canCall: true }),
};

const callMetaService = {
  getCallMeta: () => Promise.resolve({ caller: '', associatedUnitId: '' }),
};

const reduxStore = configureStore({ reducer: () => ({}) });

interface RenderCallToTestUIOptions {
  callee: string;
}

const renderCallToTestUI = (options: RenderCallToTestUIOptions) => {
  return render(
    <Provider store={reduxStore}>
      <CallTo
        callee={options.callee}
        as="button"
        callAvailableService={callAvailableService}
        callMetaService={callMetaService}
      >
        call
      </CallTo>
    </Provider>,
  );
};

/* https://st.yandex-team.ru/CRM-13387 */
// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('CallTo', () => {
  describe('on click to call', () => {
    describe('when callee is "-"', () => {
      fixGlobalHandleErrorEventsForTests();

      it('throws error', (done) => {
        process._original().on('unhandledRejection', (error: Error) => {
          expect(error.message).toBe(errors.wrongNumber);
          done();
        });

        renderCallToTestUI({ callee: '-' });

        fireEvent.click(screen.getByRole('button'));
      });
    });

    describe('when webphone not init', () => {
      fixGlobalHandleErrorEventsForTests();

      it('throws error', (done) => {
        process._original().on('unhandledRejection', (error: Error) => {
          expect(error.message).toBe(errors.webphoneNotInit);
          done();
        });

        renderCallToTestUI({ callee: '+79876543210' });

        fireEvent.click(screen.getByRole('button'));
      });
    });
  });
});
