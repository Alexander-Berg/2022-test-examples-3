import { connect } from 'react-redux';

import AuthorSolutions from 'client/components/problem-tests/author-solutions/author-solutions';
import { OwnProps } from 'client/components/problem-tests/author-solutions/types';
import { selectCompilersList, selectFetchCompilersError } from 'client/selectors/compilers';
import { selectFetchStarted, selectSubmitStarted } from 'client/selectors/problem-settings';
import { selectSubmitSolutionStarted } from 'client/selectors/problem-solutions';
import { fetchCompilers } from 'client/store/compilers/actions';
import { submitSettings } from 'client/store/problem-settings/actions';
import { clearSubmissionStatus, submitSolution } from 'client/store/problem-solutions/actions';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState, ownProps: OwnProps) => ({
    fetchCompilersError: selectFetchCompilersError(state),
    fetchCompilersStarted: selectFetchStarted(state),
    compilers: selectCompilersList(state),
    submitSolutionStarted: selectSubmitSolutionStarted(state, ownProps.problemId || ''),
    settingsFetchStarted: selectFetchStarted(state),
    settingsSubmitStarted: selectSubmitStarted(state),
});

const mapDispatchToProps = {
    fetchCompilers: fetchCompilers.request,
    submitSolution: submitSolution.request,
    submitSettings: submitSettings.request,
    clearSubmissionStatus,
};

export default connect(mapStateToProps, mapDispatchToProps)(AuthorSolutions);
