import createQueryParamAccessDecorator from 'src/main/server/decorators/createQueryParamAccessDecorator';
import HtmlPage from 'src/main/server/pages/HtmlPage';

import template from './template';

const queryParamAccessDecorator = createQueryParamAccessDecorator('_aflt_access', '1');

function Test() {}

module.exports = HtmlPage.create(Test).decorate(queryParamAccessDecorator);

Test.prototype.template = template;
