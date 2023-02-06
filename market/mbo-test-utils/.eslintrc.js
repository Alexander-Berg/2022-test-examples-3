const tsConfigDir = __dirname;
const jsExtensions = ['.js'];
const tsExtensions = ['.ts', '.tsx', '.d.ts'];
const assetsExtensions = ['jpg', 'jpeg', 'png', 'svg', 'css', 'scss', 'json'];

module.exports = {
  extends: ['./node_modules/@yandex-market/mbo-linters/.eslintrc.js'],
  globals: {
    NODE_ENV: true,
  },
  parserOptions: {
    sourceType: "module",
    tsconfigRootDir: __dirname,
    project: ['./tsconfig.json'],
  },
  settings: {
    react: { version: 'detect' },
    'import/parsers': { '@typescript-eslint/parser': tsExtensions },
    'import/resolver': {
      typescript: {
        project: tsConfigDir,
      },
    },
    'import/extensions': jsExtensions.concat(tsExtensions),
    'import/ignore': ['node_modules', `\\.(${assetsExtensions.join('|')})$`],
  },
  rules: {
    'default-case': "off",
    'no-shadow': "off",
    'no-empty': 'off',
    'no-alert': 'off',
    'no-else-return': 'off',
    'no-nested-ternary': 'off',
    'prefer-template': 'off',
    'prefer-destructuring': 'off',
    'import/no-extraneous-dependencies': [
      'error',
      {
        devDependencies: ['src/**/*.test.ts', 'src/**/*.test.tsx', 'src/**/test/**/*.{ts,tsx}'],
      },
    ],
    'import/extensions': 'off',
    'import/named': 'off',
    'camelcase': [0],
    'react/destructuring-assignment': 'off',
    'react/require-default-props': 'off',
    'jsx-a11y/anchor-is-valid': 'off',
    'jsx-a11y/label-has-for': 'off',
    'jsx-a11y/label-has-associated-control': 'off',
    'jsx-a11y/mouse-events-have-key-events': 'off',
    'jsx-a11y/accessible-emoji': 'off',
    '@typescript-eslint/no-non-null-assertion': 'off',
    '@typescript-eslint/no-object-literal-type-assertion': 'off',
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/ban-ts-ignore': 'off',
    '@typescript-eslint/explicit-module-boundary-types': 'off',
    '@typescript-eslint/camelcase': 'off',
    '@typescript-eslint/no-unused-vars': [
      'error',
      {
        vars: 'all',
        args: 'after-used',
        ignoreRestSiblings: true,
        varsIgnorePattern: '^_',
        argsIgnorePattern: '^_',
      },
    ],
  },
};
