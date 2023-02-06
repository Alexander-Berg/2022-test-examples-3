import { connect } from 'react-redux';

import Submissions from 'client/components/problem-tests/submissions/submissions';
import {
    DispatchProps,
    OwnProps,
    StateProps,
} from 'client/components/problem-tests/submissions/types';
import {
    selectGetSubmissionStatusStarted,
    selectLastSubmissionStatus,
    selectRejudgeSubmissionStarted,
} from 'client/selectors/problem-solutions';
import { clearSubmissionStatus, rejudgeSubmission } from 'client/store/problem-solutions/actions';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, ownProps: OwnProps) => ({
    rejudgeSubmissionStarted: selectRejudgeSubmissionStarted(state, ownProps.problemId),
    lastSubmissionStatus: selectLastSubmissionStatus(state, ownProps.problemId),
    getSubmissionStatusStarted: selectGetSubmissionStatusStarted(state, ownProps.problemId),
});

const mapDispatchToProps = {
    rejudgeSubmission: rejudgeSubmission.request,
    clearSubmissionStatus,
};

export default connect<StateProps, DispatchProps, OwnProps, RootState>(
    mapStateToProps,
    mapDispatchToProps,
)(Submissions);
