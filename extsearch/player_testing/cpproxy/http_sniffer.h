#pragma once
#include "log_service.h"
#include "stat_counter.h"
#include <util/generic/utility.h>
#include <util/generic/ptr.h>
#include <util/stream/mem.h>
#include <util/string/cast.h>
#include <util/string/vector.h>
#include <library/cpp/http/io/stream.h>
#include <library/cpp/logger/log.h>
#include <yweb/video/vparsrobot/common/is_video_file.h>

namespace {
    static const ui64 TS_PACKET_SIZE = 188;
    static const char TS_PACKET_MAGIC = 0x47;
    static const ui64 VIDEO_DETECT_N_BYTES = TS_PACKET_SIZE * 3;

    bool IsMpegTS(const TString& url, TStringBuf buf) {
        Y_ENSURE(buf.size() >= VIDEO_DETECT_N_BYTES);
        for (ui64 i = 0; i < buf.size() / TS_PACKET_SIZE; ++i) {
            if (buf[i * TS_PACKET_SIZE] != TS_PACKET_MAGIC) {
                return false;
            }
        }
        if (url.find(".ts") == TString::npos && url.find("hls") == TString::npos) {
            return false;
        }
        return true;
    }
}

class THttpSniffer {
    public:
        THttpSniffer(TLog& log,
                     TStatCounter* statCounter,
                     ui16 logServicePort,
                     const TString& host,
                     const TString& path = "/",
                     const TString& method = "GET")
            : Log(log)
            , StatCounter(statCounter)
            , Pos(0)
            , State(ST_REQUEST)
            , Host(host)
            , NotHttp(false)
            {
                if (logServicePort) {
                    LogService.Reset(new TLogService(logServicePort));
                }
                LogItem.Path = path;
                LogItem.Method = method;
            }

        void OnRequest(const void* data, size_t size) {
            StatCounter->AdjustBytesSent(size);
            if (!LogService || NotHttp) {
                return;
            }
            if (State > ST_REQUEST_PARSED) {
                Reset(ST_REQUEST);
            }
            if (State == ST_REQUEST) {
                if (AdjustBuf(data, size)) {
                    try {
                        TMemoryInput memInput(Buf);
                        THttpInput httpInput(&memInput);
                        TParsedHttpRequest parsedRequest(httpInput.FirstLine());
                        LogItem.Method = ToString(parsedRequest.Method);
                        LogItem.Path = ToString(parsedRequest.Request);
                        for(const auto& header: httpInput.Headers()) {
                            auto lcName = to_lower(header.Name());
                            if (lcName == "user-agent") {
                                LogItem.UserAgent = header.Value();
                                break;
                            }
                        }
                    } catch(const std::exception& ex) {
                        Log << TLOG_ERR << "HttpSniffer: fatal: " << ex.what();
                    }
                    Reset(ST_REQUEST_PARSED);
                }
            }
        }

        void OnReply(const void* data, size_t size) {
            StatCounter->AdjustBytesReceived(size);
            if (!LogService || NotHttp) {
                return;
            }
            if (State < ST_REPLY) {
                Reset(ST_REPLY);
            }
            if (State == ST_REPLY && AdjustBuf(data, size)) {
                ParseResponse();
                if (LogItem.ContentLength > VIDEO_DETECT_N_BYTES && SkipHeaders()) {
                    State = ST_REPLY_HDR_PARSED;
                    size = 0;
                } else {
                    LogRequest();
                    Reset(ST_REPLY_BODY_PARSED);
                }
            }
            if (State == ST_REPLY_HDR_PARSED) {
                AdjustBuf(data, Min(size, VIDEO_DETECT_N_BYTES));
                if (DetectVideoFormat()) {
                    LogRequest();
                    Reset(ST_REPLY_BODY_PARSED);
                }
            }
        }

    private:
        enum EState {
            ST_REQUEST,
            ST_REQUEST_PARSED,
            ST_REPLY,
            ST_REPLY_HDR_PARSED,
            ST_REPLY_BODY_PARSED
        };

    private:
        TLog& Log;
        TStatCounter* StatCounter;
        char Buf[65535];
        size_t Pos;
        EState State;
        TString Host;
        TLogService::THttpRequest LogItem;
        THolder<TLogService> LogService;
        bool NotHttp;


    private:
        bool AdjustBuf(const void* data, size_t size) {
            size_t roomSize = Min(size, sizeof(Buf) - Pos - 1);
            if (!roomSize) {
                return false;
            }
            memcpy(Buf + Pos, data, roomSize);
            Pos += roomSize;
            Buf[Pos] = '\0';
            return strstr(Buf, "\r\n\r\n") != nullptr;
        }

        bool SkipHeaders() {
            const char* end = strstr(Buf, "\r\n\r\n");
            if (end != nullptr) {
                size_t bytes = end + 4 - Buf;
                if (bytes > Pos) {
                    return false;
                }
                memmove(Buf, end + 4, bytes);
                Pos -= bytes;
                return true;
            } else {
                return false;
            }
        }

        void ParseResponse() {
            TMemoryInput memInput(Buf);
            THttpInput httpInput(&memInput);
            auto response = SplitString(httpInput.FirstLine(), " ");
            if (response.size() < 2) {
                ythrow yexception() << "invalid HTTP server response: "<< httpInput.FirstLine();
            }
            LogItem.HttpCode = FromString<ui16>(response[1]);
            const auto& headers = httpInput.Headers();
            TString newProto;
            for (auto it = headers.begin(); it != headers.end(); ++it) {
                auto lcName = to_lower(it->Name());
                if (lcName == "content-type") {
                    LogItem.ContentType = it->Value();
                } else if (lcName == "content-length") {
                    LogItem.ContentLength = FromString<ui32>(it->Value());
                } else if (lcName == "upgrade") {
                    newProto = it->Value();
                }
            }
            if (LogItem.HttpCode == HTTP_SWITCHING_PROTOCOLS) {
                Log << "protocol switched to " << newProto << ", stopping our work";
                NotHttp = true;
            }
        }

        void Reset(EState state) {
            Pos = 0;
            State = state;
            if (state == ST_REQUEST) {
                LogItem = TLogService::THttpRequest();
            }
        }

        void LogRequest() {
            if (LogItem.Method.empty()) {
                return;
            }
            LogItem.Host = Host;
            try {
                LogService->LogRequest(LogItem);
            } catch(const std::exception& ex) {
                Log << "HttpSniffer: log service error: " << ex.what();
            }
            Log << LogItem.Method << " "
                << LogItem.Host << LogItem.Path << " "
                << LogItem.HttpCode << " "
                << LogItem.ContentType << " "
                << LogItem.ContentLength;
            LogItem = TLogService::THttpRequest();
        }

        bool DetectVideoFormat() {
            if (Pos < VIDEO_DETECT_N_BYTES) {
                return false;
            }
            TStringBuf buf(Buf, Pos);
            if (NVideo::IsHls(buf) || IsMpegTS(LogItem.Path, buf)) {
                LogItem.VideoFormat = NSnail::EVideoFormat::EVF_HLS;
            } else if (NVideo::IsDash(buf)) {
                LogItem.VideoFormat = NSnail::EVideoFormat::EVF_DASH;
            } else if (NVideo::IsMpeg(buf)) {
                LogItem.VideoFormat = NSnail::EVideoFormat::EVF_MP4;
            } else if (NVideo::IsFlv(buf)) {
                LogItem.VideoFormat = NSnail::EVideoFormat::EVF_FLV;
            } else {
                LogItem.VideoFormat = NSnail::EVideoFormat::EVF_UNKNOWN;
            }
            return true;
        }
};

