import { connect } from 'react-redux';

import ProblemTestsets from 'client/components/problem-testsets/problem-testsets';
import { OwnProps } from 'client/components/problem-testsets/types';
import {
    selectAddSampleTestsetError,
    selectAddSampleTestsetStarted,
    selectAddTestsetStarted,
    selectFetchStarted,
    selectTestsets,
} from 'client/selectors/problem-testsets';
import { selectFetchStarted as selectProblemSettingsFetchStarted } from 'client/selectors/problem-settings';
import {
    addSample,
    addTestset,
    fetchTestset,
    fetchTestsets,
    removeTestset,
    updateTestset,
} from 'client/store/problem-testsets/actions';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, { problemId }: OwnProps) => ({
    addSampleStarted: selectAddSampleTestsetStarted(state, problemId),
    addSampleError: selectAddSampleTestsetError(state, problemId),
    addTestsetStarted: selectAddTestsetStarted(state, problemId),
    testsets: selectTestsets(state, problemId),
    fetchStarted: selectFetchStarted(state, problemId) || selectProblemSettingsFetchStarted(state),
});

const mapDispatchToProps = {
    addSample: addSample.request,
    addTestset: addTestset.request,
    fetchTestsets: fetchTestsets.request,
    fetchTestset: fetchTestset.request,
    removeTestset: removeTestset.request,
    updateTestset: updateTestset.request,
};

export default connect(mapStateToProps, mapDispatchToProps)(ProblemTestsets);
