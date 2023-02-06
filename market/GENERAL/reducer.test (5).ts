import { AnyAction } from 'typescript-fsa';

import { SEQUENCES_IDS } from 'src/tasks/common-logs/store/ui/logger/constants';
import { loggerActions } from './actions';
import { loggerReducer } from './reducer';

const EMPTY_ACTION = {} as AnyAction;

let requestIndex = 0;
const getRequest = () => {
  requestIndex++;

  return {
    duration_ms: requestIndex,
    req_id: `req_id_${requestIndex}`,
    url: `url_${requestIndex}`,
  };
};

describe('logger reducer', () => {
  it('should return the initial state', () => {
    expect(loggerReducer(undefined, EMPTY_ACTION)).toEqual({ sequences: {} });
  });
  it('should handle initializeSequence', () => {
    let state = loggerReducer(undefined, loggerActions.initializeSequence(SEQUENCES_IDS.INIT));
    expect(state.sequences[SEQUENCES_IDS.INIT]).toBeDefined();
    expect(state.sequences[SEQUENCES_IDS.INIT].start).toBeDefined();
    state = loggerReducer(state, loggerActions.initializeSequence(SEQUENCES_IDS.SWITCH_OFFER));
    expect(state.sequences[SEQUENCES_IDS.INIT]).toBeDefined();
    expect(state.sequences[SEQUENCES_IDS.INIT].start).toBeDefined();
    expect(state.sequences[SEQUENCES_IDS.SWITCH_OFFER]).toBeDefined();
    expect(state.sequences[SEQUENCES_IDS.SWITCH_OFFER].start).toBeDefined();
    expect(state.sequences[SEQUENCES_IDS.SUBMIT]).toBeUndefined();
  });
  it('should handle logRequest', () => {
    let state = loggerReducer(undefined, loggerActions.initializeSequence(SEQUENCES_IDS.INIT));
    const request = getRequest();
    state = loggerReducer(state, loggerActions.logRequest(request));

    expect(state.sequences[SEQUENCES_IDS.INIT].requests).toEqual([request]);
  });
  it('should handle removeSequence', () => {
    let state = loggerReducer(undefined, loggerActions.initializeSequence(SEQUENCES_IDS.INIT));
    const request = getRequest();
    state = loggerReducer(state, loggerActions.logRequest(request));
    state = loggerReducer(state, loggerActions.removeSequence(SEQUENCES_IDS.INIT));
    expect(state.sequences[SEQUENCES_IDS.INIT]).toBeUndefined();
  });
});
