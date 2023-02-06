'use strict';

// spec_5: Название типа фичи должно начинаться с большой буквы

const _ = require('lodash');
const j = require('jscodeshift');

const { TEST_CALLEE_NAMES, YML_SPECS_TITLE_KEYS } = require('./../constants');
const { astReplace, getSpecsTitle, parseSpecsTitle } = require('./../utils');

const KEY = YML_SPECS_TITLE_KEYS[2];
const replace = value => _.upperFirst(value);

module.exports = {
    testpalm: data => {
        data[KEY] = replace(data[KEY]);

        return data;
    },
    hermione: ast => {
        ast
            .find(j.CallExpression, { callee: { name: TEST_CALLEE_NAMES[0] } })
            .forEach(path => {
                const argNode = _.get(path, 'node.arguments.0');

                if (argNode.type === 'Literal') {
                    const obj = parseSpecsTitle(argNode.value);

                    obj[KEY] = replace(obj[KEY]);

                    argNode.value = getSpecsTitle(obj);
                } else {
                    astReplace(argNode, path, KEY, replace);
                }
            });

        return ast;
    }
};
