import { UndoManager, Doc } from 'yjs';

import { createConnectedDocs } from './utils';

describe('UndoManager behavior', () => {
    test('Undo redo 1 document', () => {
        const doc = new Doc();
        const text = doc.getText('code');

        const undoManager = new UndoManager(text);

        text.insert(0, 'foo');
        text.insert(0, 'bar');

        expect(text.toString()).toBe('barfoo');
        undoManager.undo();
        expect(text.toString()).toBe('');
    });

    test('Undo redo 2 connected documents', () => {
        const { text1, text2 } = createConnectedDocs();

        const undoManager = new UndoManager(text1);

        text1.insert(0, 'foo');
        text2.insert(0, 'bar');

        undoManager.undo();

        expect(text1.toString()).toBe(text2.toString());

        expect(text1.toString()).toBe('');
        expect(text2.toString()).toBe('');
    });

    test('Undo redo 2 connected documents with origin filter', () => {
        const { doc1, text1, doc2, text2 } = createConnectedDocs();

        const origin1 = Symbol('1');
        const origin2 = Symbol('2');

        const undoManager1 = new UndoManager(text1, {
            trackedOrigins: new Set([origin1]),
        });
        const undoManager2 = new UndoManager(text1, {
            trackedOrigins: new Set([origin2]),
        });

        doc1.transact(() => {
            text1.insert(0, 'foo');
        }, origin1);

        doc2.transact(() => {
            text2.insert(0, 'bar');
        }, origin2);

        undoManager1.undo();

        expect(text1.toString()).toBe(text2.toString());

        expect(text1.toString()).toBe('bar');
        expect(text2.toString()).toBe('bar');

        undoManager2.undo();

        expect(text1.toString()).toBe(text2.toString());

        expect(text1.toString()).toBe('');
        expect(text2.toString()).toBe('');
    });

    test('INTERVIEWDEV-13 experiment', () => {
        const { doc1, text1, doc2, text2 } = createConnectedDocs();

        const origin1 = Symbol('1');
        const origin2 = Symbol('2');

        const undoManager1 = new UndoManager(text1, {
            trackedOrigins: new Set([origin1]),
        });
        const undoManager2 = new UndoManager(text1, {
            trackedOrigins: new Set([origin2]),
        });

        doc1.transact(() => {
            text1.insert(0, '1\n');
        }, origin1);
        undoManager1.stopCapturing();

        doc1.transact(() => {
            text1.insert(2, '1\n');
        }, origin1);
        undoManager1.stopCapturing();

        doc1.transact(() => {
            text1.insert(4, '1\n');
        }, origin1);
        undoManager1.stopCapturing();

        doc2.transact(() => {
            text2.insert(6, '2\n');
        }, origin2);
        undoManager2.stopCapturing();

        doc2.transact(() => {
            text2.insert(8, '2\n');
        }, origin2);
        undoManager2.stopCapturing();

        doc2.transact(() => {
            text2.insert(10, '2\n');
        }, origin2);
        undoManager2.stopCapturing();

        doc2.transact(() => {
            text2.delete(0, 6);
        }, origin2);
        undoManager2.stopCapturing();

        const textBeforeUndo = text1.toString();

        undoManager1.undo();
        undoManager1.undo();
        undoManager1.undo();

        expect(text1.toString()).toBe(textBeforeUndo);
    });
});
