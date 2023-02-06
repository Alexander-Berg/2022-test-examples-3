import React from 'react';

import {
    IWithQaAttributes,
    prepareQaAttributes,
} from 'utilities/qaAttributes/qaAttributes';

import Heading from 'components/Heading/Heading';
import Flex from 'components/Flex/Flex';
import Button from 'components/Button/Button';

interface IMessageListProps extends IWithQaAttributes {
    title: string;
    messages: string[];
    messageClassName?: string;
    buttonText: string;
    onSendClick(): void;
}

const MessageList: React.FC<IMessageListProps> = props => {
    const {title, messages, messageClassName, buttonText, onSendClick} = props;

    return (
        <>
            <Heading level={2}>{title}</Heading>

            <Flex flexDirection="column" above={4} below={4} between={2}>
                {messages.length
                    ? messages.map((m, i) => (
                          <div
                              key={i}
                              className={messageClassName}
                              title={m}
                              {...prepareQaAttributes({
                                  key: i,
                                  parent: props,
                                  current: 'message',
                              })}
                          >
                              {m}
                          </div>
                      ))
                    : 'нет сообщений'}
            </Flex>

            <Button
                theme="primary"
                width="max"
                size="l"
                onClick={onSendClick}
                {...prepareQaAttributes({parent: props, current: 'sendButton'})}
            >
                {buttonText}
            </Button>
        </>
    );
};

export default MessageList;
