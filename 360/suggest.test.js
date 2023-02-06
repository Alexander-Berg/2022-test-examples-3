import reducer, { addHighlights } from './suggest';
import deepFreeze from 'deep-freeze';
import { setData, setAvatar } from '../actions/suggest';

import samples from '../samples.json';

describe('addHighlights', () => {
    it('works without ranges', () => {
        expect(addHighlights('text')).toEqual('text');
    });

    it('works with single range', () => {
        expect(addHighlights('01234567890123456789', [{ s: 0, e: 5 }]))
            .toEqual('<span class="msearch-highlight">01234</span>567890123456789');
    });

    it('works with multiple ranges', () => {
        expect(addHighlights('01234567890123456789', [{ s: 0, e: 5 }, { s: 7, e: 9 }]))
            .toEqual('<span class="msearch-highlight">01234</span>56<span class="msearch-highlight">78</span>90123456789');
    });

    it('entitifies and still works', () => {
        expect(addHighlights('test <iframe src="http://example.com"></iframe>example', [{ s: 25, e: 28 }, { s: 47, e: 50 }]))
            .toEqual('test &lt;iframe src=&quot;http:&#x2F;&#x2F;<span class="msearch-highlight">exa</span>mple.com&quot;&gt;&lt;&#x2F;iframe&gt;<span class="msearch-highlight">exa</span>mple');
    });
});

describe('suggest reducer', () => {
    const initialState = {
        entities: {
            items: {}
        },
        result: [],
        contacts: [],
        subjects: [],
        avatars: {},
        version: -1
    };

    deepFreeze(initialState);

    it('has initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual(initialState);
    });

    it('adds ids', () => {
        expect(reducer(initialState, setData([{}, {}, { id: 'deadbeef' }])).result)
            .toEqual([ 0, 1, 'deadbeef' ]);
    });

    describe('#setData', () => {
        it('works', () => {
            expect(reducer(initialState, setData(samples))).toMatchSnapshot();
        });
    });

    describe('#setAvatar', () => {
        const ava1 = {
            email: 'vasya.testov@yandex.ru',
            avatar: {
                type: 'monogram',
                monogram: 'ВТ',
                color: '03aebc'
            }
        };

        const ava2 = {
            email: 'qqq@www.ru',
            avatar: {
                type: 'image',
                image: 'http://example.com/image.jpg'
            }
        };

        let state;

        it('adds avatar', () => {
            state = reducer(initialState, setData(samples));
            state = reducer(state, setAvatar(ava1.email, ava1.avatar));

            expect(Object.keys(state.avatars)).toEqual([ 'vasya.testov@yandex.ru' ]);
        });

        it('skips unexising emails', () => {
            state = reducer(initialState, setData(samples));
            state = reducer(state, setAvatar(ava2.email, ava2.avatar));
            expect(Object.keys(state.avatars)).toEqual([]);
        });
    });
});
