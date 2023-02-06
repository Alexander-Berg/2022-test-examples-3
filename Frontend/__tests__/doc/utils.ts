/* eslint-disable @typescript-eslint/no-explicit-any */

import { Doc, applyUpdate } from 'yjs';

export const LOCAL_ORIGIN = '__local__';
export const TEXT_KEY = 'TEST_TEXT';

export const createConnectedDocs = () => {
    const { doc: doc1, text: text1 } = createDoc();
    const { doc: doc2, text: text2 } = createDoc();

    let disconnect: () => void = () => {};

    const connect = () => {
        const handler1 = (update: Uint8Array, origin: any) => {
            if (origin !== LOCAL_ORIGIN) {
                applyUpdate(doc2, update, origin);
            }
        };

        doc1.on('update', handler1);

        const handler2 = (update: Uint8Array, origin: any) => {
            if (origin !== LOCAL_ORIGIN) {
                applyUpdate(doc1, update, origin);
            }
        };

        doc2.on('update', handler2);

        disconnect = () => {
            doc1.off('update', handler1);
            doc2.off('update', handler2);
        };
    };

    connect();

    return {
        doc1,
        doc2,
        text1,
        text2,
        connect,
        disconnect,
    };
};

export const createDoc = () => {
    const doc = new Doc();
    const text = doc.getText(TEXT_KEY);

    return {
        doc,
        text,
    };
};

export const wait = (msec: number) => new Promise((resolve) => setTimeout(resolve, msec));
