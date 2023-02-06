import {
    ISSUE_GROUPS_SET_LOADING,
    ISSUE_GROUPS_REQUEST,
    ISSUE_GROUPS_RESET_ERROR,
    COMPLAINTS_SET_LOADING,
    COMPLAINTS_REQUEST,
    COMPLAINTS_RESET_ERROR,
    POST_APPEAL,
    APPEAL_SET_LOADING,
    APPEAL_ERROR_AND_SUCCESS_RESET,
    UPDATE_APPEAL,
    setIssueGroupLoading,
    issueGroupRequest,
    resetIssueGroupsError,
    setComplaintsLoading,
    complaintsRequest,
    resetComplaintsError,
    postAppeal,
    setAppealLoading,
    resetAppealErrorAndSuccess,
    updateAppeal,
} from './Perfection.actions';

describe('Perfection Action', () => {
    it('Should create payload for setting issue groups loading', () => {
        expect(setIssueGroupLoading(true)).toEqual({
            payload: true,
            type: ISSUE_GROUPS_SET_LOADING,
        });
    });

    it('Should create payload for request', () => {
        expect(issueGroupRequest({ serviceId: 5 })).toEqual({
            type: ISSUE_GROUPS_REQUEST,
            payload: { serviceId: 5 },
        });
    });

    it('Should create resetIssueGroupsError type', () => {
        expect(resetIssueGroupsError()).toEqual({
            type: ISSUE_GROUPS_RESET_ERROR,
        });
    });

    it('Should create payload for setting complaints loading', () => {
        expect(setComplaintsLoading(true)).toEqual({
            payload: true,
            type: COMPLAINTS_SET_LOADING,
        });
    });

    it('Should create payload for complaints request', () => {
        expect(complaintsRequest({ serviceId: 5 })).toEqual({
            type: COMPLAINTS_REQUEST,
            payload: { serviceId: 5 },
        });
    });

    it('Should create resetComplaintsError type', () => {
        expect(resetComplaintsError()).toEqual({
            type: COMPLAINTS_RESET_ERROR,
        });
    });

    it('Should create payload for setting post appeal loading', () => {
        expect(setAppealLoading(true)).toEqual({
            payload: true,
            type: APPEAL_SET_LOADING,
        });
    });

    it('Should create resetAppealErrorAndSuccess type', () => {
        expect(resetAppealErrorAndSuccess()).toEqual({
            type: APPEAL_ERROR_AND_SUCCESS_RESET,
        });
    });

    it('Should create payload for postAppeal', () => {
        expect(postAppeal({
            issue: 1001,
            message: 'not a problem',
        })).toEqual({
            type: POST_APPEAL,
            payload: {
                issue: 1001,
                message: 'not a problem',
            },
        });
    });

    it('Should create type for update appeal ', () => {
        expect(updateAppeal(true)).toEqual({
            payload: true,
            type: UPDATE_APPEAL,
        });
    });
});
