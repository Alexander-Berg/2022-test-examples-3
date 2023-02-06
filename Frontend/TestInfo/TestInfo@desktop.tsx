import * as React from 'react';
import { TimerTestInfoParent } from './TestInfo';
import { cnTimerClock } from '../TimerClock';
import { Text } from '../../Text';
import { Link } from '../../Link';
import { logReferenceMetrics } from '../../../metrics/reference';

export class TestInfo extends TimerTestInfoParent {
    renderTasksDone = () => {
        const { valuesCount, wholeValuesCount } = this.props;

        return (
            <Text className={cnTimerClock('SolvedCounter')}>
                Выполнено заданий: {String(valuesCount)} из {wholeValuesCount}
            </Text>
        );
    }

    renderBottom = () => {
        const { timePassed, shouldOpenSB, view } = this.props;
        const isVideoLesson = view === 'videoLesson';

        const elem = shouldOpenSB && isVideoLesson ?
            (
                <Link theme="normal" onClick={this.onClick}>
                    <Text block>
                        Справочные материалы
                    </Text>
                </Link>
            ) : `Времени прошло: ${timePassed}`;

        return (
            <Text className={cnTimerClock('Description')} block>
                {elem}
            </Text>
        );
    }

    onClick = () => {
        const { openSideBlock, variantId, subjectId } = this.props;

        logReferenceMetrics({ variantId, subjectId });

        return openSideBlock(window.pageYOffset);
    }
}
