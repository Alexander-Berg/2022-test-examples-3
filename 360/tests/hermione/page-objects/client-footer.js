const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;
const CommonObjects = {};

//ссылки в футере
CommonObjects.footer = new Entity('.footer');
CommonObjects.footerHelpAndSupportLink =
    new Entity('//a[text() = \'Справка и поддержка\' and contains(@class, \'footer__link\')]');
CommonObjects.footerSupportLink =
    new Entity('//a[text() = \'Поддержка\' and contains(@class, \'footer__link\')]');
CommonObjects.footerFeedbackLink =
    new Entity('//a[text() = \'Обратная связь\' and contains(@class, \'footer__link\')]');
CommonObjects.footerHelpAndFeedbackLink =
    new Entity('//a[text() = \'Помощь и обратная связь\' and contains(@class, \'footer__link\')]');
CommonObjects.footerBlogLink = new Entity('//a[text() = \'Блог\' and contains(@class, \'footer__link\')]');
CommonObjects.footerDevsLink = new Entity('//a[text() = \'Разработчикам\' and contains(@class, \'footer__link\')]');
CommonObjects.footerRulesLink =
    new Entity('//a[text() = \'Условия использования\' and contains(@class, \'footer__link\')]');
CommonObjects.footerResearchesLink =
    new Entity('//a[text() = \'Участие в исследованиях\' and contains(@class, \'footer__link\')]');
CommonObjects.footer.copyright = new Entity('.footer__copyright');

module.exports = {
    common: bemPageObject.create(CommonObjects)
};
