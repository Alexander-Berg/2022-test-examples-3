const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');
const sForm = require('../blocks/s-form');
const sMessage = require('../blocks/s-message');

const recommendationAction = new Entity({
    block: 'f-recommendation-action',
}).mods({
    action: 'toggle-publication',
});

const rotationAction = new Entity({
    block: 'f-rotation-action',
}).mods({
    action: 'toggle-publication',
});

/**
 * Хедер страницы
 */
blocks.header = new Entity({ block: 'f-page-publications', elem: 'header' });
/**
 * Табы страницы
 */
blocks.pubsTabs = new Entity({ block: 'f-page-publications', elem: 'tabs' })
    .child(new Entity({ block: 'f-tabs', elem: 'tabs' }));
blocks.pubsTabs.recommend = new Entity({
    block: 'tabs-menu',
    elem: 'tab',
}).mods({
    type: 'recommend',
});
blocks.pubsTabs.rotate = new Entity({
    block: 'tabs-menu',
    elem: 'tab',
}).mods({
    type: 'rotate',
});
/**
 * Список публикаций
 */
blocks.pubsList = new Entity({ block: 'f-publications-list' });
/**
 * Спиннер списка
 */
blocks.pubsList.spin = new Entity({ block: 'f-publications-list', elem: 'progress' });
/**
 * Сам список
 */
blocks.pubsList.list = new Entity({ block: 'f-publications-list', elem: 'list' });

/** Первая публикация */
blocks.pubsList.list.publication1 = new Entity({ block: 'f-publication' }).nthType(1);
blocks.pubsList.list.publication1.toRecommendation = recommendationAction.copy();
blocks.pubsList.list.publication1.toRotation = rotationAction.copy();

/** Вторая публикация */
blocks.pubsList.list.publication2 = new Entity({ block: 'f-publication' }).nthType(2);
blocks.pubsList.list.publication2.toRecommendation = recommendationAction.copy();
blocks.pubsList.list.publication2.toRotation = rotationAction.copy();

/** Третья публикация */
blocks.pubsList.list.publication3 = new Entity({ block: 'f-publication' }).nthType(3);
blocks.pubsList.list.publication3.toRecommendation = recommendationAction.copy();
blocks.pubsList.list.publication3.toRotation = rotationAction.copy();

/**
 * Тайтл 1-й публикации
 */
blocks.pubsList.list.publication1.title = new Entity({ block: 'f-publication', elem: 'title' });
/**
 * ID вакансии первой публикации
 */
blocks.pubsList.list.publication1.vacancyId = new Entity({ block: 'f-publication', elem: 'group-meta' })
    .descendant(new Entity({ block: 'f-id' }));
/**
 * Пагинатор
 */
blocks.pubsList.pager = new Entity({ block: 'f-publications-list', elem: 'pager' });
/**
 * Вторая страница пагинатора
 */
blocks.pubsList.pager.page2 = new Entity({ block: 'staff-pager', elem: 'page' }).nthChild(2);

/** Вкладка рекомендаций */
blocks.recommendPane = new Entity({
    block: 'f-page-publications',
    elem: 'pane',
}).mods({
    type: 'recommend',
});

/** Инфоплашка */
blocks.recommendPane.infoblock = new Entity({
    block: 'f-publication-action',
    elem: 'info',
});

/** Ссылка на программу рекомендаций */
blocks.recommendPane.infoblock.recommendationProgramLink = blocks.link.copy();

/** Вкладка ротаций */
blocks.rotatePane = new Entity({
    block: 'f-page-publications',
    elem: 'pane',
}).mods({
    type: 'rotate',
});

/** Инфоплашка */
blocks.rotatePane.infoblock = new Entity({
    block: 'f-publication-action',
    elem: 'info',
});
/** Ссылка на правила рекомендаций */
blocks.rotatePane.infoblock.rotationRulesLink = blocks.link.copy();

