import { connect } from 'react-redux';
import { default as idx } from 'idx';
import { Dispatch, bindActionCreators } from 'redux';
import { IRootState } from '../../../reducers/index';
import { getKeysCount, timeToString } from '../../../utils/helpers';
import { ITimerTestInfoProps } from '../../../components/TimerClock/TestInfo/TestInfo';

type Fields = 'valuesCount' | 'wholeValuesCount' | 'timePassed' | 'variantId' | 'subjectId' | 'shouldOpenSB';
type MapState = Pick<ITimerTestInfoProps, Fields>;
type MapDisp = Pick<ITimerTestInfoProps, 'openSideBlock'>;

export type TTimerTestInfoContainerProps = Omit<ITimerTestInfoProps, Fields | 'openSideBlock'>;

export const TimerTestInfoContainerMaker = (
    component: React.ComponentType<ITimerTestInfoProps>,
    openSideBlock: MapDisp['openSideBlock'],
) => {
    const mapStateToProps = (state: IRootState): MapState => {
        return {
            timePassed: timeToString(state.timer.time),
            valuesCount: getKeysCount(idx(state, _ => _.form.variant.values) || {}),
            wholeValuesCount: state.variantTest.tasks ? state.variantTest.tasks.length : 0,
            shouldOpenSB: Boolean(state.sideBlock.cardText),

            variantId: String(state.variantTest.id),
            subjectId: state.variantTest.subjectId,
        };
    };

    const mapDispToProps = (disp: Dispatch): MapDisp => {
        return bindActionCreators({
            openSideBlock,
        }, disp);
    };

    return connect(mapStateToProps, mapDispToProps)(component);
};
