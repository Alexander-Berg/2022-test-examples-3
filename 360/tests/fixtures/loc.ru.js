(function(){    /* Это автоматически сгенеренный файл. Не редактируйте его самостоятельно. */
var i18n = i18n || {};

i18n['project'] = i18n['project'] || {};


i18n['project']['keyset'] = {
    'common': function(params) {return (
        'Ошибка!'
    )},
    'with_param': function(params) {return (
        'Не удалось сохранить файл «' + params['name'] + '» на Диск.'
    )},
    'with_param_and_custom_tag': function(params) {return (
        'Файл «' + params['name'] + '» перемещен в папку «<x-folder\/>».'
    )},
    'with_not_self-closed_tag': function(params) {return (
        'Файл «' + params['name'] + '» скопирован в папку «<x-folder>Изображения</x-folder>».'
    )},
    'with_only_tag': function(params) {return (
        '<x-link\/>'
    )},
    'with_absent_child': function(params) {return (
        'Нету дитятки «<x-Sasha\/>».'
    )},
    'with_not_string_ref': function(params) {return (
        'Странный child.'
    )},
    'with_deprecated_ref': function(params) {return (
        'Файл перемещен в папку «<x-folder\/>».'
    )},
    'with_className': function(params) {return (
        'Просто текст'
    )},
    'with_props_tag': function(params) {return (
        'Просто текст'
    )},
    'with_props_tag_and_className': function(params) {return (
        'Просто текст'
    )},
    'with_props_tag_and_only_tag': function(params) {return (
        '<x-link\/>'
    )},
    'double_clone': function(params) {return (
        'Press <x-key>Ctrl</x-key> + <x-key>S</x-key>'
    )},
    'double_br': function(params) {return (
        '1<br/>2<br></br>3'
    )}
};

    if (module && module.exports) {        module.exports = i18n;    } else {        window.i18n = window.i18n || {};        window.i18n["ru"] = i18n;    }})();
