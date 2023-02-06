import { TimerTestInfoContainerMaker } from './TimerTestInfoContainer';
import { openSideBlock } from '../../../actions/sideBlock';
import { TestInfo } from '../../../components/TimerClock/TestInfo/TestInfo@desktop';

export const TimerTestInfoContainer = TimerTestInfoContainerMaker(TestInfo, openSideBlock);
