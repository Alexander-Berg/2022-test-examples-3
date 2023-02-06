package ru.yandex.market.tsum.trace;

import ru.yandex.market.tsum.trace.model.RequestId;
import ru.yandex.market.tsum.trace.model.Trace;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.09.16
 */
public class TraceTestData {

    private static long idMs = 1472564785207L;
    private static String hash = "3b97de9bd6edbf0a8173e2fbebfc3d47";
    private static RequestId requestId = new RequestId(idMs, hash, new int[]{});

    private TraceTestData() {
    }

    public static RequestId getRequestId() {
        return requestId;
    }

    public static Trace getTrace() {
        return getTrace(requestId);
    }

    public static Trace getTrace1() {
        return getTrace(cloneRequestId(requestId, new int[]{1}));
    }

    public static Trace getTrace2() {
        return getTrace(cloneRequestId(requestId, new int[]{2}));
    }

    public static Trace getTrace21() {
        return getTrace(cloneRequestId(requestId, new int[]{2, 1}));
    }

    public static Trace getTrace11() {
        return getTrace(cloneRequestId(requestId, new int[]{1, 1}));
    }

    public static Trace getTrace12() {
        return getTrace(cloneRequestId(requestId, new int[]{1, 2}));
    }

    public static RequestId cloneRequestId(RequestId base, int[] seq) {
        return new RequestId(base.getTimestampMillis(), base.getHash(), seq);
    }

    private static Trace getTrace(RequestId requestId) {
        Trace trace = new Trace();
        trace.setIdMs(requestId.getTimestampMillis());
        trace.setIdHash(requestId.getHash());
        trace.setIdSeq(requestId.getSeq());
        return trace;
    }
}
