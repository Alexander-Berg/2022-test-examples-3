import React, {useCallback, useState} from 'react';
import uniqueId from 'lodash/uniqueId';

import {SOURCE} from '../../constants/source';

import {prepareQaAttributes} from 'utilities/qaAttributes/qaAttributes';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import MessageList from 'projects/testControlPanel/pages/Test3DSPage/components/MessageList/MessageList';

import useMessageListener from 'projects/testControlPanel/pages/Test3DSPage/hooks/useMessageListener';

import cx from './Test3DSFramePage.scss';

const ROOT_QA = 'test3DSFramePage';

interface ITest3DSFramePageProps {}

const Test3DSFramePage: React.FC<ITest3DSFramePageProps> = () => {
    const initialMessages: string[] = [];
    const [messages, setMessages] = useState(initialMessages);

    const handleReceiveMessage = useCallback(
        msg => {
            setMessages([...messages, msg.message]);
        },
        [messages, setMessages],
    );

    useMessageListener(handleReceiveMessage);

    const handleSendMessageButtonClick = useCallback(() => {
        window.parent.postMessage(
            {source: SOURCE, message: `${uniqueId('message_')} from frame`},
            '*',
        );
    }, []);

    return (
        <CardWithDeviceLayout
            className={cx('root')}
            {...prepareQaAttributes(ROOT_QA)}
        >
            <MessageList
                title="Полученные сообщения во фрейме"
                messages={messages}
                buttonText="Отправить сообщение на страницу"
                onSendClick={handleSendMessageButtonClick}
                {...prepareQaAttributes({
                    parent: ROOT_QA,
                    current: 'messageList',
                })}
            />
        </CardWithDeviceLayout>
    );
};

export default Test3DSFramePage;
