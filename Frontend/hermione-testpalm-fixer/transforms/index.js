'use strict';

const _ = require('lodash');

// https://wiki.yandex-team.ru/users/dakhetov/Specifikacija-yml-fajjlov/Specifiuacija-yml-fajjlov-2/

const transforms = {
    // spec_2: Название кейса должно быть лаконичным. Оно не должно содержать слов, подчеркивающих назначение самих тест-кейсов, например: проверка, корректность и так далее
    'no-unexpected-test': (tool, data) => require('./no-unexpected-test')[tool](data),
    // spec_4: Название фичи должно начинаться с большой буквы
    'capitalized-feature': (tool, data) => require('./capitalized-feature')[tool](data),
    // spec_5: Название типа фичи должно начинаться с большой буквы
    'capitalized-type': (tool, data) => require('./capitalized-type')[tool](data),
    // spec_7: Название тест-кейса должно начинаться с большой буквы. Исключая случаи со специальными словами
    'capitalized-test': (tool, data) => require('./capitalized-test')[tool](data),
    // spec_10: В названии фичи не может содержаться цель тестирования. К примеру, Проверка корректности
    // spec_11: В названии фичи не должно встречаться слово функциональность. | Не используем слово Эксперимент в названии эксперимента, и слово Фича в названии фичи. Стараемся давать название, из которого сразу понятен контекст
    // Название фичи должно быть включено в разрешенный список
    'no-unexpected-feature': (tool, data) => require('./no-unexpected-feature')[tool](data),
    // spec_13: Все -do: -assert должны быть с маленькой буквы
    'capitalized-step': (tool, data) => require('./capitalized-step')[tool](data),
    // Тег должен быть включен в разрешенный список
    'no-unexpected-tags': (tool, data) => require('./no-unexpected-tags')[tool](data),
    // В files должны быть только файлы тестов или TODO
    'no-unexpected-files': (tool, data) => require('./no-unexpected-files')[tool](data)
};

const executeTransforms = (tool, data) => {
    const result = _.keys(transforms)
        .reduce((data, transform) => transforms[transform](tool, data), data);

    return tool === 'testpalm' ? _.pickBy(result, _.identity) : result;
};

module.exports = {
    executeTransforms
};
