import assert from 'assert';

import {
    notJinja,
    inVars,
    supportedVars,
} from 'message_templates/shared/validations';

const AVAILABLE_VARS = ['string_var', 'number_var'];

describe('supportedVars', () => {
    it('should support only STRING AND NUMBER', () => {
        const vars = supportedVars([
            {type: 'STRING', name: 'string_var'},
            {type: 'NUMBER', name: 'number_var'},
            {type: 'BOOLEAN', name: 'boolean_var'},
            {type: 'NUMBER_ARRAY', name: 'number_array_var'},
            {type: 'OBJECT', name: 'object_var'},
            {type: 'OBJECT_ARRAY', name: 'object_array_var'},
            {type: 'UNKNOWN', name: 'unknown_var'},
        ]);

        assert.deepStrictEqual(vars, AVAILABLE_VARS);
    });
});

describe('notJinja validation', () => {
    it('should be valid if undefined', () => {
        assert.ok(notJinja(undefined));
    });

    it('should be valid if null', () => {
        assert.ok(notJinja(null));
    });

    it('should be valid if empty', () => {
        assert.ok(notJinja(''));
    });

    it('should be valid if blank', () => {
        assert.ok(notJinja(' '));
    });

    it('should be valid if simple text', () => {
        assert.ok(notJinja('Welcome to Yandex!'));
    });

    it('should be valid if similar to groovy', () => {
        assert.ok(notJinja('${}<%%>'));
        assert.ok(notJinja(' ${ } <% %> '));
        assert.ok(notJinja(' } ${ %> <% '));
        assert.ok(notJinja('y${a }nd<%e%>x'));
    });

    it('should be valid if contains groovy', () => {
        assert.ok(notJinja('Welcome to <% def str = "Yandex!" %>'));
    });

    it('should not be valid if contains jinja', () => {
        assert.ok(!notJinja('{{}}{%%}'));
        assert.ok(!notJinja('{{ }} {% %}'));
        assert.ok(!notJinja('Y{{a }} n d {% e%}x'));
    });

    it('should be valid if contains similar to jinja', () => {
        assert.ok(notJinja('}}{{%}{%'));
        assert.ok(notJinja('{ {} }'));
        assert.ok(notJinja('{ %% }'));
        assert.ok(notJinja('{{}{%}'));
        assert.ok(notJinja('y{a{n}}{d%e%}x'));
    });
});

describe('inVars validation', () => {
    it('should be valid if undefined', () => {
        assert.ok(inVars(undefined, AVAILABLE_VARS));
    });

    it('should be valid if null', () => {
        assert.ok(inVars(null, AVAILABLE_VARS));
    });

    it('should be valid if empty', () => {
        assert.ok(inVars('', AVAILABLE_VARS));
    });

    it('should be valid if blank', () => {
        assert.ok(inVars(' ', AVAILABLE_VARS));
    });

    it('should be valid if simple text', () => {
        assert.ok(inVars('Welcome to Yandex!', AVAILABLE_VARS));
    });

    it('should be valid if contains available vars', () => {
        assert.ok(inVars('Just ${string_var}', AVAILABLE_VARS));
        assert.ok(inVars('Just $string_var', AVAILABLE_VARS));
        assert.ok(inVars('Just ${string_var} or string_var', AVAILABLE_VARS));
        assert.ok(inVars('Just ${number_var}', AVAILABLE_VARS));
        assert.ok(inVars('Just $number_var', AVAILABLE_VARS));
        assert.ok(inVars('Just ${number_var} or number_var', AVAILABLE_VARS));
        assert.ok(
            inVars('Just ${string_var} and ${number_var}', AVAILABLE_VARS)
        );
        assert.ok(inVars('Just $string_var and $number_var', AVAILABLE_VARS));
        assert.ok(
            inVars('Just ${string_var} and ${string_var}', AVAILABLE_VARS)
        );
        assert.ok(inVars('Just $string_var and $string_var', AVAILABLE_VARS));
    });

    it('should not be valid if contains unavailable vars', () => {
        assert.ok(!inVars('Just ${boolean_var}', AVAILABLE_VARS));
        assert.ok(!inVars('Just ${number_array_var}', AVAILABLE_VARS));
        assert.ok(!inVars('Just ${object_var}', AVAILABLE_VARS));
        assert.ok(!inVars('Just ${object_array_var}', AVAILABLE_VARS));
        assert.ok(!inVars('Just ${unknown_var}', AVAILABLE_VARS));
        assert.ok(!inVars('Just $boolean_var', AVAILABLE_VARS));
        assert.ok(!inVars('Just $number_array_var', AVAILABLE_VARS));
        assert.ok(!inVars('Just $object_var', AVAILABLE_VARS));
        assert.ok(!inVars('Just $object_array_var', AVAILABLE_VARS));
        assert.ok(!inVars('Just $unknown_var', AVAILABLE_VARS));
    });
});
