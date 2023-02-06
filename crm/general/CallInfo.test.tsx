import React from 'react';
import { Call, CallOptions } from 'types/entities/call';
import { render, screen } from '@testing-library/react';
import CallInfo from './CallInfo';
import { getFullList } from './utils/getFieldsList';

jest.mock('components/AudioSource', () => ({
  AudioSource: () => null,
}));

const createCallStubForOptionsTest = (options?: Partial<CallOptions>): Call => {
  const { callId, callStatus, upsaleFactors, tags }: CallOptions = {
    callStatus: { id: 1, name: 'В процессе' },
    callId: '',
    ...options,
  };

  return {
    id: 1,
    author: {
      id: 10,
      login: 'login',
      name: 'author',
      phones: {
        work: '+7',
      },
    },
    eType: 'YcCall',
    dt: '',
    duration: 100,
    recordId: 'recordId',
    callId,
    operatorPhoneNumber: '+7',
    direction: {
      id: 1,
      name: 'Входящий',
    },
    callStatus,
    kik: {
      id: 1,
      name: 'kik name',
      firstName: 'kik firstName',
      phone: '+7',
      phoneE164: '+7',
    },
    endReason: null,
    categorization: { categories: [], selectedCategories: {} },
    upsaleFactors,
    tags,
    issues: [],
  };
};

describe('CallInfo', () => {
  describe('props.call', () => {
    describe('when callStatus.id is 2', () => {
      describe('props.preview', () => {
        describe('when is true', () => {
          it('renders CallPreview', () => {
            const call: Call = createCallStubForOptionsTest({
              callStatus: { id: 2, name: 'Завершён' },
            });
            render(<CallInfo call={call} isTest view="full" preview />);

            const callPreview = screen.getByTestId('call-preview');

            expect(callPreview).toBeInTheDocument();
          });
        });

        describe('when is false', () => {
          it('renders fields list', () => {
            const call: Call = createCallStubForOptionsTest({
              callStatus: { id: 2, name: 'Завершён' },
            });
            render(<CallInfo call={call} isTest view="full" preview={false} />);

            const fieldsList = screen.queryByTestId('fields-list');
            expect(fieldsList).toBeInTheDocument();

            const fullFieldsList = getFullList(call, jest.fn(), undefined, undefined);
            expect(fieldsList?.childElementCount).toBe(fullFieldsList.length);
          });

          it('renders callId', () => {
            const call: Call = createCallStubForOptionsTest({ callId: 'callId' });
            render(<CallInfo call={call} isTest view="full" preview={false} />);

            expect(screen.getByText('callId')).toBeInTheDocument();
          });
        });
      });
    });

    describe("when callStatus.id isn't 2", () => {
      it("doesn't render CallPreview", () => {
        const call: Call = createCallStubForOptionsTest({
          callStatus: { id: 1, name: 'В процессе' },
        });
        render(<CallInfo call={call} isTest view="full" preview />);

        const callPreview = screen.queryByTestId('call-preview');

        expect(callPreview).not.toBeInTheDocument();
      });
    });
  });

  describe('render upsale', () => {
    it('should no error without factors', () => {
      const call: Call = createCallStubForOptionsTest();

      expect(() => {
        render(<CallInfo call={call} isTest view="full" preview={false} />);
      }).not.toThrow();
    });

    it('should render factors in full view', () => {
      const call: Call = createCallStubForOptionsTest({
        upsaleFactors: [{ id: 1, name: 'factor' }],
      });
      render(<CallInfo call={call} isTest view="full" preview={false} />);

      expect(() => {
        screen.getByText('factor');
      }).not.toThrow();
    });

    it('should render factors in regular view', () => {
      const call: Call = createCallStubForOptionsTest({
        upsaleFactors: [{ id: 1, name: 'factor' }],
      });
      render(<CallInfo call={call} isTest view="regular" preview={false} />);

      expect(() => {
        screen.getByText('factor');
      }).not.toThrow();
    });

    it('should render factors in preview view', () => {
      const call: Call = createCallStubForOptionsTest({
        upsaleFactors: [{ id: 1, name: 'factor' }],
      });
      render(<CallInfo call={call} isTest view="regular" preview />);

      expect(() => {
        screen.getByText('factor');
      }).not.toThrow();
    });

    it('should render factors in account history view', () => {
      const call: Call = createCallStubForOptionsTest({
        upsaleFactors: [{ id: 1, name: 'factor' }],
      });
      render(<CallInfo call={call} isTest view="accountHistory" preview={false} />);

      expect(() => {
        screen.getByText('factor');
      }).not.toThrow();
    });
  });

  it('renders tags', () => {
    const call: Call = createCallStubForOptionsTest({
      tags: [
        { id: 1, name: 'Tag 1', color: '#000' },
        { id: 2, name: 'Tag 2', color: '#000' },
      ],
    });
    render(<CallInfo call={call} isTest view="accountHistoryPersonal" preview={false} />);

    expect(screen.queryByText('Tag 1')).toBeInTheDocument();
    expect(screen.queryByText('Tag 2')).toBeInTheDocument();
  });
});
