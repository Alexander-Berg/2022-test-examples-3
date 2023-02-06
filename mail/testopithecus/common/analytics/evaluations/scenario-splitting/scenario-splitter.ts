import { TestopithecusEvent } from '../../../code/mail/logging/testopithecus-event';
import { ScenarioAttributes } from '../../scenario';
import { Evaluation } from '../evaluation';

export interface ScenarioSplitter<C> extends Evaluation<ScenarioAttributes[], C> {

  isScenarioStarted(event: TestopithecusEvent): boolean

  isScenarioEnded(event: TestopithecusEvent): boolean

  getEvaluations(): Array<Evaluation<any, C>>

}
