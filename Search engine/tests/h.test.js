import assert from 'assert';

import deepFreeze from 'deep-freeze-strict';

import h, {c, toggleClasses} from '../h';

const defaultProps = {
    id: '',
    className: '',
    title: '',
    lang: '',
    dir: '',
    hidden: false,
    tabIndex: -1,
    accessKey: '',
};

const defaultTdProps = {
    colSpan: 1,
    rowSpan: 1,
    abbr: '',
    align: '',
    axis: '',
    height: '',
    width: '',
    ch: '',
    chOff: '',
    noWrap: false,
    vAlign: '',
    bgColor: '',
    ...defaultProps,
};

describe('h', () => {
    test('tagName', () => {
        assert.deepStrictEqual(h({tagName: 'span'}).toJSON(), {
            tagName: 'span',
            children: [],
            defaultProps,
        });
    });

    test('flat properties', () => {
        assert.deepStrictEqual(h({tagName: 'td', colSpan: 1}).toJSON(), {
            tagName: 'td',
            props: {colSpan: 1},
            defaultProps: defaultTdProps,
            children: [],
        });
    });

    test('flat and nested properties', () => {
        assert.deepStrictEqual(
            h({
                tagName: 'td',
                colSpan: 1,
                props: {colSpan: 2, id: 'id'},
            }).toJSON(),
            {
                tagName: 'td',
                props: {id: 'id', colSpan: 2},
                defaultProps: defaultTdProps,
                children: [],
            },
        );
    });

    test('options object is not mutaged', () => {
        h(
            deepFreeze({
                tagName: 'td',
                colSpan: 1,
                props: {colSpan: 2, id: 'id'},
            }),
        );
    });

    test('tag shorthand function', () => {
        assert.deepStrictEqual(
            h.td({key: 'test', colSpan: 1, children: 'str'}).toJSON(),
            {
                tagName: 'td',
                key: 'test',
                props: {colSpan: 1},
                defaultProps: defaultTdProps,
                children: [{text: 'str'}],
            },
        );
    });

    test('tag shorthand function options object is not mutated', () => {
        h.td(deepFreeze({key: 'test', colSpan: 1, children: 'str'}));
    });

    test('class property', () => {
        const objClass = {test5: true, 'test6 test7': true};
        assert.deepStrictEqual(
            h.div({
                class: [
                    'test1 test2',
                    'test3',
                    '',
                    null,
                    0,
                    false,
                    undefined,
                    [['test4']],
                    12,
                    {},
                    [],
                    objClass,
                ],
            }).class,
            {
                12: true,
                test1: true,
                test2: true,
                test3: true,
                test4: true,
                test5: true,
                test6: true,
                test7: true,
            },
        );
    });

    test('class property with primitive value', () => {
        assert.deepStrictEqual(h.div({class: 'test1 test2'}).class, {
            test1: true,
            test2: true,
        });
        assert.deepStrictEqual(h.div({class: 'test3'}).class, {test3: true});
        ['', null, 0, false, undefined, {}, []].forEach(c =>
            assert.deepStrictEqual(h.div({class: c}).class, undefined),
        );
    });

    test('class propery with object value', () => {
        assert.deepStrictEqual(
            h.div({
                class: {
                    test0: false,
                    test1: true,
                    'base1 test2': true,
                    'base1 test3': false,
                },
            }).class,
            {
                test1: true,
                base1: true,
                test2: true,
            },
        );
    });
});

test('c', () => {
    assert.deepStrictEqual(
        c('base1 test1', 'test2', 'test3', {
            test3: false,
            test4: true,
            'base2 test5': true,
            'base2 test6': false,
        }),
        {
            base1: true,
            test1: true,
            test2: true,
            test3: true,
            test4: true,
            base2: true,
            test5: true,
            test6: false,
        },
    );
});

test('toggleClasses', () => {
    assert.deepStrictEqual(
        toggleClasses(
            {
                class1: '__class1',
                class2: '__class2',
                class3: '__class3',
                class4: '__class4',
            },
            {
                class1: true,
                class3: false,
            },
        ),
        {
            __class1: true,
            __class3: false,
        },
    );
});
