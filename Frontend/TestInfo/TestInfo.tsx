import * as React from 'react';
import { cnTimerClock } from '../TimerClock';
import { openSideBlockAndSavePos, openSideBlock } from '../../../actions/sideBlock';

export interface ITimerTestInfoProps {
    timePassed: string;
    valuesCount: number;
    wholeValuesCount: number;
    shouldOpenSB?: boolean;

    openSideBlock: typeof openSideBlock | typeof openSideBlockAndSavePos;

    view: 'videoLesson' | undefined;

    // log-data
    variantId?: string;
    subjectId?: string;
}

export abstract class TimerTestInfoParent extends React.PureComponent<ITimerTestInfoProps> {
    abstract renderBottom: () => React.ReactNode;
    abstract renderTasksDone: () => React.ReactNode;

    render() {
        return (
            <div className={cnTimerClock('TestInfo')}>
                {this.renderTasksDone()}
                {this.renderBottom()}
            </div>
        );
    }
}
