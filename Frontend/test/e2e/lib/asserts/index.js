const _ = require('lodash');
const { ErrorMessage } = require('./message');

/**
 * Проверить корректность данных аналитики в ответе
 *
 * @param {Object} client
 * @param {Object} data
 * @param {Object} expected
 * @param {String} expected.intent
 * @param {String} expected.product_scenario_name
 * @param {Object} params
 * @param {Boolean} params.equal
 */
const yaCheckAnalyticsInfo = (client, data, expected, params = { equal: true }) => {
    let analyticsInfo = _.get(data, 'rawEvent.directive.payload.megamind_analytics_info.analytics_info');

    analyticsInfo = analyticsInfo && (
        analyticsInfo.Wizard ||
        analyticsInfo.FindPoi ||
        analyticsInfo.Vins ||
        analyticsInfo.Covid19 ||
        analyticsInfo.Search
    );

    analyticsInfo = analyticsInfo && analyticsInfo.scenario_analytics_info;

    assert.exists(
        analyticsInfo,
        ErrorMessage.create('Отсутствуют данные для аналитики', client, data),
    );

    analyticsInfo = {
        intent: analyticsInfo.intent || null,
        product_scenario_name: analyticsInfo.product_scenario_name || null,
    };

    // Проверяем что данные аналитики не сопдают, но это валидно
    if (!params.equal) {
        return assert.notDeepEqual(
            analyticsInfo,
            expected,
            ErrorMessage.create('Некорректные данные аналитики', client, data),
        );
    }

    return assert.deepEqual(
        analyticsInfo,
        expected,
        ErrorMessage.create('Некорректные данные аналитики', client, data),
    );
};

/**
 * Проверить корректность голосового ответа
 *
 * @param {Object} client
 * @param {Object} data
 * @param {String} voice
 */
const yaCheckVoiceInclude = (client, data, voice) => {
    assert.include(
        data.voice,
        voice,
        ErrorMessage.create('Голосовой ответ не содержит заданную фразу:', client, data),
    );
};

/**
 * Проверить наличие фрагмента голосового ответа в единичном экземпляре
 *
 * @param {Object} client
 * @param {Object} data
 * @param {String} voice
 */
const yaCheckVoiceIncludeOnce = (client, data, voice) => {
    const regExp = new RegExp(voice, 'gi');
    const matches = data.voice.match(regExp) || [];

    assert.equal(matches.length, 1, ErrorMessage.create('Найдено более или менее одного вхождения:', client, data));
};

/**
 * Проверить наличие фрагмента текстового ответа в единичном экземпляре
 *
 * @param {Object} client
 * @param {Object} data
 * @param {String} text
 */
const yaCheckTextIncludeOnce = (client, data, text) => {
    const regExp = new RegExp(text, 'gi');
    const matches = (data.text || data.card).match(regExp) || [];

    assert.equal(matches.length, 1, ErrorMessage.create('Найдено более или менее одного вхождения:', client, data));
};

const yaCheckVoiceMatch = (client, data, voiceMatch) => {
    assert.match(
        data.voice,
        voiceMatch,
        ErrorMessage.create('Голосовой ответ не соответствует регулярному выражению:', client, data),
    );
};

/**
 * Проверить корректность текстового ответа
 *
 * @param {Object} client
 * @param {Object} data
 * @param {String} text
 */
const yaCheckTextInclude = (client, data, text) => {
    assert.include(
        data.text || data.card,
        text,
        ErrorMessage.create('Текстовый ответ не содержит заданный текст', client, data),
    );
};

/**
 * Проверить отсутствие текстового ответа
 *
 * @param {Object} client
 * @param {Object} data
 * @param {String} text
 */
const yaCheckTextNoInclude = (client, data, text) => {
    assert.notInclude(
        data.text || data.card,
        text,
        ErrorMessage.create('Текстовый ответ содержит заданный текст', client, data),
    );
};

const yaCheckTextMatch = (client, data, voiceMatch) => {
    assert.match(
        data.text || data.card,
        voiceMatch,
        new ErrorMessage('Текстовый ответ не соответствует регулярному выражению', client, data),
    );
};

const yaCheckCard = (client, data, expected) => {
    const cards = data.rawEvent.directive.payload.response.cards.filter(card => {
        const cardType = card.type;

        return cardType === 'div_card' || cardType === 'div2_card';
    });
    const isSomeCardMatched = cards.some(card => _.isMatch(card, expected));

    if (!isSomeCardMatched) {
        throw new Error(ErrorMessage.create(`Дивные карточки не содержат данных ${JSON.stringify(expected)}`, client, data));
    }
};

module.exports.getAsserts = client => {
    return {
        yaCheckVoiceIncludeOnce: yaCheckVoiceIncludeOnce.bind(this, client),
        yaCheckTextIncludeOnce: yaCheckTextIncludeOnce.bind(this, client),
        yaCheckAnalyticsInfo: yaCheckAnalyticsInfo.bind(this, client),
        yaCheckVoiceInclude: yaCheckVoiceInclude.bind(this, client),
        yaCheckTextInclude: yaCheckTextInclude.bind(this, client),
        yaCheckTextNoInclude: yaCheckTextNoInclude.bind(this, client),
        yaCheckVoiceMatch: yaCheckVoiceMatch.bind(this, client),
        yaCheckTextMatch: yaCheckTextMatch.bind(this, client),
        yaCheckCard: yaCheckCard.bind(this, client),
    };
};
