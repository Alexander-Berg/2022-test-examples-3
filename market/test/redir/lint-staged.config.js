/**
 * @see {@link https://github.com/okonet/lint-staged#configuration}
 */
module.exports = {
    '**/*.js': ['prettier --write', 'eslint --fix'],
    '**/*.json': ['prettier --write'],
};
