import {buildOperatorScriptsRequestBody} from '../utils';
import {DependentField} from '../types';

/** ========================== Тестовые данные ============================ */

const testCode1 = 'code1';
const testCode2 = 'code2';
const testCode3 = 'code3';
const testCode4 = 'code4';
const testCode5 = 'code5';
const testCode6 = 'code6';

const testDependents1: DependentField[] = [
    {code: testCode1, value: 100},
    {code: testCode2, value: 'hello'},
    {code: testCode3, value: false},
    {code: testCode4, value: null},
    {code: testCode5, value: undefined},
];

const testGid1 = 'gid@test1';
const testGid2 = 'gid@test2';

const testObjectWithGid1 = {gid: testGid1};
const testObjectWithGid2 = {gid: testGid2};

const testDependents2: DependentField[] = [{code: testCode1, value: testObjectWithGid1}];

const testObjectWithoutGid1 = {a: 'hello'};
const testObjectWithoutGid2 = {b: 'hi'};

const testDependents3: DependentField[] = [{code: testCode1, value: testObjectWithoutGid1}];

const testDependents4: DependentField[] = [{code: testCode1, value: [testObjectWithGid1, testObjectWithGid2]}];

const testDependents5: DependentField[] = [{code: testCode1, value: [testObjectWithoutGid1, testObjectWithoutGid2]}];

const testDependents6: DependentField[] = [
    {code: testCode1, value: undefined},
    {code: testCode2, value: null},
    {code: testCode3, value: 'hello'},
    {code: testCode4, value: testObjectWithGid1},
    {code: testCode5, value: testObjectWithoutGid1},
    {code: testCode6, value: [testObjectWithGid1, testObjectWithoutGid1]},
];

/** ======================================================================= */

describe('Тестирование вспомогательных утилит operatorScripts', () => {
    describe('buildOperatorScriptsRequestBody', () => {
        it('Если не передать зависимые поля (или передать пустой массив) возвращаем пустой объект атрибутов', () => {
            const requestBody1 = buildOperatorScriptsRequestBody();
            const requestBody2 = buildOperatorScriptsRequestBody([]);

            expect(requestBody1).toHaveProperty('attributes', {});
            expect(requestBody2).toHaveProperty('attributes', {});
        });

        it('При исаользовании примитов в значении зависимых они попадают как есть в объект атрибутов (кроме undefined)', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents1);

            expect(requestBody).toHaveProperty(['attributes', testCode1], 100);
            expect(requestBody).toHaveProperty(['attributes', testCode2], 'hello');
            expect(requestBody).toHaveProperty(['attributes', testCode3], false);
            expect(requestBody).toHaveProperty(['attributes', testCode4], null);

            /** Проверяем, что поля undefined не проросло в атрибуты */
            expect(requestBody).not.toHaveProperty(['attributes', testCode5]);
        });

        it('При предачи объектов с gid в зависимых полях получаем только значения gid в объекте атрибутов', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents2);

            expect(requestBody).toHaveProperty(['attributes', testCode1], testGid1);
        });

        it('При передаче объектов без gid в зависимых полях они попадают как есть в объект атрибутов', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents3);

            expect(requestBody).toHaveProperty(['attributes', testCode1], testObjectWithoutGid1);
        });

        it('При передаче массива объектов с gid в зависимых поля в объект атрибутов попадает массив gid', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents4);

            expect(requestBody).toHaveProperty(['attributes', testCode1], [testGid1, testGid2]);
        });

        it('При передаче массива объектов без gid в зависымых полях эти объекты попадают как есть в объект атрибутов', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents5);

            expect(requestBody).toHaveProperty(
                ['attributes', testCode1],
                [testObjectWithoutGid1, testObjectWithoutGid2]
            );
        });

        it('При передаче разных типов полей выдаем корректный объект атрибутов', () => {
            const requestBody = buildOperatorScriptsRequestBody(testDependents6);

            expect(requestBody).not.toHaveProperty(['attributes', testCode1]);
            expect(requestBody).toHaveProperty(['attributes', testCode2], null);
            expect(requestBody).toHaveProperty(['attributes', testCode3], 'hello');
            expect(requestBody).toHaveProperty(['attributes', testCode4], testGid1);
            expect(requestBody).toHaveProperty(['attributes', testCode5], testObjectWithoutGid1);
            expect(requestBody).toHaveProperty(['attributes', testCode6], [testGid1, testObjectWithoutGid1]);
        });
    });
});
