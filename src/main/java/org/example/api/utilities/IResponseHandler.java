package org.example.api.utilities;

import org.example.api.utilities.payload.FeedbackPayload;

public interface IResponseHandler {

    void addNewResponse(FeedbackPayload feedbackPayload);

    void notifySubscription(Message message);

}
