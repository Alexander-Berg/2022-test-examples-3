document.addEventListener('DOMContentLoaded', __hermioneSetFontLoadedClass);

if (document.readyState === 'complete' || document.readyState === 'loaded') {
    __hermioneSetFontLoadedClass();
}

/**
 * Принудительно устанавливаем класс, символизирующий, что шрифт загружен.
 * Так как шрифт заинлайнен в соседнем css-файле, то он должен отрендериться мгновенно.
 *
 * См. https://github.yandex-team.ru/lego/islands/blob/dev/packages/yandex-font/src/browser.css
 */
function __hermioneSetFontLoadedClass() {
    document.documentElement.classList.add('font_loaded');
}
