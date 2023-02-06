/**
 * Убираем под флагом панель социальности в конец страницы
 * чтобы снять скриншоты height > 100vh
 */
if (window.location.search.indexOf('hermione_social_panel=absolute') >= 0) {
    document.body.classList.add('social-panel-absolute');
}
