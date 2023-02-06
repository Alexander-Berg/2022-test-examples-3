import { connect } from 'react-redux';

import { generateTestsetMapKey } from 'common/utils/helpers/problem-testsets';

import ProblemTestset from 'client/components/problem-testsets/problem-testset/problem-testset';
import { OwnProps } from 'client/components/problem-testsets/problem-testset/types';
import {
    selectTestsetFetchTestsError,
    selectTestsetFetchTestsStarted,
    selectTestsetWithValidity,
    selectTestsetUpdateTestsStarted,
} from 'client/selectors/problem-testsets';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, { problemId, testsetId }: OwnProps) => {
    const testsetMapKey = generateTestsetMapKey(problemId, testsetId);

    return {
        data: selectTestsetWithValidity(state, problemId, testsetMapKey),
        fetchTestsStarted: selectTestsetFetchTestsStarted(state, problemId, testsetMapKey),
        fetchTestsError: selectTestsetFetchTestsError(state, problemId, testsetMapKey),
        updateTestsStarted: selectTestsetUpdateTestsStarted(state, problemId, testsetMapKey),
    };
};

export default connect(mapStateToProps)(ProblemTestset);
