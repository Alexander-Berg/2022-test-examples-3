/**
 * Имитирует Api ПП, которое позволяет сохранять правила подмены доменов в адресной строке.
 * @see https://st.yandex-team.ru/TURBOUI-1130
 */
(function() {
    if (window.location.search.indexOf('yabro-override-domain-api-v2=1') === -1) {
        return;
    }

    window.yandex || (window.yandex = {});
    window.yandex.publicFeature || (window.yandex.publicFeature = {});
    window.overrides || (window.overrides = []); // Сюда будем сохранять все правила, переданные в ручку
    window.hrefsAtOverride = [];

    window.yandex.publicFeature.addOverrideHostEntries = function(overrides) {
        // Запоминаем url документа на момент последнего вызова функции
        window.hrefsAtOverride.push(window.location.href);
        window.overrides = window.overrides.concat(overrides);
    };
})();
