'use strict';

const { iframe } = require('../page-objects');

// BASED ON https://a.yandex-team.ru/arc_vcs/frontend/projects/web4/hermione/commands/commands-templar/common/yaCheckElementA11y.js

/**
 * Проверяет элемент на доступность (accessibility) посредством axe-core
 * Важно: для тачей работает только в iphone, т.к. в chrome-phone не работают executeAsync - FEI-21896
 * Важно: не поддержимвает элементы в iframe
 *
 * Фича в стадии разработки - SERPTEST-2463
 * Как все отладим и проверим на мышах (себе) - анонсируем в этушке.
 *
 * Для удобной отладки откройте страницу в браузере с флагом a11y_validate
 * и вызовите в консоли:
 * `await axe.run(selector)`
 *
 * @param {String} selector - проверяемый элемент
 * @param {Object} params - конфигурация axe.run() - https://github.com/dequelabs/axe-core/blob/HEAD/doc/API.md#parameters-axerun
 *
 * @returns {Promise}
 */

/**
 * Формат конфига разный для запуска через window.axe.run (axe-core)
 * И @axe-core/react в helpers/axe
 * @type {import('axe-core').RunOptions}
 */
const axeConfig = {
    reporter: 'v2',
    runOnly: [
        'wcag2a',
        'wcag21a',
        'best-practice'
    ],
    // в CI-режиме нужно больше стабильности, поэтому игнорируем iframe, которые могут дать шум
    iframes: false,
    rules: (rules => rules.reduce((result, rule) => {
        result[rule.id] = rule;

        return result;
    }, {}))([
        // за контрастность пока не душим
        { id: 'color-contrast', enabled: false },
        // на данный момент используем такой хак, т.к. скринридеры на тачах спотыкаются на тегах <b>, <span>, etc
        { id: 'aria-text', enabled: false },
        // требования к Медиа пока не мониторим (Guideline 1.2 – Time-based Media)
        { id: 'audio-caption', enabled: false },
        // WEATHERFRONT-8063
        { id: 'aria-dialog-name', enabled: false },
        { id: 'meta-viewport', enabled: false },
    ]),
    resultTypes: ['violations']
};

/**
 *
 * @param {String}          [selector]
 * @param {Object}          [params]
 * @param {AxeConfig}       [params.config]
 * @param {Array<String>}   [params.levels]
 * @param {Array<String>}   [params.hide] Набор селекторов элементов, которые нужно скрыть до запуска проверок
 * @returns {Promise<Boolean>}
 */
module.exports = async function yaCheckElementA11y(selector = 'html[data-react-helmet="lang"]', params = {}) {
    const config = {
        ...axeConfig,
        ...(params.config || {})
    };

    // по умолчанию ругаемся только на серьезные и критичные проблемы
    const levels = new Set(params.levels || ['critical', 'serious']);
    // опция iframes конфига не помогает
    const hide = params.hide || [iframe.safebundle];

    await this.ywHideElems(hide);
    await this.timeouts('script', 5000);

    const { value: result } = await this.executeAsync(async function(selector, config, done) {
        if (!('axe' in window)) {
            return done({
                violations: ['No window.axe runtime found. Try open page with ?axe=1 param set.']
            });
        }

        try {
            window.axe.run(selector, config)
                .then(results => {
                    done(results);
                })
                .catch(e => {
                    done({ violations: [`Axe tools is broken. ${e.toString()}`] });
                });
        } catch (e) {
            done({ violations: [`Axe tools is broken. ${e.toString()}`] });
        }
    }, selector, config);

    const violations = result.violations && (
        levels.size ?
            result.violations.filter(violation => !violation.impact || levels.has(violation.impact)) :
            result.violations
    ) || null;

    // axe-core returns "incomplete" where the checking could not be certain, and manual review is needed.
    if (violations && violations.length) {
        throw new Error(`Accessibility issues found: ${violations.length} ...\n` +
            JSON.stringify(violations, null, '  '));
    }

    return true;
};
