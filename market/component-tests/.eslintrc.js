const RESTRICTED_RAMDA_IMPORTS = {
    name: 'ramda',
    importNames: ['path', 'pathOr', 'default'],
    message: 'path/pathOr are banned, use optional chaining instead',
};

const ALL_RESTRICTED_IMPORTS = [RESTRICTED_RAMDA_IMPORTS];

const RESTRICTED_ROUTER_BUILD_URL_RULE = {
    // @see https://st.yandex-team.ru/MARKETPARTNER-34774 не дает вызвать `stout.router.buildURL` и `router.buildURL`
    selector:
        // stout.router.buildURL
        'MemberExpression[property.name="buildURL"] > MemberExpression[property.name="router"] > Identifier[name="stout"], ' +
        // router.buildURL
        'Program:has(ImportDeclaration[source.value="@yandex-market/stout"] Identifier[name="router"])' +
        ' MemberExpression[property.name="buildURL"] > Identifier[name="router"]',
    message: "Используй `import {buildURL} from '@yandex-market/b2b-core/app';` для корректного проброса euid",
};

module.exports = {
    plugins: [
        'prettier',
        'eslint-plugin-market',
        'eslint-plugin-import',
        'react-hooks',
        '@typescript-eslint',
        '@yandex-market/eslint-plugin-no-object-freeze',
    ],

    rules: {
        'prettier/prettier': 'error',
        'import/prefer-default-export': 'off',
        'react/display-name': 'off',
        'import/extensions': 'off',
        'arrow-body-style': 'off',
        'prefer-arrow-callback': 'off',
        'import/order': 'off', // MARKETPARTNER-12970
        'market/i18n/static-keyset-name': 'error',
        'market/no-global-moment-locale': 'error',
    },

    extends: [
        require.resolve('@yandex-market/codestyle'),
        'plugin:react-hooks/recommended',
        'plugin:prettier/recommended',
        'prettier',
    ],

    parser: '@typescript-eslint/parser',

    root: true,

    env: {
        es6: true,
        'jest/globals': true,
    },

    parserOptions: {
        sourceType: 'module',
        ecmaFeatures: {
            jsx: true,
        },
        impliedStrict: true,
    },

    settings: {
        'import/resolver': {
            node: {
                moduleDirectory: ['node_modules', '.'],
                extensions: ['.js', '.ts', '.jsx', '.tsx'],
            },
        },
        flowtype: {
            onlyFilesWithFlowAnnotation: true,
        },
        react: {
            version: 'detect',
        },
    },

    overrides: [
        {
            files: ['**/*.{ts,tsx,d.ts}'],
            rules: {
                'flowtype/require-valid-file-annotation': 0,
                'flowtype/no-types-missing-file-annotation': 0,
                'no-restricted-imports': [
                    'error',
                    {
                        paths: ALL_RESTRICTED_IMPORTS,
                    },
                ],
                'no-restricted-syntax': ['error', RESTRICTED_ROUTER_BUILD_URL_RULE],
                'jest/no-done-callback': 'off',
                '@typescript-eslint/ban-ts-comment': [
                    'error',
                    {
                        'ts-expect-error': 'allow-with-description',
                        'ts-ignore': true,
                        'ts-nocheck': true,
                        'ts-check': false,
                        minimumDescriptionLength: 5,
                    },
                ],
                '@typescript-eslint/array-type': [
                    'error',
                    {
                        default: 'array-simple',
                        readonly: 'generic',
                    },
                ],
                '@typescript-eslint/no-invalid-void-type': 'error',
                '@typescript-eslint/ban-types': [
                    'error',
                    {
                        extendDefaults: true,
                        types: {
                            Function: null,
                            Object: null,
                            object: false,
                            '{}': null,
                        },
                    },
                ],
                '@yandex-market/no-object-freeze/no-object-freeze': 'error',

                // Исправляет ошибки в импортах
                'no-unused-vars': 'off',
                '@typescript-eslint/no-unused-vars': [
                    'error',
                    {vars: 'all', args: 'after-used', ignoreRestSiblings: true, argsIgnorePattern: '^_'},
                ],

                // Исправляет ошибку при импорте реакта в TypeScript
                // @see https://github.com/iamturns/eslint-config-airbnb-typescript/blob/master/lib/shared.js#L169
                'no-use-before-define': 'off',
                '@typescript-eslint/no-use-before-define': 'off',

                // Правила выключены, так как проверяются на уровне компилятора TypeSrcipt или работают некорректно с TypeScript
                // @see https://github.com/iamturns/eslint-config-airbnb-typescript/blob/master/lib/shared.js#L245
                'constructor-super': 'off',
                'getter-return': 'off',
                'no-const-assign': 'off',
                'no-dupe-args': 'off',
                'no-dupe-class-members': 'off',
                'no-dupe-keys': 'off',
                'no-func-assign': 'off',
                'no-new-symbol': 'off',
                'no-obj-calls': 'off',
                'no-redeclare': 'off',
                'no-this-before-super': 'off',
                'no-undef': 'off',
                'no-unreachable': 'off',
                'no-unsafe-negation': 'off',
                'valid-typeof': 'off',
                'import/named': 'off',
                'import/no-unresolved': 'off',
                'import/no-named-as-default-member': 'off',
                'class-methods-use-this': 'off',
            },
        },
    ],
};
