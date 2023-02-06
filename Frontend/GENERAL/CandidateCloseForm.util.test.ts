import { IChoice } from '~/interfaces/ISField';

import { getConsiderationResolutionChoices } from './CandidateCloseForm.util';

describe('CandidateCloseForm.util', function() {
    describe('getConsiderationResolutionChoices', function() {
        it('should sort and group consideration resolutions', function() {
            const source: IChoice[] = [
                {
                    key: '-',
                    value: '',
                },
                {
                    key: 'refused_one',
                    value: 'refused_one',
                },
                {
                    key: 'rejected_one',
                    value: 'rejected_one',
                },
                {
                    key: 'foo',
                    value: 'foo',
                },
                {
                    key: 'refused_two',
                    value: 'refused_two',
                },
                {
                    key: 'rejected_two',
                    value: 'rejected_two',
                },
                {
                    key: 'other',
                    value: 'other',
                },
            ];

            const result = [
                {
                    title: 'rejected',
                    items: ['rejected_one', 'rejected_two'],
                },
                {
                    title: 'refused',
                    items: ['refused_one', 'refused_two'],
                },
                {
                    title: 'other',
                    items: ['foo', 'other'],
                },
            ];

            expect(getConsiderationResolutionChoices(source)).toEqual(result);
        });
    });
});
