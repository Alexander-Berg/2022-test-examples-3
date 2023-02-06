import React from 'react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SupportChatIssue } from './SupportChatIssue';
import {
  issueIdStub,
  responseErrorStub,
  responseNotFoundStub,
  responseFoundStub,
  requestCreateIssueStub,
} from './SupportChatIssue.stub';

jest.mock('modules/issues', () => ({
  Issue: () => <div data-testid="internalissue" />,
}));

describe('SupportChatIssue', () => {
  describe('props.issueId', () => {
    describe('when defined', () => {
      describe('when something went wrong', () => {
        it('renders error', async () => {
          const server = setupServer(
            rest.get(
              `${window.CRM_SPACE_API_HOST}/issues/oneAndHalfLine/${issueIdStub}`,
              (req, res, ctx) => {
                return res(ctx.json(responseErrorStub));
              },
            ),
          );
          server.listen();

          render(<SupportChatIssue issueId={issueIdStub} />);

          userEvent.click(screen.getByRole('button'));

          expect(screen.getByTestId('spinner')).toBeInTheDocument();

          await waitFor(() => expect(screen.queryByTestId('spinner')).not.toBeInTheDocument());

          expect(screen.getByText(responseErrorStub.error.title)).toBeInTheDocument();

          server.close();
        });
      });

      describe('when issue not found', () => {
        it('renders request button', async () => {
          const server = setupServer(
            rest.get(
              `${window.CRM_SPACE_API_HOST}/issues/oneAndHalfLine/${issueIdStub}`,
              (req, res, ctx) => {
                return res(ctx.json(responseNotFoundStub));
              },
            ),
          );
          server.listen();

          render(<SupportChatIssue issueId={issueIdStub} />);

          userEvent.click(screen.getByRole('button'));

          expect(screen.getByTestId('spinner')).toBeInTheDocument();

          await waitFor(() => expect(screen.queryByTestId('spinner')).not.toBeInTheDocument());

          expect(screen.getByText('Обратиться в 1.5 линию')).toBeInTheDocument();

          server.close();
        });

        describe('when on request button click', () => {
          it('creates new issue', async () => {
            const requestSpy = jest.fn();
            const server = setupServer(
              rest.get(
                `${window.CRM_SPACE_API_HOST}/issues/oneAndHalfLine/${issueIdStub}`,
                (req, res, ctx) => {
                  return res(ctx.json(responseNotFoundStub));
                },
              ),
              rest.post(
                `${window.CRM_SPACE_API_HOST}/issues/oneAndHalfLine/create`,
                (req, res, ctx) => {
                  requestSpy(req);
                  return res(ctx.json(responseNotFoundStub));
                },
              ),
            );
            server.listen();

            render(<SupportChatIssue issueId={issueIdStub} />);

            userEvent.click(screen.getByRole('button'));

            await waitFor(() =>
              expect(screen.getByText('Обратиться в 1.5 линию')).toBeInTheDocument(),
            );

            userEvent.click(screen.getByText('Обратиться в 1.5 линию'));

            await waitFor(() =>
              expect(requestSpy.mock.calls[0][0].body).toStrictEqual(requestCreateIssueStub),
            );

            server.close();
          });
        });
      });

      describe('when issue found', () => {
        it('renders issue', async () => {
          const server = setupServer(
            rest.get(
              `${window.CRM_SPACE_API_HOST}/issues/oneAndHalfLine/${issueIdStub}`,
              (req, res, ctx) => {
                return res(ctx.json(responseFoundStub));
              },
            ),
          );
          server.listen();

          render(<SupportChatIssue issueId={issueIdStub} />);

          expect(screen.queryByTestId('internalissue')).not.toBeInTheDocument();

          userEvent.click(screen.getByRole('button'));

          expect(screen.getByTestId('spinner')).toBeInTheDocument();

          await waitFor(() => expect(screen.queryByTestId('spinner')).not.toBeInTheDocument());

          expect(screen.getByTestId('internalissue')).toBeInTheDocument();

          server.close();
        });
      });
    });
  });
});
