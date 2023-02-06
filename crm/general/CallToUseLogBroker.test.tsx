import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ETypeString } from 'types/entities';
import { callService } from 'services/CallService';
import { mocked } from 'ts-jest/utils';
import { CallToUseLogBroker } from './CallToUseLogBroker';
import { IssueIdContext } from '../Issue/IssueIdContext';

jest.mock('services/CallService');

const mockedCallService = mocked(callService);

describe('CallToUseLogBroker', () => {
  beforeEach(() => {
    mockedCallService.call.mockClear();
  });

  const renderTestSetup = () => {
    const children = 'children';

    render(
      <IssueIdContext.Provider value={1}>
        <CallToUseLogBroker
          phone="phone"
          phoneData={{ id: 2, phone: 'phone', phoneExt: 'phoneExt', phoneE164: 'phoneE164' }}
          cardData={{ accountKikId: 3 }}
        >
          {children}
        </CallToUseLogBroker>
      </IssueIdContext.Provider>,
    );

    return { children };
  };

  it('renders component with children', async () => {
    const { children } = renderTestSetup();

    await waitFor(() => expect(screen.queryByText(children)).toBeInTheDocument());
  });

  describe('when clicked', function() {
    it('inits create call', async () => {
      const { children } = renderTestSetup();

      const link = screen.getByText(children);

      fireEvent.click(link);

      expect(mockedCallService.call).toBeCalledTimes(1);
      expect(mockedCallService.call).toBeCalledWith({
        kikId: 2,
        source: { eid: 1, etype: ETypeString.Issue },
      });
    });
  });
});
