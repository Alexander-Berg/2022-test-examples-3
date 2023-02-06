import * as React from 'react';
import { Link } from '../../Link';
import { TimerTestInfoParent } from './TestInfo';
import { cnTimerClock } from '../TimerClock';
import { Text } from '../../Text';
import { logReferenceMetrics } from '../../../metrics/reference';

export class TestInfo extends TimerTestInfoParent {
    renderBottom = () => {
        const { timePassed, shouldOpenSB } = this.props;

        const elem = shouldOpenSB ?
            (
                <Link theme="normal" onClick={this.onClick}>
                    <Text size="s" block>
                        Справочные материалы
                    </Text>
                </Link>
            ) : `Времени прошло: ${timePassed}`;

        return (
            <Text size="s" className={cnTimerClock('Reference')} block>
                {elem}
            </Text>
        );
    }

    renderTasksDone = () => null;

    onClick = () => {
        const { openSideBlock, variantId, subjectId } = this.props;

        logReferenceMetrics({ variantId, subjectId });

        return openSideBlock(window.pageYOffset);
    }
}
