import React from 'react';

import { fireEvent, render, screen } from '@testing-library/react';
import { ExpandText } from './ExpandText';

const TEXT_110 =
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc interdum, sapien sit amet molestie posuere biam.';
const TEXT_100 =
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc interdum, sapien sit amet molestie pos';
const TEXT_5 = 'Lorem';

describe('ExpandText', () => {
  describe('props.text', () => {
    describe('when value.length <= MAX_TEXT_LENGTH', () => {
      it('does not render expand more text', () => {
        render(<ExpandText text={{ value: 'Text' }} />);

        expect(screen.queryByTestId('expand-more')).not.toBeInTheDocument();
      });
    });

    describe('when value.length > MAX_TEXT_LENGTH', () => {
      it('renders expand more text', () => {
        render(
          <ExpandText
            text={{
              value: TEXT_110,
            }}
          />,
        );

        expect(screen.getByTestId('expand-more')).toBeVisible();
      });

      it('renders clamped text', () => {
        render(
          <ExpandText
            text={{
              value: TEXT_110,
            }}
          />,
        );

        expect(screen.getByTestId('clamped-text')).toHaveTextContent(TEXT_100);
      });

      describe('when expand more text is clicked', () => {
        it('toggles expand', () => {
          render(
            <ExpandText
              text={{
                value: TEXT_110,
              }}
            />,
          );

          fireEvent.click(screen.getByTestId('expand-more'));

          expect(screen.getByTestId('clamped-text')).toHaveTextContent(TEXT_110);

          fireEvent.click(screen.getByTestId('expand-more'));

          expect(screen.getByTestId('clamped-text')).toHaveTextContent(TEXT_100);
        });
      });

      describe('props.maxSymbols', () => {
        describe('when > default', () => {
          it('clamps text by maxSymbols correctly', () => {
            render(
              <ExpandText
                text={{
                  value: TEXT_110,
                }}
                maxSymbols={5}
              />,
            );

            expect(screen.getByTestId('clamped-text')).toHaveTextContent(TEXT_5);
          });
        });

        describe('when < default', () => {
          it('clamps text by maxSymbols correctly', () => {
            render(
              <ExpandText
                text={{
                  value: TEXT_110,
                }}
                maxSymbols={500}
              />,
            );

            expect(screen.getByTestId('clamped-text')).toHaveTextContent(TEXT_110);
          });
        });
      });
    });
  });
});
