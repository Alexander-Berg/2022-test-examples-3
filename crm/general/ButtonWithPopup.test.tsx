import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Button from '@crm/components/dist/lego2/Button';
import { ButtonWithPopup } from './ButtonWithPopup';

const ButtonComponent = <Button data-testid={'moreButton'} size="l" />;

describe('ButtonWithPopup', () => {
  describe('props.button', () => {
    describe('when defined', () => {
      it('renders button', () => {
        render(<ButtonWithPopup button={ButtonComponent} />);

        expect(screen.getByTestId('moreButton')).toBeInTheDocument();
      });
    });
  });

  describe('props.onClick', () => {
    describe('when defined', () => {
      it('calls on button click', () => {
        const onClick = jest.fn();
        render(<ButtonWithPopup button={ButtonComponent} onClick={onClick} />);

        userEvent.click(screen.getByTestId('moreButton'));

        expect(onClick).toBeCalled();
      });
    });
  });

  describe('props.onVisibleChange', () => {
    describe('when defined', () => {
      it('calls on popup visibility change', () => {
        const onVisibleChange = jest.fn();
        render(<ButtonWithPopup button={ButtonComponent} onVisibleChange={onVisibleChange} />);

        userEvent.click(screen.getByTestId('moreButton'));
        expect(onVisibleChange).toBeCalledWith(true);

        userEvent.click(screen.getByTestId('moreButton'));
        expect(onVisibleChange).toBeCalledWith(false);
      });
    });
  });

  describe('props.onOutsideClick', () => {
    describe('when defined', () => {
      it('calls on popup outside click', () => {
        const onOutsideClick = jest.fn();
        render(
          <div data-testid="outside">
            <ButtonWithPopup button={ButtonComponent} onOutsideClick={onOutsideClick} />
          </div>,
        );

        userEvent.click(screen.getByTestId('moreButton'));
        userEvent.click(screen.getByTestId('outside'));

        expect(onOutsideClick).toBeCalled();
      });
    });
  });

  describe('props.children', () => {
    describe('when defined', () => {
      it('renders children at popup', () => {
        render(
          <ButtonWithPopup button={ButtonComponent}>
            <div />
            <div />
          </ButtonWithPopup>,
        );

        userEvent.click(screen.getByTestId('moreButton'));

        expect(screen.getByRole('presentation').children.length).toBe(2);
      });
    });

    describe('when undefined', () => {
      it("doesn't render children at popup", () => {
        render(<ButtonWithPopup button={ButtonComponent} />);

        userEvent.click(screen.getByTestId('moreButton'));

        expect(screen.getByRole('presentation').children.length).toBe(0);
      });
    });
  });

  describe('props.visible', () => {
    describe('when true', () => {
      it('renders popup by default', () => {
        render(<ButtonWithPopup button={ButtonComponent} visible />);

        expect(screen.getByRole('presentation')).toBeInTheDocument();
      });
    });

    describe('when false or undefined', () => {
      it("doesn't render children at popup by default", () => {
        render(<ButtonWithPopup button={ButtonComponent} visible={false} />);

        expect(screen.queryByRole('presentation')).not.toBeInTheDocument();
      });
    });
  });
});
