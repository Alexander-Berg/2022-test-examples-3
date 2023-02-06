package ru.yandex.market.gutgin.tms.mocks;

import ru.yandex.market.cleanweb.client.CWDocumentRequest;
import ru.yandex.market.cleanweb.client.CWDocumentResponse;
import ru.yandex.market.cleanweb.client.CWRawResult;
import ru.yandex.market.cleanweb.client.CWRawVerdict;
import ru.yandex.market.cleanweb.client.CheckType;
import ru.yandex.market.cleanweb.client.CleanWebService;
import ru.yandex.market.cleanweb.client.ImageVerdict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CleanWebServiceMock extends CleanWebService {

    public Set<Long> notOkIds = Collections.emptySet();
    private Set<Long> needAsyncIds = Collections.emptySet();
    private final CWRawResultGenerator RawResultGenerator;

    public CleanWebServiceMock() {
        super("url", null);
        RawResultGenerator = new CWRawResultGenerator(false);
    }

    @Override
    public List<CWDocumentResponse> check(List<CWDocumentRequest> request) {
        List<CWDocumentResponse> responses = new ArrayList<>();
        for (CWDocumentRequest documentRequest : request) {
            CWDocumentResponse documentResponse = new CWDocumentResponse(documentRequest.getId());
            long ticketId = extractTicketId(documentRequest);
            documentResponse.setResult(RawResultGenerator.getOk(documentRequest));
            if (notOkIds.contains(ticketId)) {
                documentResponse.setResult(RawResultGenerator.getNotOk(documentRequest));
            }
            if (needAsyncIds.contains(ticketId)) {
                documentResponse.setResult(RawResultGenerator.getNeedAsync(documentRequest));
            }
            responses.add(documentResponse);
        }
        return responses;
    }


    private long extractTicketId(CWDocumentRequest request) {
        return Long.parseLong(request.getCwParams().getKey().split("_")[0]);
    }

    public void setNotOkIds(Set<Long> notOkIds) {
        this.notOkIds = notOkIds;
    }

    public void setNeedAsyncIds(Set<Long> needAsyncIds) {
        this.needAsyncIds = needAsyncIds;
    }

    public static class CWRawResultGenerator {

        private static final String SOURCE = "clean-web";
        private static final String SUB_SOURCE = "tmu";

        private final boolean fromYt;

        public CWRawResultGenerator(boolean fromYt) {
            this.fromYt = fromYt;
        }

        public CWRawResult getNotOk(CWDocumentRequest request) {
            List<CWRawVerdict> verdicts = new ArrayList<>();
            verdicts.add(new CWRawVerdict(
                    request.getCwParams().getKey(),
                    "obscene",
                    "true",
                    SOURCE,
                    SUB_SOURCE,
                    request.getCwParams().getType().getCheckName()
            ));

            if (isFashionRequest(request)) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.FASHION_MANNEQUIN_MANNEQUIN.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));
            }
            if (isShoesRequest(request)) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.SHOES_ORIENTATION_OTHER.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));
            }

            if (isImageRequest(request)) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.IMAGE_TOLOKA_WATERMARK.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));
            }

            if (!fromYt) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        "need_async",
                        "false",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));

                if (isFashionRequest(request)) {
                    verdicts.add(new CWRawVerdict(
                            request.getCwParams().getKey(),
                            ImageVerdict.FASHION_QUALITY_CUT.name().toLowerCase(),
                            "true",
                            SOURCE,
                            SUB_SOURCE,
                            request.getCwParams().getType().getCheckName()
                    ));
                }
                if (isShoesRequest(request)) {
                    verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.SHOES_BACKGROUND_OTHER.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                    ));
                }
            }

            return new CWRawResult(verdicts, new HashMap<>(), new HashMap<>());
        }

        public CWRawResult getOk(CWDocumentRequest request) {
            List<CWRawVerdict> verdicts = new ArrayList<>();
            verdicts.add(new CWRawVerdict(
                    request.getCwParams().getKey(),
                    "clean_text",
                    "true",
                    SOURCE,
                    SUB_SOURCE,
                    request.getCwParams().getType().getCheckName()
            ));
            if (isImageRequest(request)) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.WATERMARK_CLEAN.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));
            }

            if (isFashionRequest(request)) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        ImageVerdict.FASHION_BACKGROUND_GOOD.name().toLowerCase(),
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));
            }

            if (isShoesRequest(request)) {
                verdicts.add(new CWRawVerdict(
                    request.getCwParams().getKey(),
                    ImageVerdict.SHOES_BACKGROUND_LIGHT.name().toLowerCase(),
                    "true",
                    SOURCE,
                    SUB_SOURCE,
                    request.getCwParams().getType().getCheckName()
                ));
            }

            if (!fromYt) {
                verdicts.add(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        "need_async",
                        "false",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName()
                ));

                if (isFashionRequest(request)) {
                    verdicts.add(new CWRawVerdict(
                            request.getCwParams().getKey(),
                            ImageVerdict.FASHION_MANNEQUIN_GOOD.name().toLowerCase(),
                            "true",
                            SOURCE,
                            SUB_SOURCE,
                            request.getCwParams().getType().getCheckName()
                    ));
                }
                if (isShoesRequest(request)) {
                    verdicts.add(new CWRawVerdict(
                            request.getCwParams().getKey(),
                            ImageVerdict.SHOES_ORIENTATION_RIGHT.name().toLowerCase(),
                            "true",
                            SOURCE,
                            SUB_SOURCE,
                            request.getCwParams().getType().getCheckName()
                    ));
                }
            }

            return new CWRawResult(verdicts, new HashMap<>(), new HashMap<>());
        }

        private boolean isFashionRequest(CWDocumentRequest request) {
            if (request.getCwParams().getBody().getModerationType() == null) {
                return false;
            }
            return request.getCwParams().getBody().getModerationType().contains("fashion");
        }

        private boolean isShoesRequest(CWDocumentRequest request) {
            if (request.getCwParams().getBody().getModerationType() == null) {
                return false;
            }
            return request.getCwParams().getBody().getModerationType().contains("shoes");
        }

        private boolean isImageRequest(CWDocumentRequest request) {
            return request.getCwParams().getType() == CheckType.IMAGE;
        }

        public CWRawResult getNeedAsync(CWDocumentRequest request) {
            if (fromYt) {
                throw new IllegalArgumentException("Can't be \"need_async\" in yt answer");
            }
            return new CWRawResult(
                    Collections.singletonList(new CWRawVerdict(
                        request.getCwParams().getKey(),
                        "need_async",
                        "true",
                        SOURCE,
                        SUB_SOURCE,
                        request.getCwParams().getType().getCheckName())),
                    new HashMap<>(), new HashMap<>()
            );
        }
    }
}
