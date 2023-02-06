#pragma once
#include <extsearch/video/kernel/protobuf/writer.h>
#include <extsearch/video/robot/crawling/player_testing/protos/job.pb.h>
#include <library/cpp/http/simple/http_client.h>

class TLogService {
    public:
        struct THttpRequest {
            TString Method;
            TString Host;
            TString Path;
            TString ContentType;
            TString UserAgent;
            ui64 ContentLength = 0;
            ui16 HttpCode = 0;
            ui8 VideoFormat = 0;
        };

        TLogService(ui16 logServerPort)
            : HttpClient("localhost", logServerPort)
            {}

        void LogRequest(const THttpRequest& rec) {
            NSnail::THttpResource pb;
            pb.SetUrl(rec.Host + rec.Path);
            if (!rec.ContentType.empty()) {
                pb.SetContentType(rec.ContentType);
            }
            if (rec.ContentLength) {
                pb.SetContentSize(rec.ContentLength);
            }
            if (rec.HttpCode) {
                pb.SetHttpCode(rec.HttpCode);
            }
            if (rec.VideoFormat) {
                pb.SetVideoFormat((NSnail::EVideoFormat)rec.VideoFormat);
            }
            HttpClient.DoPost(TString("/http/log"), NVideo::TProtoWriter::ToStringBinary(pb), &Cerr);
        }

    private:
        TSimpleHttpClient HttpClient;
};
