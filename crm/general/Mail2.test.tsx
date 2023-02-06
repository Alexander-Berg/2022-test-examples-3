import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Mail2 } from './Mail2';

const blockquoteTextStub = 'plainText';
const blockquoteHtmlStub = `<blockquote><div>${blockquoteTextStub}</div></blockquote>`;
const blockquoteIdStub = 'mail_id_BLOCKQUOTE_0';

describe('Mail2', () => {
  describe('props.from', () => {
    describe('when defined', () => {
      it("renders 'from' text", () => {
        render(<Mail2 id="id" from="from" />);

        expect(screen.getByText('from')).toBeInTheDocument();
      });
    });
  });

  describe('props.to', () => {
    describe('when defined', () => {
      it("renders 'to' text", () => {
        render(<Mail2 id="id" to="to" />);

        expect(screen.getByText('to')).toBeInTheDocument();
      });
    });
  });

  describe('props.cc', () => {
    describe('when defined', () => {
      it("renders 'cc' text when header expanded", () => {
        render(<Mail2 id="id" cc="cc" expandedHeader />);

        expect(screen.getByText('cc')).toBeInTheDocument();
      });

      it("doesn't render 'cc' text by default", () => {
        render(<Mail2 id="id" cc="cc" />);

        expect(screen.queryByText('cc')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.bcc', () => {
    describe('when defined', () => {
      it("renders 'bcc' text when header expanded", () => {
        render(<Mail2 id="id" bcc="bcc" expandedHeader />);

        expect(screen.getByText('bcc')).toBeInTheDocument();
      });

      it("doesn't render 'bcc' text by default", () => {
        render(<Mail2 id="id" bcc="bcc" />);

        expect(screen.queryByText('bcc')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.subject', () => {
    describe('when defined', () => {
      it("renders 'subject' text when header expanded", () => {
        render(<Mail2 id="id" subject="subject" expandedHeader />);

        expect(screen.getByText('subject')).toBeInTheDocument();
      });

      it("doesn't render 'subject' text by default", () => {
        render(<Mail2 id="id" subject="subject" />);

        expect(screen.queryByText('subject')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.expandedHeader', () => {
    describe('when true', () => {
      it('renders expandedHeader header', () => {
        render(<Mail2 id="id" expandedHeader />);

        expect(screen.getByText('Тема:')).toBeInTheDocument();
      });
    });

    describe('when false or undefined', () => {
      it("doesn't render expandedHeader header", () => {
        render(<Mail2 id="id" />);

        expect(screen.queryByText('Тема:')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.body', () => {
    describe('when defined', () => {
      it('renders body', () => {
        render(<Mail2 id="id" body={'body'} />);

        expect(screen.getByText('body')).toBeInTheDocument();
      });
    });
  });

  describe('props.isHtml', () => {
    describe('when defined', () => {
      it('renders body as HTML', () => {
        render(<Mail2 id="id" isHtml body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).not.toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render body as HTML", () => {
        render(<Mail2 id="id" body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteHtmlStub)).toBeInTheDocument();
      });
    });
  });

  describe('props.calendarTip', () => {
    describe('when defined', () => {
      it('renders CalendarEvent', () => {
        const { container } = render(<Mail2 id="id" calendarTip={{}} />);

        expect(container.getElementsByClassName('CalendarEvent').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render CalendarEvent", () => {
        const { container } = render(<Mail2 id="id" />);

        expect(container.getElementsByClassName('CalendarEvent').length).toBe(0);
      });
    });
  });

  describe('props.expandedQuoteIds', () => {
    describe('when defined', () => {
      it('renders expanded quotes', () => {
        render(
          <Mail2 id="id" expandedQuoteIds={[blockquoteIdStub]} isHtml body={blockquoteHtmlStub} />,
        );

        expect(screen.queryByText(blockquoteTextStub)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render expanded quotes", () => {
        render(<Mail2 id="id" isHtml body={blockquoteHtmlStub} />);

        expect(screen.queryByText(blockquoteTextStub)).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onQuoteClick', () => {
    describe('when defined', () => {
      it('calls on show quote button click and renders quote text', () => {
        const onQuoteClick = jest.fn();
        render(<Mail2 id="id" onQuoteClick={onQuoteClick} isHtml body={blockquoteHtmlStub} />);
        expect(screen.queryByText(blockquoteTextStub)).not.toBeInTheDocument();

        fireEvent.click(screen.getByText('...'));

        expect(screen.queryByText(blockquoteTextStub)).toBeInTheDocument();
        expect(onQuoteClick).toBeCalledWith(true, blockquoteIdStub);
      });
    });
  });

  describe('props.onExpandHeader', () => {
    describe('when defined', () => {
      it('calls on header expand click', () => {
        const onExpandHeader = jest.fn();
        render(<Mail2 id="id" onExpandHeader={onExpandHeader} expandedHeader={false} />);

        fireEvent.click(screen.getByTestId('ExpandButtonText'));

        expect(onExpandHeader).toBeCalled();
      });
    });
  });
});
