import { Toasts } from '@yandex-market/mbo-components';
import { mount, ReactWrapper } from 'enzyme';
import React, { ReactNode } from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { Messages } from '../constants';
import { createMessagesReducerFactory } from '../store/reducerFactory';
import { Message } from '../types';
import { MessagesViewer } from './MessagesViewer';

interface RootState {
  messages: Message[];
}

const setupComponent = (formatter?: (message: Message) => ReactNode) => {
  const { messageReducer, messageActions, messagesSelector } = createMessagesReducerFactory('messages');
  const rootReducer = combineReducers<RootState>({
    messages: messageReducer,
  });
  const store = createStore(rootReducer, { messages: [] });

  const wrapper: ReactWrapper = mount(
    <div>
      <Provider store={store}>
        <MessagesViewer actions={messageActions} selector={messagesSelector} formatter={formatter} />
      </Provider>
    </div>
  );

  return { store, wrapper, actions: messageActions };
};

const customFormatter = (message: Message): ReactNode => {
  if (message.details?.customDetail) {
    return message.details.customDetail as string;
  }

  return '';
};

describe('MessagesViewer', () => {
  it('Render MessagesViewer', () => {
    const { wrapper } = setupComponent();
    const messagesViewer = wrapper.find(MessagesViewer);
    expect(messagesViewer).toHaveLength(1);
    expect(messagesViewer.find(Toasts)).toHaveLength(1);
  });
  it('Display message', () => {
    const { wrapper, store, actions } = setupComponent();
    const messagesViewer = wrapper.find(MessagesViewer);
    const toastsContainer = messagesViewer.find(Toasts);
    store.dispatch(actions.add(Messages.error('Error')));
    expect(toastsContainer.html()).toContain('Error');
  });
  it('Auto close message', () => {
    const { wrapper, store, actions } = setupComponent();
    const messagesViewer = wrapper.find(MessagesViewer);
    const toastsContainer = messagesViewer.find(Toasts);
    const delay = 1000;
    store.dispatch(actions.add(Messages.success('Success', { delay })));
    expect(toastsContainer.html()).toContain('Success');

    return new Promise(res => {
      setTimeout(() => {
        expect(toastsContainer.html()).not.toContain('Success');
        res();
      }, delay);
    });
  });
  it('Display format message', () => {
    const { wrapper, store, actions } = setupComponent(customFormatter);
    const messagesViewer = wrapper.find(MessagesViewer);
    store.dispatch(
      actions.add(Messages.error('Error', { details: { body: 'DefaultMsg', customDetail: 'CustomErrorMsg' } }))
    );
    const toastsContainerHtml = messagesViewer.find(Toasts).html();
    expect(toastsContainerHtml).toContain('CustomErrorMsg');
    expect(toastsContainerHtml).not.toContain('DefaultMsg');
  });
  it('Display default format message', () => {
    const { wrapper, store, actions } = setupComponent(customFormatter);
    const messagesViewer = wrapper.find(MessagesViewer);
    store.dispatch(actions.add(Messages.error('Error', { details: { body: ['DefaultMsg1', 'DefaultMsg2'] } })));
    const toastsContainerHtml = messagesViewer.find(Toasts).html();
    expect(toastsContainerHtml).toContain('DefaultMsg1');
    expect(toastsContainerHtml).toContain('DefaultMsg2');
  });
});
