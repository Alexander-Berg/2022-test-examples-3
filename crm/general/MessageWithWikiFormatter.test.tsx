import React from 'react';
import { render } from '@testing-library/react';
import { MessageWithWikiFormatter } from './MessageWithWikiFormatter';

describe('MessageWithWikiFormatter', () => {
  describe('props.text', () => {
    describe('when defined', () => {
      it('renders WikiFormatter', async () => {
        const { container } = render(<MessageWithWikiFormatter text={'text'} />);

        expect(container.getElementsByClassName('WikiFormatter').length).toBe(1);
      });
    });

    describe('when undefined', () => {
      it("doesn't render WikiFormatter", async () => {
        const { container } = render(<MessageWithWikiFormatter />);

        expect(container.getElementsByClassName('WikiFormatter').length).toBe(0);
      });
    });
  });
});
