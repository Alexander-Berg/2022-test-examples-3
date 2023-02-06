/**
 * С пар-ом yandex_login=stas.mihailov666 файлы на Диск не загружаются. Стабим ручку
 */
BEM.decl('disk-save-action', {
    _processSavingFile: function() {
        this.trigger('destruct');
    }
});
