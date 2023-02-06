import React from 'react';
import { render, screen } from '@testing-library/react';
import { LeftRight } from './LeftRight';

describe('design/LeftRight', () => {
  describe('props.left', () => {
    it('renders ReactNode in the left part', () => {
      render(<LeftRight left="test" />);
      const leftNode = screen.getByLabelText('left');

      expect(leftNode).toHaveTextContent('test');
    });
  });

  describe('props.right', () => {
    it('renders ReactNode in the right part', () => {
      render(<LeftRight right="test" />);
      const rightNode = screen.getByLabelText('right');

      expect(rightNode).toHaveTextContent('test');
    });
  });

  describe('props.size', () => {
    describe('.max', () => {
      describe(`when left one's max is defined`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{
                max: {
                  left: 100,
                },
              }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '57%',
            'margin-right': '6%',
            'max-width': '100px',
          });
          expect(rightNode).toHaveStyle({
            'max-width': 'calc(100% - 100px)',
            'flex-basis': 'calc(100% - 100px)',
          });
        });
      });

      describe(`when right one's max is defined`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{
                max: {
                  right: 100,
                },
              }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': 'calc(100% - 100px)',
            'margin-right': '6%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '100px',
          });
        });
      });

      describe(`when both's maximums are defined`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{
                max: {
                  left: 50,
                  right: 100,
                },
              }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'max-width': '50px',
            'flex-basis': 'calc(100% - 100px)',
            'margin-right': '6%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '100px',
            'flex-basis': 'calc(100% - 50px)',
          });
        });
      });
    });

    describe('.ratio', () => {
      describe('when is not defined', () => {
        it('computes style with default ratio', () => {
          render(<LeftRight size={{}} left="left-test" right="right-test" />);
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '57%',
            'margin-right': '6%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '37%',
          });
        });
      });

      describe(`when both's ratios are not 0`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{ ratio: { left: 1, right: 3 } }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '22%',
            'margin-right': '6%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '72%',
          });
        });
      });

      describe(`when left one's ratio is 0`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{ ratio: { left: 0, right: 1 } }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '0%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '100%',
          });
        });
      });

      describe(`when right one's ratio is 0`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{ ratio: { left: 3, right: 0 } }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '100%',
            'margin-right': '0%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '0%',
          });
        });
      });

      describe(`when both's ratios are 0`, () => {
        it('computes style properly', () => {
          render(
            <LeftRight
              size={{ ratio: { left: 0, right: 0 } }}
              left="left-test"
              right="right-test"
            />,
          );
          const leftNode = screen.getByLabelText('left');
          const rightNode = screen.getByLabelText('right');

          expect(leftNode).toHaveStyle({
            'flex-basis': '0%',
            'margin-right': '0%',
          });
          expect(rightNode).toHaveStyle({
            'max-width': '0%',
          });
        });
      });
    });
  });
});
