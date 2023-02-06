import { render, cleanup, screen } from '@testing-library/react/pure';
import React from 'react';
import { XivaContext, CRMXiva } from 'modules/xiva';
import { getIsSupportedBrowser } from 'services/SupportedBrowsers';
import { mocked } from 'ts-jest';
import App from '../App';
import { TestBed } from '../TestBed';

const xiva = new EventTarget() as CRMXiva;

jest.mock('services/SupportedBrowsers/getIsSupportedBrowser', () => ({
  getIsSupportedBrowser: jest.fn(),
}));

const mockGetIsSupportedBrowser = mocked(getIsSupportedBrowser);

const WrappedApp = () => (
  <TestBed>
    <XivaContext.Provider value={xiva}>
      <App />
    </XivaContext.Provider>
  </TestBed>
);

describe('App', () => {
  beforeAll(() => {
    Object.defineProperty(global, '__APP_VERSION__', {
      get() {
        return 'test';
      },
    });
  });

  describe('when browser is not supported', () => {
    it('renders stub', () => {
      mockGetIsSupportedBrowser.mockReturnValueOnce(false);

      render(<WrappedApp />);

      expect(screen.queryByTestId('unsupported-browser')).toBeVisible();

      cleanup();
    });
  });

  describe('when browser is supported', () => {
    it('does not render stub', () => {
      mockGetIsSupportedBrowser.mockReturnValueOnce(true);

      render(<WrappedApp />);

      expect(screen.queryByTestId('unsupported-browser')).not.toBeInTheDocument();

      cleanup();
    });
  });
});
