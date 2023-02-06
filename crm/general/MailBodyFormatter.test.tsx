import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MailBodyFormatter } from './MailBodyFormatter';

const blockquoteTextStub = 'plainText';
const blockquoteHtmlStub = `<blockquote><div>${blockquoteTextStub}</div></blockquote>`;
const blockquoteIdStub = 'mail_id_BLOCKQUOTE_0';

describe('MailBodyFormatter', () => {
  describe('props.isHtml', () => {
    describe('when true', () => {
      it('renders body as HTML', () => {
        render(<MailBodyFormatter id="id" isHtml body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).not.toBeInTheDocument();
      });

      it('renders formated body', () => {
        const { container } = render(
          <MailBodyFormatter id="id" isHtml body={blockquoteHtmlStub} />,
        );

        expect(container.querySelector(`#${blockquoteIdStub}`)).toBeInTheDocument();
      });

      describe('props.onQuoteClick', () => {
        describe('when defined', () => {
          it('calls on show quote button click and renders quote text', () => {
            const onQuoteClick = jest.fn();
            render(
              <MailBodyFormatter
                id="id"
                onQuoteClick={onQuoteClick}
                isHtml
                body={blockquoteHtmlStub}
              />,
            );

            expect(screen.queryByText(blockquoteTextStub)).not.toBeInTheDocument();

            fireEvent.click(screen.getByRole('button'));

            expect(screen.queryByText(blockquoteTextStub)).toBeInTheDocument();
            expect(onQuoteClick).toBeCalledWith(true, blockquoteIdStub);
          });
        });
      });

      describe('props.expandedQuoteIds', () => {
        describe('when defined', () => {
          it('renders quote text by default', () => {
            render(
              <MailBodyFormatter
                id="id"
                expandedQuoteIds={[blockquoteIdStub]}
                isHtml
                body={blockquoteHtmlStub}
              />,
            );

            expect(screen.queryByText(blockquoteTextStub)).toBeInTheDocument();
          });
        });

        describe('when undefined', () => {
          it("doesn't render quote text by default", () => {
            render(<MailBodyFormatter id="id" isHtml body={blockquoteHtmlStub} />);

            expect(screen.queryByText(blockquoteTextStub)).not.toBeInTheDocument();
          });
        });
      });
    });

    describe('when false', () => {
      it('renders body as plain text', () => {
        render(<MailBodyFormatter id="id" isHtml={false} body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it('renders body as plain text', () => {
        render(<MailBodyFormatter id="id" body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).toBeInTheDocument();
      });
    });
  });

  describe('props.body', () => {
    describe('when defined', () => {
      it('renders body', () => {
        render(<MailBodyFormatter id="id" body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render body", () => {
        render(<MailBodyFormatter id="id" />);

        expect(screen.queryByText(blockquoteHtmlStub)).not.toBeInTheDocument();
      });
    });
  });

  describe('props.calendarTip', () => {
    describe('when defined', () => {
      it('renders CalendarEvent', () => {
        const { container } = render(<MailBodyFormatter id="id" calendarTip={{}} />);

        expect(container.getElementsByClassName('CalendarEvent').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it('renders CalendarEvent', () => {
        const { container } = render(<MailBodyFormatter id="id" />);

        expect(container.getElementsByClassName('CalendarEvent').length).toBe(0);
      });
    });
  });
});
