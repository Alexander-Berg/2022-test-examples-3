import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MessageActionType } from 'types/Message/MessageActionType';
import { NotificationItem } from './NotificationItem';

const handleClose = () => {};

describe('NotificationItem', () => {
  it('renders button action', () => {
    const actionButtonClickMock = jest.fn();
    render(
      <NotificationItem
        id={1}
        onClose={handleClose}
        actions={[{ children: 'text', onClick: actionButtonClickMock }]}
      />,
    );

    const actionButton = screen.getByRole('button', { name: 'text' });
    fireEvent.click(actionButton);

    expect(actionButtonClickMock).toBeCalled();
  });

  it('renders link action', () => {
    render(
      <NotificationItem
        id={1}
        onClose={handleClose}
        actions={[{ type: MessageActionType.Link, data: { text: 'text', url: 'url' } }]}
      />,
    );

    const actionLink = screen.getByRole('link', { name: 'text' }) as HTMLAreaElement;
    expect(actionLink.href).toMatch(/url/);
  });
});
