import { Messages } from '../constants';
import { Message } from '../types';
import { createMessagesReducerFactory } from './reducerFactory';

const { messageReducer, messageActions } = createMessagesReducerFactory('messages');
const mockMessage: Message = { id: 1, title: 'Error', type: 'error', lastUpdate: new Date() };

describe('MessagesViewer', () => {
  it('Add message', () => {
    const messages = messageReducer([], messageActions.add(Messages.error('Error')));
    expect(messages).toHaveLength(1);
  });

  it('Remove message', () => {
    const messages = messageReducer([mockMessage], messageActions.remove(mockMessage.id));
    expect(messages).toHaveLength(0);
  });

  it('Clear messages', () => {
    const messagesState = messageReducer([mockMessage], messageActions.clear());
    expect(messagesState).toHaveLength(0);
  });

  it('Change messages by key', () => {
    const message = Messages.info('info', { key: 'uniqKey' });
    const messagesState = messageReducer([{ ...message, id: 1, lastUpdate: new Date() }], messageActions.add(message));

    expect(messagesState).toHaveLength(1);
  });

  it('Remove messages by key', () => {
    const message = Messages.info('info', { key: 'uniqKey' });
    const messagesState = messageReducer(
      [{ ...message, id: 1, lastUpdate: new Date() }],
      messageActions.remove('uniqKey')
    );

    expect(messagesState).toHaveLength(0);
  });
});