/** Форма рекомендации */
blocks.recommendationForm = new Entity({ block: 'f-recommendation-form' });
/** Кнопка сабмита на форме */
blocks.recommendationForm.submit = sForm.submit.copy();
/** Ошибка отправки */
blocks.recommendationForm.error = sMessage.sMessageTypeError;
/** Поля ввода формы */
blocks.recommendationForm.fieldFirstName = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'first-name' });
blocks.recommendationForm.fieldFirstName.input = blocks.input.control.copy();
blocks.recommendationForm.fieldLastName = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'last-name' });
blocks.recommendationForm.fieldLastName.input = blocks.input.control.copy();
blocks.recommendationForm.fieldEmail = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'email' });
blocks.recommendationForm.fieldEmail.input = blocks.input.control.copy();
blocks.recommendationForm.fieldPhone = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'phone' });
blocks.recommendationForm.fieldPhone.input = blocks.input.control.copy();
blocks.recommendationForm.fieldComment = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'comment' });
blocks.recommendationForm.fieldComment.input = blocks.textAreaControl.copy();
blocks.recommendationForm.fieldIsInformed = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'is-candidate-informed' });
blocks.recommendationForm.fieldIsInformed.checkbox = blocks.checkbox.copy();
blocks.recommendationForm.fieldResume = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'attachments' });
blocks.recommendationForm.fieldResume.uploader = new Entity({
    block: 'f-attach-uploader',
});
blocks.recommendationForm.publications = new Entity({ block: 'f-recommendation-form', elem: 'field' }).mods({ type: 'publications' });
blocks.recommendationForm.publications.button = blocks.button2.copy();
blocks.recommendationForm.publications.selected1 = new Entity({
    block: 's-field',
    elem: 'plate',
}).nthChild(1);
blocks.recommendationForm.publications.selected1.delete = new Entity({
    block: 'f-removable',
    elem: 'delete',
});

/** Форма ротации */
blocks.rotationForm = new Entity({ block: 'f-rotation-form' });
/** Кнопка сабмита на форме */
blocks.rotationForm.submit = sForm.submit.copy();
/** Ошибка отправки */
blocks.rotationForm.error = sMessage.sMessageTypeError;
/** Поля ввода формы */
blocks.rotationForm.fieldReason = new Entity({ block: 'f-rotation-form', elem: 'field' }).mods({ type: 'reason' });
blocks.reasonSelect = blocks.select2Popup.copy();
blocks.reasonSelect.second = blocks.select2Item.nthType(2);
blocks.rotationForm.fieldComment = new Entity({ block: 'f-rotation-form', elem: 'field' }).mods({ type: 'comment' });
blocks.rotationForm.fieldComment.input = blocks.textAreaControl.copy();
blocks.rotationForm.fieldIsAgree = new Entity({ block: 'f-rotation-form', elem: 'field' }).mods({ type: 'is-agree' });
blocks.rotationForm.fieldIsAgree.checkbox = blocks.checkbox.copy();
blocks.rotationForm.fieldIsPrivacyNeeded = new Entity({ block: 'f-rotation-form', elem: 'field' }).mods({ type: 'is-privacy-needed' });
blocks.rotationForm.fieldIsPrivacyNeeded.checkbox = blocks.checkbox.copy();
blocks.rotationForm.publications = new Entity({ block: 'f-rotation-form', elem: 'field' }).mods({ type: 'publications' });
blocks.rotationForm.publications.button = blocks.button2.copy();
blocks.rotationForm.publications.selected1 = new Entity({
    block: 's-field',
    elem: 'plate',
}).nthChild(1);
blocks.rotationForm.publications.selected1.delete = new Entity({
    block: 'f-removable',
    elem: 'delete',
});

/** Сообщение об успехе */
blocks.successMessage = new Entity({
    block: 'f-page-publications',
    elem: 'message',
}).mods({
    type: 'success',
});

module.exports = pageObject.create(blocks);
