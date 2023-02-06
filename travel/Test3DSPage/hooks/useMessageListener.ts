import {useEffect} from 'react';

import {SOURCE} from 'projects/testControlPanel/pages/Test3DSPage/constants/source';

export default function useMessageListener(
    onMessage: (msg: string) => void,
): void {
    useEffect(() => {
        const handleChildrenPostMessage = (e: MessageEvent): void => {
            const {data} = e;

            if (data.source !== SOURCE) {
                return;
            }

            onMessage(data);
        };

        window.addEventListener('message', handleChildrenPostMessage, false);

        return (): void => {
            window.removeEventListener('message', handleChildrenPostMessage);
        };
    }, [onMessage]);
}
