jest.mock('axios');

import Axios from 'axios';
import sendCallFeedback from '../sendCallFeedback';

describe('sendCallFeedback', () => {
    let feedback;

    beforeEach(() => {
        feedback = {
            callGuid: 'call-guid',
            userGuid: 'user-guid',
            email: 'test-mail@example.com',
        };
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('Not send post request to rtc if no score and no text', () => {
        sendCallFeedback(feedback);

        expect(Axios.post).not.toBeCalled();
    });

    it('Not send post request to rtc if no score and has text', () => {
        feedback.text = 'feedback-text';

        sendCallFeedback(feedback);

        expect(Axios.post).not.toBeCalledWith('rtcFeedbackUrl?undefined', expect.any(Object));
    });

    it('Send post request to rtc url with correct params', () => {
        feedback.score = 5;

        sendCallFeedback(feedback);

        expect(Axios.post).toBeCalledWith('rtcFeedbackUrl?undefined', {
            CallGUID: 'call-guid',
            Score: 5,
            UserGUID: 'user-guid',
        });
    });

    it('Not send request if no text feedback provided and has score', () => {
        feedback.score = 5;

        sendCallFeedback(feedback);

        expect(Axios.post).not.toBeCalledWith('feedbackApiUrl?undefined', expect.any(Object));
    });

    it('Send post request to api url with correct params', () => {
        feedback.text = 'feedback-text';

        sendCallFeedback(feedback);

        expect(Axios.post).toBeCalledWith('feedbackApiUrl?undefined', {
            answer_long_text_21797: 'feedback-text',
            app_version: expect.any(String),
            browser: expect.any(String),
            call_guid: 'call-guid',
            client: 'Web',
            email: 'test-mail@example.com',
            page: expect.any(String),
            referrer: expect.any(String),
            screen: expect.any(String),
            window: expect.any(String),
        });
    });
});
