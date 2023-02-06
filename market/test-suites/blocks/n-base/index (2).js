import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import LinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import OpenGraphDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-description';
import OpenGraphImageSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-image';
import OpenGraphUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-url';

/**
 * @typedef Suite
 * @type {object}
 * @property {string} path - путь до сюита
 * @property {string} paramPath - поле в объекте params, которое регулирует пропускается этот тест или нет
 * @property {string} [isSkippedByDefault=false] - дефолтная опция пропускается этот тест или нет,
 * если параметр не передан
 */

/**
 * Хелпер для упрощенного импорта сюитов, которые возможно будет пропускать при помощи хука beforeEach
 *
 * @param {Array.<Suite>} importSuits
 */
function importProvider(importSuits) {
    return importSuits.map(data =>
        prepareSuite(data.suite, {
            hooks: {
                beforeEach() {
                    let skipCondition;
                    if (this.params && Object.prototype.hasOwnProperty.call(this.params, data.paramPath)) {
                        skipCondition = this.params[data.paramPath];
                    } else {
                        skipCondition = data.isSkippedByDefault || false;
                    }

                    if (skipCondition === true) {
                        // eslint-disable-next-line market/ginny/no-skip
                        this.skip('Тег не поддерживается страницей');
                    }
                },
            },
        })
    );
}

/**
 * Тесты на мета-разметку страницы: тайтл, Open Graph, meta-теги.
 * можно параметризовать включение/выключение проверок.
 * Дефолтное состояние определяет сюит, рекомендуется выключенное состояние
 *
 * Доступные параметры:
 *     skipTitle: false,
 *     skipLinkCanonical: false,
 *     skipDescription: false,
 *     skipOpenGraphDescription: false,
 *     skipOpenGraphImage: false,
 *     skipOpenGraphUrl: false,
 *
 * @Example:
 * prepareSuite(BaseSuite, {
 *     params: {
 *         skipLinkCanonical: true,
 *         ...
 *     }
 * })
 *
 * Руководство по добавлению сюитов, необходимо:
 * 1. Написать сюит
 * 2. Добавить его в список импорта для importHelper`a
 */
const suite = makeSuite('Мета-разметка.', {
    story: mergeSuites(
        ...importProvider([{
            suite: LinkCanonicalSuite,
            paramPath: 'skipLinkCanonical',
        }, {
            suite: OpenGraphDescriptionSuite,
            paramPath: 'skipOpenGraphDescription',
        }, {
            suite: OpenGraphImageSuite,
            paramPath: 'skipOpenGraphImage',
        }, {
            suite: OpenGraphUrlSuite,
            paramPath: 'skipOpenGraphUrl',
        }])
    ),
});

export default suite;
