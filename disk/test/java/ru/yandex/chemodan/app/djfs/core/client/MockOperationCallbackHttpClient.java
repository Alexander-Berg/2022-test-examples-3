package ru.yandex.chemodan.app.djfs.core.client;

import java.net.URI;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.io.http.HttpException;


@Getter
public class MockOperationCallbackHttpClient extends OperationCallbackHttpClient {

    private ListF<CallParams> callParams;
    private boolean shouldRaiseHttpError;

    MockOperationCallbackHttpClient() {
        super(null);
        callParams = Cf.arrayList();
        shouldRaiseHttpError = false;
    }

    public void clearCallParams() {
        callParams.clear();
        shouldRaiseHttpError = false;
    }

    public void setFailRequest() {
        shouldRaiseHttpError = true;
    }

    @Override
    public void doRequest(URI callbackUri, OperationCallbackData data) {
        if (shouldRaiseHttpError) {
            throw new HttpException();
        }
        callParams.add(new CallParams(callbackUri, data));
    }

    @Getter
    @RequiredArgsConstructor
    public class CallParams {
        public final URI callbackUri;
        public final OperationCallbackData data;
    }
}
