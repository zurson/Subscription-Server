package org.example.api.utilities;

import org.example.api.utilities.payload.FeedbackPayload;

public interface IResponseHandler {

    void handleServerResponse(FeedbackPayload feedbackPayload);

}
