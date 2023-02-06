'use strict';

const {updateTestFile} = require('./file-test');

updateTestFile({
    file: 'features/common/maps-static-prefetch/maps-static-prefetch.hermione.js',
    fullTitle: 'Префетч статики БЯК Пробки',
    dataFilter: 'traffic'
});

updateTestFile({
    file: 'features/common/adapter-autoru-thumbs-price/experiments/autoru-mag-mark/autoru-thumbs-price.hermione.js',
    fullTitle: 'Колдунщик Авто.ру / Журнальная тумба Внешний вид с ценами',
    dataFilter: 'autoru/thumbs-price'
});

updateTestFile({
    file: 'features/common/social-snippet/social-snippet.hermione.js',
    fullTitle: 'Социальный сниппет Виды соц сниппетов Instagram',
    dataFilter: 'social_snippet'
});
