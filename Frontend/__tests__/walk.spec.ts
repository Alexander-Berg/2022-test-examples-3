import TestAdapter from './TestComponent/Test.adapter';
import { walk } from '../../../src/core/walk';

jest.mock('../../.tmp/applications.registry', () => ({}));

const data = {
    block: 'test',
    slots: {
        bottomElem: {
            block: 'paragraph',
            content: 'bottomElem',
        },
    },
};

test('walk.tsx корректно обрабатывает поле slots', () => {
    const adapterContext = {
        predicates: {},
        expFlags: {},
        assets: {
            generateId: () => Math.floor(Math.random() * 100),
            getReactPlatform: () => 'phone',
            getStore: () => ({
                getState: () => ({}),
            }),
            pushBundleReact: () => {},
            pushTree: () => {},
            bemhtmlContext: {
                hasContext: () => false,
            },
        },
        data: {
            cgidata: {
                args: {},
            },
        },
    };
    // @ts-ignore
    const result = walk(data, { Test: TestAdapter }, adapterContext);
    expect(result.html).toEqual('<div data-reactroot=""><div>bottomElem</div></div>');
});
