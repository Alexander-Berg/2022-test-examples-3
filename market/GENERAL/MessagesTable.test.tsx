import React from 'react';
import { render } from '@testing-library/react';
import { MessagesTable } from './MessagesTable';

describe('<MessagesTable />', () => {
  it('renders unknown messages', () => {
    const messages: string[] = [
      'unknown message',
      'Offer content system response version is higher than content version',
    ];

    const app = render(<MessagesTable messages={messages} />);
    expect(app.container.getElementsByTagName('td')).toHaveLength(messages.length * 4);
  });
});
