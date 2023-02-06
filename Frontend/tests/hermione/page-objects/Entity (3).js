const PageObject = require('@yandex-int/bem-page-object');
const inherit = require('inherit');

module.exports = inherit(PageObject.Entity, {}, { preset: 'react' });
