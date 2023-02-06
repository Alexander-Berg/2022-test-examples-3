#include <extsearch/audio/generative/cpp/backend/library/yqlhttpclient/yqlhttpclient.h>

#include <library/cpp/json/json_writer.h>

#include <util/system/env.h>
#include <util/generic/yexception.h>

int main() {
    try{
        auto clnt = NGenerative::IYqlHttpClient::CreateDefaultImplementation("https://yql.yandex.net", GetEnv("YQL_TOKEN"), nullptr); // WILL CRASH
        auto res = clnt->RunQuery("SELECT error, request FROM hahn.range('logs/music-generative-backend-log/stream/5min/','2022-06-21T11:30:00', '2022-06-21T11:40:00') where session_id == '179f11a34e64a6e8' LIMIT 100;");
        if(res.second) {
            auto status = clnt->WaitCompletion(res.second);
            if(status == NGenerative::IYqlHttpClient::COMPLETED) {
                auto json = clnt->GetResult(res.second);
                if(json) {
                    Cout << NJson::WriteJson(*json) << Endl;
                } else {
                    Cout << "GetResult faled\n";
                }
            } else {
                Cout << "Status is " << ToString(status) << Endl;
            }
        } else {
            Cout << "RunQuery returned " << ToString(res.first) << Endl;
        }
    }catch(...) {
        Cerr << CurrentExceptionMessage() << Endl;
    }
}
