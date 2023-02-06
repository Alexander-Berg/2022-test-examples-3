const assert = require('chai').assert;

/**
 * Проверка рекламных блоков на странице.
 * Может проверять точное совпадение id объявлений или просто количество загруженных блоков
 * @param {String[]|Number} advTestTarget проверяемый наабор рекламы (массив id или число объявлений)
 */
module.exports = async function ywAssertAdv(advTestTarget) {
    // ADVERT_R-I-486953-3819_1612717448591 -> R-I-486953-3819
    const convertToIds = items => items.map(adv => {
        // remove ADVERT_
        let advert = adv.substr(7);

        // remove _1612717448591
        return advert.substr(0, advert.indexOf('_'));
    });

    const result = await this.execute(function() {
        return window.completelyLoadResources || [];
    });

    const advSuccess = [...new Set((result.value && convertToIds(result.value)) || [])]; // unique ids

    assert.sameMembers(advSuccess, advTestTarget, `Фактические id рекламных блоков (${advSuccess}), отличаются от ожидаемых (${advTestTarget})`);
};
