const converter = require('../../../../../src/server/data-adapters/nirvana/design');
const _ = require('lodash');
const commonCases = require('../common-cases');
const { CONFIG_INPUT, CONFIG_OUTPUT } = require('./fixtures');

describe('nirvana/design', () => {
    let converterInput, result;

    beforeEach(function() {
        converterInput = _.cloneDeep(CONFIG_INPUT);
        result = converter(converterInput);
    });

    it('правильно конвертирует конфиг в формат макетного графа', () => {
        assert.deepEqual(result, CONFIG_OUTPUT);
    });

    describe('должны правильно формироваться профиль и параметры Толоки:', () => {
        const cases = commonCases.profileAndTolokaParams;

        cases.forEach(({ description, device, iphone, poolTitle, profile, tolokaParams }) => {
            it(description, () => {
                converterInput.config.device = device;
                converterInput.config.iphone = iphone;
                converterInput.config.poolTitle = poolTitle;

                const nirvanaConfig = converter(converterInput);

                assert.equal(nirvanaConfig.where['screen-profile-name'], profile);
                assert.deepEqual(nirvanaConfig.where['custom-toloka-view-params'], tolokaParams);
            });
        });
    });

    describe('должно правильно формироваться поле do-skip-assessment', () => {
        it('при отсутствии значения', () => {
            const result = converter(CONFIG_INPUT);
            assert.strictEqual(result.main['do-skip-assessment'], false);
        });


        it('при значении assessmentGroup === "none" устанавлiивается в true', () => {
            const result = converter({ ...CONFIG_INPUT, config: { ...CONFIG_INPUT.config, assessmentGroup: 'none' } });
            assert.strictEqual(result.main['do-skip-assessment'], true);
        });


        it('при значении assessmentGroup !== "none" устанавливается в false', () => {
            const result = converter({ ...CONFIG_INPUT, config: { ...CONFIG_INPUT.config, assessmentGroup: 'tolokers' } });
            assert.strictEqual(result.main['do-skip-assessment'], false);
        });
    });

    it('если пользователь не разметил ханипоты, но одновременно задал ненулевое кол-во ханипот-заданий, то в итоговом конфиге кол-во ханипот-заданий будет выставлено в нуль', () => {
        converterInput.config.layouts.layouts.forEach((l) => {
            l.honeypots = [];
        });
        converterInput.config.badTasks = 10;

        assert.isTrue(converter(converterInput).exp['honeypot-tasks'] === 0);
    });

    it('правильно формируется поле creation-type', () => {
        const result = converter({ ...CONFIG_INPUT, uiVersion: undefined });
        assert.strictEqual(result.main['creation-type'], 'api');
        assert.strictEqual(result.main['ui-version'], undefined);
    });

    it('автоханипоты должны быть включены, если пользователь их не отключил самостоятельно', () => {
        const result = converter(CONFIG_INPUT);

        assert.isTrue(result.main['use-auto-honeypots']);
    });

    it('автоханипоты должны быть вывключены, если пользователь их выключил', () => {
        const result = converter({ ...CONFIG_INPUT, config: { ...CONFIG_INPUT.config, useAutoHoneypots: 'no' } });

        assert.isFalse(result.main['use-auto-honeypots']);
    });

    describe('должен правильно выбираться пул для экспериментов на коллегах и внутренних асессорах', () => {
        it('для экспериментов на коллегах должен в первую очередь использоваться пул из справочника', () => {
            converterInput.config.assessmentGroup = 'colleagues';
            converterInput.config.poolTitle = 'desktop_colleagues_or_internal-assessors';
            assert.isTrue(converter(converterInput)['pool-clone-info'].template.production['pool-id'] === 107751);
        });

        it('для экспериментов на внутренних асессорах должен в первую очередь использоваться пул из справочника', () => {
            converterInput.config.assessmentGroup = 'internal-assessors';
            converterInput.config.poolTitle = 'desktop_colleagues_or_internal-assessors';
            assert.isTrue(converter(converterInput)['pool-clone-info'].template.production['pool-id'] === 14217383);
        });

        it('для экспериментов на коллегах, если в справочнике пулов не найден подходящий продакшн пул и !throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict, то должен использоваться пул из админки', () => {
            converterInput.config.assessmentGroup = 'colleagues';
            converterInput.throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict = false;
            assert.isTrue(converter(converterInput)['pool-clone-info'].template.production['pool-id'] === 2);
        });

        it('для экспериментов внутренних асессорах, если в справочнике пулов не найден подходящий продакшн пул и !throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict, то должен использоваться пул из админки', () => {
            converterInput.config.assessmentGroup = 'internal-assessors';
            converterInput.throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict = false;
            assert.isTrue(converter(converterInput)['pool-clone-info'].template.production['pool-id'] === 2);
        });

        it('для экспериментов на коллегах, если в справочнике пулов не найден подходящий продакшн пул и throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict, то генератор должен кидать ошибку', () => {
            converterInput.config.assessmentGroup = 'colleagues';
            converterInput.throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict = true;
            assert.throws(() => converter(converterInput), 'Пул для разметки на коллегах или внутренних асессорах не найден. prodPoolId 2, sandboxPoolId 2');
        });

        it('для экспериментов внутренних асессорах, если в справочнике пулов не найден подходящий продакшн пул и throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict, то генератор должен кидать ошибку', () => {
            converterInput.config.assessmentGroup = 'internal-assessors';
            converterInput.throwErrorIfInternalAssessorsOrColleaguesPoolIsNotInDict = true;
            assert.throws(() => converter(converterInput), 'Пул для разметки на коллегах или внутренних асессорах не найден. prodPoolId 2, sandboxPoolId 2');
        });
    });
});
