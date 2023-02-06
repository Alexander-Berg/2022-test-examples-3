import j from 'jscodeshift';
import { assert } from 'chai';
import { createConflict } from '../../../src/plugins/tide-mover/utils';

describe('tide-mover / utils', () => {
    describe('createConflict', () => {
        it('should create a conflict between two non empty arrays', () => {
            const expNode = j(`describe('exp', () => {});`).find(j.ExpressionStatement).get().value;
            const ast = j(`
            it('first it');
            describe('prod', () => {});
            describe('second describe', () => {});
            describe('last describe');`);
            const body = ast.find(j.Program).get().value.body;
            const bodyIndex = 1;

            const expectedCode = `
            it('first it');
            /* <<<<<<< production */
            describe('prod', () => {});
            /*
            =======
            */
            describe('exp', () => {});/*
            >>>>>>> experiment */
            describe('second describe', () => {});
            describe('last describe');`;

            createConflict(j, body, [body[bodyIndex]], [expNode], 1);

            assert.equal(expectedCode, ast.toSource());
        });
    });
});
