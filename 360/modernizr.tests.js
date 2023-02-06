Modernizr.addTest('is-pdd', function() {
    return !!Daria.Config.pddDomain;
});

Modernizr.addTest('draganddrop', function() {
    var div = document.createElement('div');
    return ('draggable' in div) || ('ondragstart' in div && 'ondrop' in div);
});

// Нам надо определить, что не только есть dnd события, но и что через него можно аттачить файлы.
// В IE есть drag-n-drop, но файлы через него заливать нельзя.
Modernizr.addTest('draganddrop-files', function() {
    return Boolean(Modernizr['draganddrop'] && window['FormData'] && Modernizr['filereader']);
});

// для загрузки файлов по одному из multiple-input нам нужна FormData
Modernizr.addTest('input-multiple', function() {
    return Boolean(Modernizr['input']['multiple'] && window['FormData'] && Modernizr['filereader']);
});

/**
 * Проверка поддержки 2го аргумента в функциях add и remove в classList
 */
Modernizr.addTest('classlist-second-arg', function() {
    if (!Modernizr.classlist) {
        return false;
    }

    var tmp = document.createElement('div');
    tmp.classList.add('cl1', 'cl2');

    return tmp.className !== 'cl1';
});

