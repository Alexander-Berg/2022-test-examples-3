import { bindActionCreators, Dispatch } from 'redux';
import { connect } from 'react-redux';

import { ApplicationState } from '../../store';

import { TestsAction } from '../../store/tests/types';

import Dictionary from '../../../common/types/dictionary';

import {
    showTooltip,
    hideTooltip
} from '../../store/tests/actions';

import IExam from '../../../common/types/exam';

import Tests from './tests';

interface IStateProps {
    visibleLockName: string | null
}

export interface ILockedExam {
    lockDate: Date,
    login: string
}

interface IOwnProps {
    exams: IExam[],
    api: string,
    lockedExams: Dictionary<ILockedExam>
}

type TestsProps = IStateProps & IOwnProps;

interface IDispatchProps {
    showTooltip(lockName: string): void,
    hideTooltip(): void
}

function mapStateToProps(state: ApplicationState, ownProps: IOwnProps): TestsProps {
    const {
        visibleLockName
    } = state.tests;
    const { exams, api, lockedExams } = ownProps;

    return {
        exams,
        api,
        lockedExams,
        visibleLockName
    };
}

function mapDispatchToProps(dispatch: Dispatch<TestsAction>): IDispatchProps {
    return bindActionCreators({
        showTooltip,
        hideTooltip
    }, dispatch);
}

export default connect<IStateProps, IDispatchProps, IOwnProps>(
    mapStateToProps,
    mapDispatchToProps
)(Tests);
