import { issueChangelogToGoalLog } from './logMapper';
import * as data from './logMapper.test-data.json';

const {
    inputInARow, inputSpreaded, inputExtra,
    outputInARow, outputSpreaded, outputExtra,
} = data;

describe('Remove goal_status_transfer from the journal', () => {
    // A -> transfer, transfer -> B ==> A -> B
    it('should merge two transfer labels that go one after the other', () => {
        expect(issueChangelogToGoalLog(inputInARow)).toMatchObject(outputInARow);
    });

    // A -> transfer, ..., transfer -> B ==> A -> B
    it('should merge two spreaded transfer labels', () => {
        expect(issueChangelogToGoalLog(inputSpreaded)).toMatchObject(outputSpreaded);
    });

    // A -> transfer, transfer -> A ==> Nothing
    it('should remove two transfer labels, because they are pointless', () => {
        expect(issueChangelogToGoalLog(inputExtra)).toMatchObject(outputExtra);
    });
});
