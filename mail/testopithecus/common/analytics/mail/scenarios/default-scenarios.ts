import { FirstEventTimestampEvaluation } from '../../evaluations/general-evaluations/function/default/first-event-timestamp-evaluation';
import { InitiatorNameEventEvaluation } from '../../evaluations/general-evaluations/function/default/initiator-name-event-evaluation';
import { LastEventTimestampEvaluation } from '../../evaluations/general-evaluations/function/default/last-event-timestamp-evaluation';
import { FirstEventEvaluation } from '../../evaluations/general-evaluations/default/first-event-evaluation';
import { SessionLengthEvaluation } from '../../evaluations/general-evaluations/default/session-length-evaluation';
import { StartScenarioSplitter } from '../../evaluations/scenario-splitting/start-scenario-splitter';
import { ComposeAttachmentsCountEvaluation } from './compose/compose-attachments-count-evaluation';
import { ComposeBodyLengthEvaluation } from './compose/compose-body-length-evaluation';
import { ComposeCompletedEvaluation } from './compose/compose-completed-evaluation';
import { ComposeFinalizerEvaluation } from './compose/compose-finalizer-evaluation';
import { ComposeHasAttachmentsEvaluation } from './compose/compose-has-attachments-evaluation';
import { ComposeMidValueExtractor } from './compose/compose-mid-value-extractor';
import { ComposeQualityTypeEvaluation } from './compose/compose-quality-type-evaluation';
import { ComposeReceiveTimestampValueExtractor } from './compose/compose-receive-timestamp-value-extractor';
import { ComposeScenarioSplitter } from './compose/compose-scenario-splitter';
import { ComposeSimpleEvaluations } from './compose/compose-simple-evaluations';
import { ComposeTypeEvaluation } from './compose/compose-type-evaluation';
import { ComposeSrIndexUsedEvaluation } from './compose/smart_replies/compose-sr-index-used-evaluation';
import { ComposeSrItemsCountEvaluation } from './compose/smart_replies/compose-sr-items-count-evaluation';
import { ComposeSrUsedEvaluation } from './compose/smart_replies/compose-sr-used-evaluation';
import { ComposeSrUsedExactEvaluation } from './compose/smart_replies/compose-sr-used-exact-evaluation';

export class DefaultScenarios {

  public static globalScenario = () =>
    new StartScenarioSplitter([
      () => new FirstEventEvaluation(), // first_event
      () => new SessionLengthEvaluation(), // session_length
      // () => new FullScenarioEvaluation(), // full_scenario
    ]);

  public static composeScenario = () =>
    new ComposeScenarioSplitter([
      () => new FirstEventTimestampEvaluation(), // start_timestamp_ms
      () => new LastEventTimestampEvaluation(), // finish_timestamp_ms
      () => new SessionLengthEvaluation(), // duration_scenario_ms
      () => new ComposeTypeEvaluation(), // scenario_type
      () => new ComposeMidValueExtractor(), // mid
      () => new ComposeReceiveTimestampValueExtractor(), // receive_timestamp
      ComposeSimpleEvaluations.sendingEvaluation, // sending
      () => new ComposeCompletedEvaluation(), // is_full
      () => new InitiatorNameEventEvaluation(), // initiator
      () => new ComposeFinalizerEvaluation(), // finalizer
      () => new ComposeQualityTypeEvaluation(), // qa_type
      () => new ComposeBodyLengthEvaluation(), // body_length
      () => new ComposeAttachmentsCountEvaluation(), // _attachments_count
      () => new ComposeHasAttachmentsEvaluation(), // _has_attachments
      ComposeSimpleEvaluations.bodyEditedEvaluation, // body_edited

      () => new ComposeSrUsedEvaluation(), // sr_used
      () => new ComposeSrIndexUsedEvaluation(), // sr_index_used
      () => new ComposeSrItemsCountEvaluation(), // sr_items_count
      () => new ComposeSrUsedExactEvaluation(), // sr_used_exact
      // () => new FullScenarioEvaluation(), // full_scenario
    ]);

}
