import React, {useCallback, useEffect, useRef, useState} from 'react';
import uniqueId from 'lodash/uniqueId';

import {URLs} from 'constants/urls';
import {SOURCE} from 'projects/testControlPanel/pages/Test3DSPage/constants/source';

import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';

import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {prepareQaAttributes} from 'utilities/qaAttributes/qaAttributes';

import SecureIFrameProxy from 'components/SecureIFrameProxy/SecureIFrameProxy';
import TwoColumnLayout from 'components/Layouts/TwoColumnLayout/TwoColumnLayout';
import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import MessageList from 'projects/testControlPanel/pages/Test3DSPage/components/MessageList/MessageList';
import Box from 'components/Box/Box';
import Text from 'components/Text/Text';

import useMessageListener from 'projects/testControlPanel/pages/Test3DSPage/hooks/useMessageListener';

import cx from './Test3DSDemoPage.scss';

const ROOT_QA = 'test3DSDemoPage';

interface ITest3DSDemoPageProps {}

const Test3DSDemoPage: React.FC<ITest3DSDemoPageProps> = () => {
    const deviceType = useDeviceType();
    const iframeRef = useRef<HTMLIFrameElement>(null);

    const initialMessages: string[] = [];
    const [messages, setMessages] = useState(initialMessages);

    const [src, setSrc] = useState('');

    useEffect(() => {
        const origin = window.location.origin;

        setSrc(`${origin}${URLs.testControlPanel3DSFrame}`);
    }, [setSrc]);

    const handleReceiveMessage = useCallback(
        msg => {
            setMessages([...messages, msg.message]);
        },
        [messages, setMessages],
    );

    useMessageListener(handleReceiveMessage);

    const handleSendMessageButtonClick = useCallback(() => {
        iframeRef.current?.contentWindow?.postMessage(
            {source: SOURCE, message: `${uniqueId('message_')} from page`},
            '*',
        );
    }, [iframeRef]);

    if (!src) {
        return null;
    }

    return (
        <TwoColumnLayout
            deviceType={deviceType}
            rightColumnOffset={10}
            rightColumnWidth={80}
            {...prepareQaAttributes(ROOT_QA)}
        >
            <TwoColumnLayout.LeftColumn>
                <CardWithDeviceLayout>
                    <Text size="m">
                        Страница для тестирования обмена сообщениями через
                        обертку SecureIFrameProxy, без кук и csp политик.
                        Сообщения должны доходить в единичном экземпляре в обе
                        стороны. Работа SecureIFrameProxy на стендах / Y.Deploy
                        отличается.
                    </Text>
                    <Box above={4}>
                        <SecureIFrameProxy
                            className={cx('iframe')}
                            src={src}
                            frameRef={iframeRef}
                            {...prepareQaAttributes({
                                parent: ROOT_QA,
                                current: 'iframe',
                            })}
                        />
                    </Box>
                </CardWithDeviceLayout>
            </TwoColumnLayout.LeftColumn>
            <TwoColumnLayout.RightColumn>
                <CardWithDeviceLayout
                    variation={ECardWithDeviceLayoutVariation.ASIDE}
                >
                    <MessageList
                        title="Полученные сообщения"
                        messages={messages}
                        messageClassName={cx('message')}
                        buttonText="Отправить сообщение во фрейм"
                        onSendClick={handleSendMessageButtonClick}
                        {...prepareQaAttributes({
                            parent: ROOT_QA,
                            current: 'messageList',
                        })}
                    />
                </CardWithDeviceLayout>
            </TwoColumnLayout.RightColumn>
        </TwoColumnLayout>
    );
};

export default Test3DSDemoPage;
