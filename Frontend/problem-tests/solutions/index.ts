import { connect } from 'react-redux';

import Solutions from 'client/components/problem-tests/solutions/solutions';
import { OwnProps, StateProps } from 'client/components/problem-tests/solutions/types';
import { selectSettings } from 'client/selectors/problem-settings';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, ownProps: OwnProps) => ({
    settings: selectSettings(state, ownProps.problemId),
});

export default connect<StateProps, undefined, OwnProps, RootState>(mapStateToProps)(Solutions);
