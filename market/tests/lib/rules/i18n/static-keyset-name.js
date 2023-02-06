'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/i18n/static-keyset-name.js');

const ruleTester = new RuleTester({parser: 'babel-eslint'});

const expectedErrors = {
    default: [{ message: rule.msg.default }],
    staticString: [{ message: rule.msg.staticString }],
    jsxIdValue: [{message: rule.msg.jsxIdValue}],
};

ruleTester.run('i18n/static-keyset-name', rule, {
    valid: [
        {
            code: 'i18n`keyset-name:key-name`'
        },
        {
            code: 'i18n`keyset.name:key-name`'
        },
        {
            code: 'i18n`keyset.name:${someVar}`'
        },
        {
            code: '<I18n id="keyset-name:key-name" />'
        },
        {
            code: '<I18n id="keyset.name-kek:key-name" />'
        },
        {
            code: '<I18n id={`keyset.name:${someVar}`} />'
        },
        {
            code: "<I18n id={`pages.delivery:minimal-sum.${isEnabled ? 'on' : 'off'}`} />"
        },
        {
            code: "<I18n id={`shared.stat-placement:table.header.${rowName}`} />"
        },
        {
            code: '<I18n id="shared.stat-placement:filters.entity.clicks" />'
        },
        {
            code: '<I18n id={"shared.stat-placement:filters.entity.clicks"} />'
        },
        {
            code: '[<I18n id="shared.costs-and-sales:filters.entity.clicks" key="costsAndSales.table.header.clicks" />,]'
        },
    ],

    invalid: [
        {
            code: 'i18n`${someVar}`',
            errors: expectedErrors.default
        },
        {
            code: 'i18n`keyset${someVar}`',
            errors: expectedErrors.default
        },
        {
            code: '<I18n id="invalid-key" />',
            errors: expectedErrors.staticString
        },
        {
            code: '<I18n id={`keyset${someVar}`} />',
            errors: expectedErrors.staticString
        },
        {
            code: '<I18n id={someVar} />',
            errors: expectedErrors.jsxIdValue
        },
        {
            code: '<I18n id={cond ? "keyset.name:key1" : "keyset.name:key2"} />',
            errors: expectedErrors.jsxIdValue
        },
        {
            code: '<I18n id={obj.prop} />',
            errors: expectedErrors.jsxIdValue
        },
    ]
});
