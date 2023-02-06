import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { SvgIconType } from '@crm/components/dist/lego2/Icon';
import { ExpandHeader } from './ExpandHeader';

const fieldsStub = [
  {
    title: 'From',
    text: 'from text',
    icon: 'person' as SvgIconType,
  },
];

describe('ExpandHeader', () => {
  describe('props.fields', () => {
    describe('when defined', () => {
      it('renders fields', () => {
        render(<ExpandHeader fields={fieldsStub} />);

        expect(screen.queryByText(fieldsStub[0].text)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render fields", () => {
        render(<ExpandHeader />);

        expect(screen.queryByText(fieldsStub[0].text)).not.toBeInTheDocument();
      });
    });
  });

  describe('props.expandFields', () => {
    describe('when defined', () => {
      it('renders expandFields when expanded', () => {
        render(<ExpandHeader expandFields={fieldsStub} expanded />);

        expect(screen.queryByText(fieldsStub[0].text)).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render expandFields and ExpandButton", () => {
        render(<ExpandHeader expanded />);

        expect(screen.queryByText(fieldsStub[0].text)).not.toBeInTheDocument();
        expect(screen.queryByTestId('ExpandButtonText')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.expandText', () => {
    describe('when defined', () => {
      it('renders expandText', () => {
        render(<ExpandHeader expandFields={fieldsStub} expandText="expandText" />);

        expect(screen.queryByText('expandText')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render expandText", () => {
        render(<ExpandHeader expandFields={fieldsStub} />);

        expect(screen.queryByText('expandText')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.expanded', () => {
    describe('when true', () => {
      it('renders expandFields by default', () => {
        render(<ExpandHeader expandFields={fieldsStub} expanded />);

        expect(screen.queryByText(fieldsStub[0].text)).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it("doesn't render expandFields by default", () => {
        render(<ExpandHeader expandFields={fieldsStub} expanded={false} />);

        expect(screen.queryByText(fieldsStub[0].text)).not.toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it("doesn't render expandFields by default", () => {
        render(<ExpandHeader expandFields={fieldsStub} />);

        expect(screen.queryByText(fieldsStub[0].text)).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onExpand', () => {
    describe('when defined', () => {
      it('calls on expand click', () => {
        const onExpand = jest.fn();
        render(<ExpandHeader onExpand={onExpand} expandFields={fieldsStub} />);
        fireEvent.click(screen.getByTestId('ExpandButtonText'));

        expect(onExpand).toBeCalled();
      });
    });
  });
});
