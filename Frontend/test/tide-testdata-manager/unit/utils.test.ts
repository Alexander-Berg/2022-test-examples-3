import url from 'url';
import _ from 'lodash';
import sinon from 'sinon';
import { assert } from 'chai';
import {
    matchUrl,
    replaceUrl,
    walk,
    walkAndUnwrapJson,
} from '../../../src/plugins/tide-testdata-manager/utils';
import { UrlPlain } from '../../../src/plugins/tide-testdata-manager/types';

describe('tide-testdata-manager / utils', () => {
    describe('matchUrl', () => {
        it('should match pathname and query', () => {
            const subject = url.parse(
                '/api/method/?p=1&dump=reqans&exp_flags=font-size%3D10&exp_flags=hideads%3D1&foreverdata=1234&lr=213&no-tests=1&&promo=nomooa&text=something2',
                true,
            );
            const pattern = {
                pathname: '/api/method',
                query: [['exp_flags', 'font-size=10']],
            } as UrlPlain;

            assert(matchUrl(subject, pattern));
        });

        it('should correctly handle undefined query items', () => {
            const subject = url.parse(
                '/api/method/?p=1&dump=reqans&exp_flags=font-size%3D10&exp_flags=hideads%3D1&foreverdata=1234&lr=213&no-tests=1&&promo=nomooa&text=something2',
                true,
            );
            const pattern = {
                query: [[undefined, undefined]],
            } as UrlPlain;

            assert(matchUrl(subject, pattern));
        });
    });

    describe('replaceUrl', () => {
        it('should replace one query param with the other', () => {
            const subject = url.parse(
                '/search/?text=1&exp_flags=some-flag=1&exp_flags=second-flag',
                true,
            );
            const pattern: UrlPlain = {
                query: [['text', '1']],
            } as UrlPlain;
            const replacement: UrlPlain = {
                query: [['text', 'new-text']],
            } as UrlPlain;

            const expected = '/search/?text=new-text&exp_flags=some-flag%3D1&exp_flags=second-flag';

            const actual = url.format(replaceUrl(subject, pattern, replacement));

            assert.deepEqual(actual, expected);
        });

        it('should add new query parameter if pattern is undefined', () => {
            const subject = url.parse(
                '/search/?text=1&exp_flags=some-flag=1&exp_flags=second-flag',
                true,
            );
            const pattern: UrlPlain = {
                query: [[undefined, undefined]],
            } as UrlPlain;
            const replacement: UrlPlain = {
                query: [['exp_flags', 'new-flag']],
            } as UrlPlain;

            const expected =
                '/search/?exp_flags=new-flag&exp_flags=some-flag%3D1&exp_flags=second-flag&text=1';

            const actual = url.format(replaceUrl(subject, pattern, replacement));

            assert.deepEqual(actual, expected);
        });
    });

    describe('walk', () => {
        it('should walk over every object property', () => {
            const obj = {
                prop: 'value',
                nested: {
                    arr: [1, 2, 3],
                    deeplyNested: {
                        prop: 'another-value',
                    },
                },
            };

            const cb = sinon.stub().returns(true);

            walk(obj, cb);

            assert(cb.calledWith(obj, 'prop', ['prop']));
            assert(cb.calledWith(obj, 'nested', ['nested']));
            assert(cb.calledWith(obj.nested, 'arr', ['nested', 'arr']));
            assert(cb.calledWith(obj.nested.arr, '0', ['nested', 'arr', '0']));
            assert(cb.calledWith(obj.nested.arr, '1', ['nested', 'arr', '1']));
            assert(cb.calledWith(obj.nested.arr, '2', ['nested', 'arr', '2']));
            assert(cb.calledWith(obj.nested, 'deeplyNested', ['nested', 'deeplyNested']));
            assert(
                cb.calledWith(obj.nested.deeplyNested, 'prop', ['nested', 'deeplyNested', 'prop']),
            );
        });
    });

    describe('walkAndUnwrapJson', () => {
        it('should walk over every object property, unwrapping json if needed', () => {
            const input = {
                prop: 'value',
                nested: {
                    anotherProp: '{invalid json',
                    data: '{"deeplyNested":"{\\"obj\\":\\"{\\\\\\"property\\\\\\":\\\\\\"value\\\\\\"}\\",\\"arr\\":[1]}","num":1}',
                },
            };

            // Такой workaround вместо использования обычных sinon.calledWith нужен из-за того,
            // что объект меняется в процессе обхода, а assert происходит лишь в конце. Нужно сохранять состояние
            const calls: any[] = [];
            const cb = (obj, prop, path): boolean => {
                calls.push([_.cloneDeep(obj), prop, _.cloneDeep(path)]);
                return true;
            };

            walkAndUnwrapJson(input, cb);

            assert(calls.some((item) => _.isEqual(item, [input, 'prop', ['prop']])));
            assert(calls.some((item) => _.isEqual(item, [input, 'nested', ['nested']])));
            assert(
                calls.some((item) =>
                    _.isEqual(item, [input.nested, 'anotherProp', ['nested', 'anotherProp']]),
                ),
            );
            const data = JSON.parse(input.nested.data);
            assert(
                calls.some((item) =>
                    _.isEqual(item, [{ ...input.nested, data }, 'data', ['nested', 'data']]),
                ),
            );
            const deeplyNested = JSON.parse(data.deeplyNested);
            assert(
                calls.some((item) =>
                    _.isEqual(item, [
                        { ...data, deeplyNested },
                        'deeplyNested',
                        ['nested', 'data', 'deeplyNested'],
                    ]),
                ),
            );
            assert(calls.some((item) => _.isEqual(item, [data, 'num', ['nested', 'data', 'num']])));
            const obj = JSON.parse(deeplyNested.obj);
            assert(
                calls.some((item) =>
                    _.isEqual(item, [
                        { ...deeplyNested, obj },
                        'obj',
                        ['nested', 'data', 'deeplyNested', 'obj'],
                    ]),
                ),
            );

            assert(
                calls.some((item) =>
                    _.isEqual(item, [
                        deeplyNested,
                        'arr',
                        ['nested', 'data', 'deeplyNested', 'arr'],
                    ]),
                ),
            );
            assert(
                calls.some((item) =>
                    _.isEqual(item, [
                        obj,
                        'property',
                        ['nested', 'data', 'deeplyNested', 'obj', 'property'],
                    ]),
                ),
            );
            assert(
                calls.some((item) =>
                    _.isEqual(item, [
                        deeplyNested.arr,
                        '0',
                        ['nested', 'data', 'deeplyNested', 'arr', '0'],
                    ]),
                ),
            );
        });
    });
});
