#include <extsearch/video/kernel/protobuf/reader.h>
#include <extsearch/video/players/unbanlib/unban.h>
#include <extsearch/video/robot/deletes/util/yastatic_player.h>
#include <extsearch/video/robot/players/parameters.h>
#include <extsearch/video/robot/players/generator.h>
#include <extsearch/video/meta/rearrs/videoplayers/qproxy.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/stream/file.h>
#include <util/string/vector.h>
#include <util/string/strip.h>
#include <util/string/subst.h>
#include <util/system/env.h>


namespace NVideo {

    using namespace NUtil::NYastaticPlayer;

    static const TString LABEL_OK = "OK";
    static const TString LABEL_BAD = "BAD";
    static const TString LABEL_UNK = "UNK";

    class TStationPlayerGenerator {
    public:
        struct TPlayerInfo {
            TPlayerInfo()
                : Label(LABEL_UNK)
                {}

            TString Url;
            TString Label;
            TString Params;
        };

        TStationPlayerGenerator(const TString& generatorsConfig, const TString& unbanConfig, const TString& hmacSecret) {
            TIFStream configStream(generatorsConfig);
            PlayerGenerator.Reset(new NVideo::NPlayers::TGenerator(configStream));
            QProxyGenerator.Reset(new NVideoPlayersRule::TQProxyUrlGenerator(hmacSecret));
            TIFStream unbanStream(unbanConfig);
            PlayerUnban.Reset(NVideo::NPlayers::TUnban::CreateFromJson(unbanStream));
        }

        void GetPlayerInfo(const TString& pageUrl, const TString& playerData, TPlayerInfo& result) {
            result = TPlayerInfo();
            NVideo::TVideoPlayer videoPlayer;
            if (NVideo::TProtoReader::FromStringJson(playerData, videoPlayer)) {
                NVideo::NPlayers::TPlayerParams params(videoPlayer);
                params.Add("autoplay", "false");
                auto playerCode = PlayerGenerator->GetPlayer(params);
                auto playerId = videoPlayer.GetPlayerId();
                if (playerId == "vh" || playerId == "ott") {
                    result.Label = "OK";
                } else if (playerId == "youtube") {
                    CGIEscape(playerCode);
                    SubstGlobal(playerCode, "+", "%20");
                    result.Url.assign(VIDEO_PLAYER_TEMPLATE);
                    result.Url.append(playerCode);
                } else {
                    auto unbanQuasar = PlayerUnban->GetAttrsBuilder()
                        .SetPlayerId(playerId)
                        .SetUrl(pageUrl)
                        .SetPlatform("quasar")
                        .Build();
                    if (PlayerUnban->IsPlayerUnbanned(unbanQuasar)) {
                        CGIEscape(playerCode);
                        SubstGlobal(playerCode, "+", "%20");
                        result.Url.assign(VIDEO_PLAYER_TEMPLATE);
                        result.Url.append(playerCode);
                        result.Params = "jsapi.play=1,browser=chromedriver";
                    } else {
                        auto unbanProxy = PlayerUnban->GetAttrsBuilder()
                            .SetPlayerId(playerId)
                            .SetUrl(pageUrl)
                            .SetPlatform("quasar_proxy")
                            .Build();
                        if (PlayerUnban->IsPlayerUnbanned(unbanProxy)) {
                            auto videoCode = TStringBuilder() << "<video src=\""
                                << QProxyGenerator->GenerateQProxyURL(pageUrl, playerCode, "") << "\" controls>";
                            CGIEscape(videoCode);
                            SubstGlobal(videoCode, "+", "%20");
                            result.Url.assign(VIDEO_PLAYER_TEMPLATE);
                            result.Url.append(videoCode);
                        } else {
                            result.Label = LABEL_BAD;
                        }
                    }
                }
            }
        }
    private:
        THolder<NVideo::NPlayers::TGenerator> PlayerGenerator;
        THolder<NVideoPlayersRule::TQProxyUrlGenerator> QProxyGenerator;
        THolder<NVideo::NPlayers::TUnban> PlayerUnban;
    };
}

int main(int argc, const char* argv[]) {
    TString inputFile, outputFile;
    TString generatorsConfig, unbanConfig;
    NLastGetopt::TOpts opts = NLastGetopt::TOpts::Default();
    opts.AddLongOption('i', "input", "Input data")
        .RequiredArgument("TSV file")
        .Required()
        .StoreResult(&inputFile);
    opts.AddLongOption('o', "output", "Output data")
        .RequiredArgument("TSV file")
        .Required()
        .StoreResult(&outputFile);
    opts.AddLongOption('g', "generators", "Player generators config")
        .RequiredArgument("file")
        .Required()
        .StoreResult(&generatorsConfig);
    opts.AddLongOption('u', "unban", "Player unban config")
        .RequiredArgument("file")
        .Required()
        .StoreResult(&unbanConfig);
    NLastGetopt::TOptsParseResult(&opts, argc, argv);
    NVideo::TStationPlayerGenerator generator(generatorsConfig, unbanConfig, Strip(GetEnv("HMAC_SECRET")));
    TIFStream ifs(inputFile);
    TOFStream ofs(outputFile);
    TString line;
    while (ifs.ReadLine(line)) {
        auto fields = SplitString(line, "\t");
        if (fields.size() >= 2) {
            const auto& pageUrl = fields[0];
            const auto& playerParams = fields[1];
            NVideo::TStationPlayerGenerator::TPlayerInfo info;
            generator.GetPlayerInfo(pageUrl, playerParams, info);
            ofs << pageUrl << "\t" << info.Label << "\t" << info.Url << "\t" << info.Params << Endl;
        }
    }
}
