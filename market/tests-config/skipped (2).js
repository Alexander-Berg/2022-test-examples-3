const {forPlatform} = require('./textFormat');

// скип-пак переехал в текстовые файлы skipped.*.txt
module.exports = {
    partner_desktop: forPlatform('desktop'),
    partner_touch: forPlatform('touch'),
};
