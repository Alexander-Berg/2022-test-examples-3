import { verdictDTOToSearchResultVerdict, userPendingVerdict, getFinalVerdict, makeVerdictKey } from '../objectDTOtoModerationObject';

import objectDTO from '../../../../jest/__fixtures__/objectDTO.json';

let obj;

beforeEach(() => {
    obj = objectDTO;
});

describe('hacks/backend/objectDTOtoModerationObject/userPendingVerdict', () => {
    it('finds correct user verdict', () => {
        expect(userPendingVerdict(obj.verdicts, obj.verdicts[obj.verdicts.length - 1])).toBeUndefined();
    });
});

describe('hacks/backend/objectDTOtoModerationObject/getFinalVerdict', () => {
    it('finds vtotal verdict and returns it', () => {
        expect(getFinalVerdict(obj.verdicts)).toEqual({
            circuit: 'VTOTAL',
            create_time: '2021-07-27 11:38:31',
            verdict_time: '2021-07-27 11:38:31',
            flags: {
                acids: '1',
                annoying: '1',
                region_ru: '1',
            },
            minus_regions: [
                '149',
                '159',
                '225',
                '977',
            ],
            dyn_disclaimer: '',
            catalogia_ids: [
                '200004535',
                '200004157',
                '200063792',
                '200064104',
            ],
            source: 7354679859,
            source_id: 23673709888,
            forced: true,
            copy_time: 1627375098,
            abuse: false,
            was_seen: false,
            version_id: 24071706627,
            control_remoderation_flag: false,
            moderator: 'mamedovaleyla',
            redirect: '',
            moderator_login: 'mamedovaleyla',
            mod_update_time: '2021-07-27 11:38:18',
            statusPostModerate: 'Auto',
            reasons: [
                129,
                29,
                69,
            ],
            verdict: 'No',
        });
    });
});

describe('hacks/backend/objectDTOtoModerationObject/makeVerdictKey', () => {
    it('returns correct key', () => {
        const verdict = obj.verdicts[0];

        expect(makeVerdictKey(verdict)).toBe('moderation/9/Yes/2019-04-11 14:28:22/');
    });
});

describe('hacks/backend/objectDTOtoModerationObject/verdictDTOToSearchResultVerdict', () => {
    it('changes antispam circuits to safety', () => {
        const verdict = obj.verdicts[0];
        verdict.circuit = 'antispam';
        expect(verdictDTOToSearchResultVerdict(verdict).circuit).toBe('safety');
    });

    it('adds moderator field if circuit is automatic', () => {
        const verdict = obj.verdicts[0];
        verdict.circuit = 'antispam';
        expect(verdictDTOToSearchResultVerdict(verdict).moderator).toBe('🤖 safety');
    });
});
