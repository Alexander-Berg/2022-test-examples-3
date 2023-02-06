import { render, waitFor, screen, fireEvent, cleanup, act } from '@testing-library/react/pure';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { TestBed } from 'components/TestBed';
import { runPendingPromises } from 'utils/runPendingPromises';
import { NewMailForm as NewMailFormLoadableBundle } from '../NewMailForm.bundle/loadable';
import { AUTOSAVE_DELAY } from '../NewMailForm.config';

jest.mock('components/RichHtmlEditor');

const onLoad = async () => {
  return {
    froms: ['crm@yandex-team.ru', 'crm2@yandex-team.ru'],
    signatures: [
      { signatureId: 1, name: 'test 1', bodyHtml: 'test 1', bodyPlain: 'test 1', isDefault: true },
      { signatureId: 2, name: 'test 2', bodyHtml: 'test 2', bodyPlain: 'test 2', isDefault: false },
    ],
    templates: [
      {
        bodyHtml: 'bodyHtml 1',
        bodyPlain: 'bodyPlain 2',
        name: 'name 1',
        orderPosition: 0,
        templateId: 1,
        type: 'private',
        typeName: 'private',
      },
      {
        bodyHtml: 'bodyHtml 2',
        bodyPlain: 'bodyPlain 2',
        name: 'name 2',
        orderPosition: 0,
        templateId: 2,
        type: 'common',
        typeName: 'common',
        files: [
          {
            id: 1,
            name: 'file',
          },
        ],
      },
    ],
    initialValues: {
      subject: 'Subject',
    },
  };
};

jest.useFakeTimers();

describe('NewMailFormLoadable', () => {
  describe('resend', () => {
    let sendButton: HTMLElement;
    const mockSubmit = jest.fn(async (_values) => ({ mailId: 100 }));

    beforeAll(async () => {
      render(
        <TestBed>
          <NewMailFormLoadableBundle
            onLoad={onLoad}
            onSubmit={mockSubmit}
            isPreventUnload
            hasAutoSave
          />
        </TestBed>,
      );

      await waitFor(() => screen.findByDisplayValue('Subject'));

      sendButton = screen.getByText('Отправить');

      return;
    });

    afterAll(() => {
      cleanup();
    });

    beforeEach(() => {
      mockSubmit.mockClear();
    });

    it("doesn't send mailId on first try", async () => {
      await act(async () => {
        fireEvent.click(sendButton);
      });
      expect(mockSubmit).toBeCalledTimes(1);
      expect(mockSubmit.mock.calls[0][0]!.mailId).toBeUndefined();
      await runPendingPromises();
    });

    it('does send mailId on second try', async () => {
      await act(async () => {
        fireEvent.click(sendButton);
      });
      expect(mockSubmit).toBeCalledTimes(1);
      expect(mockSubmit.mock.calls[0][0]!.mailId).toBe(100);
    });
  });

  describe('autosave', () => {
    const mockSubmit = jest.fn(async (_values) => ({ mailId: 100 }));

    beforeAll(() => {
      render(
        <TestBed>
          <NewMailFormLoadableBundle
            onLoad={onLoad}
            onSubmit={mockSubmit}
            isPreventUnload
            hasAutoSave
          />
        </TestBed>,
      );

      return waitFor(() => screen.findByDisplayValue('Subject'));
    });

    afterAll(() => {
      cleanup();
    });

    beforeEach(() => {
      mockSubmit.mockClear();
    });

    it("doesn't call send if no changes", async () => {
      jest.advanceTimersByTime(AUTOSAVE_DELAY);
      await runPendingPromises();

      expect(mockSubmit).toBeCalledTimes(0);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('sends mailId on first try', async () => {
      await act(async () => {
        userEvent.type(screen.getByDisplayValue('Subject'), 'Subject2');

        jest.advanceTimersByTime(AUTOSAVE_DELAY);
        await runPendingPromises();
      });

      expect(mockSubmit).toBeCalledTimes(1);
      expect(mockSubmit.mock.calls[0][0]!.mailId).toBeUndefined();
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('sends mailId on second try', async () => {
      await act(async () => {
        userEvent.type(screen.getByDisplayValue('Subject2'), 'Subject3');

        jest.advanceTimersByTime(AUTOSAVE_DELAY);
        await runPendingPromises();
      });

      expect(mockSubmit).toBeCalledTimes(1);
      expect(mockSubmit.mock.calls[0][0]!.mailId).toBe(100);
    });
  });
});
