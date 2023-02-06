import { connect } from 'react-redux';
import { Dispatch, bindActionCreators } from 'redux';
import {
    VariantFinishButton,
    IVariantFinishButtonProps,
    IVariantFinishButtonActions,
} from '../../components/VariantFinishButton/VariantFinishButton';
import { IRootState } from '../../reducers/index';
import { finishLesson } from '../../actions/lessonPage';
import { toggleFinishModal } from '../../actions/variantTest';

type MapState = Pick<IVariantFinishButtonProps, 'sendingInProgress' | 'allowClientRedirect' >;
type OwnProps = Partial<Pick<IVariantFinishButtonProps, 'sendingInProgress'>>
type MapDispatch = Pick<IVariantFinishButtonActions, 'finishAction' | 'toggleFinishModal'>;

const mapStateToProps = (_: IRootState, own: OwnProps): MapState & OwnProps => {
    return {
        sendingInProgress: Boolean(own.sendingInProgress),
        allowClientRedirect: false,
    };
};

const mapDispToProps = (dispatch: Dispatch): MapDispatch => {
    return bindActionCreators({
        finishAction: finishLesson,
        toggleFinishModal,
    }, dispatch);
};

export const TestFinishButtonContainer = connect(mapStateToProps, mapDispToProps)(VariantFinishButton);
