import { encodeStateAsUpdate, encodeStateVector, applyUpdate } from 'yjs';

import { createConnectedDocs, wait } from './utils';

describe('Doc behavior', () => {
    test('Connected docs', async () => {
        const { text1, text2 } = createConnectedDocs();

        expect(text1.toString()).toBe(text2.toString());

        text1.insert(0, 'foo');

        expect(text1.toString()).toBe(text2.toString());

        text2.insert(0, 'bar');

        expect(text1.toString()).toBe(text2.toString());
    });

    test('Disconnect', async () => {
        const { text1, text2, connect, disconnect } = createConnectedDocs();

        text1.insert(0, 'foo');
        const text1Value = text1.toString();

        expect(text1.toString()).toBe(text2.toString());

        disconnect();
        text2.insert(0, 'bar');

        expect(text1.toString()).not.toBe(text2.toString());
        connect();

        text2.insert(0, 'bar');

        expect(text1.toString()).toBe(text1Value);
    });

    test('Restore state with sync', () => {
        const { text1, text2, doc1, doc2, disconnect } = createConnectedDocs();

        text1.insert(0, 'foo'); // foo, foo
        disconnect();

        text1.insert(0, 'bar'); // barfoo, foo

        expect(text1.toString()).not.toBe(text2.toString());

        const vector2 = encodeStateVector(doc2);
        const diff1 = encodeStateAsUpdate(doc1, vector2);

        applyUpdate(doc2, diff1);

        expect(text1.toString()).toBe(text2.toString());
    });

    test('Restore state with sync conflict', async () => {
        const { text1, text2, doc1, doc2, disconnect } = createConnectedDocs();

        text1.insert(0, 'foo'); // foo, foo
        disconnect();

        text1.insert(0, 'bar'); // barfoo, foo
        await wait(100);
        text2.insert(0, 'baz'); // barfoo, bazfoo

        expect(text1.toString()).not.toBe(text2.toString());
        expect(text1.toString()).toBe('barfoo');
        expect(text2.toString()).toBe('bazfoo');

        const vector2 = encodeStateVector(doc2);
        const vector1 = encodeStateVector(doc1);
        const diff1 = encodeStateAsUpdate(doc1, vector2);
        const diff2 = encodeStateAsUpdate(doc2, vector1);

        applyUpdate(doc2, diff1);
        applyUpdate(doc1, diff2);

        expect(text1.toString()).toBe(text2.toString());
    });
});
