import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import ProblemTests from 'client/components/problem-tests/problem-tests';
import { DispatchProps, OwnProps, StateProps } from 'client/components/problem-tests/types';
import {
    selectFetchStarted,
    selectPermission,
    selectSettings,
} from 'client/selectors/problem-settings';
import { getSettings } from 'client/store/problem-settings/actions';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, ownProps: OwnProps) => ({
    fetchStarted: selectFetchStarted(state),
    settings: selectSettings(state, ownProps.problemId),
    permission: selectPermission(state, ownProps.problemId),
});

const mapDispatchToProps = (dispatch: Dispatch, { problemId }: OwnProps): DispatchProps =>
    bindActionCreators(
        {
            getSettings: () => getSettings.request({ problemId }),
        },
        dispatch,
    );

export default connect<StateProps, DispatchProps, OwnProps, RootState>(
    mapStateToProps,
    mapDispatchToProps,
)(ProblemTests);
