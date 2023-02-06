import { TimerTestInfoContainerMaker } from './TimerTestInfoContainer';
import { openSideBlockAndSavePos } from '../../../actions/sideBlock';
import { TestInfo } from '../../../components/TimerClock/TestInfo/TestInfo@touch';

export const TimerTestInfoContainer = TimerTestInfoContainerMaker(TestInfo, openSideBlockAndSavePos);
