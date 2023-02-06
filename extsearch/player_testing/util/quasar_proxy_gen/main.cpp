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

    class TStationPlayerGenerator {
    public:
        TStationPlayerGenerator(const TString& generatorsConfig, const TString& hmacSecret) {
            TIFStream configStream(generatorsConfig);
            PlayerGenerator.Reset(new NVideo::NPlayers::TGenerator(configStream));
            QProxyGenerator.Reset(new NVideoPlayersRule::TQProxyUrlGenerator(hmacSecret));
        }

        TString GetQProxyUrl(const TString& pageUrl, const TString& playerData) {
            TString result;
            NVideo::TVideoPlayer videoPlayer;
            if (NVideo::TProtoReader::FromStringJson(playerData, videoPlayer)) {
                NVideo::NPlayers::TPlayerParams params(videoPlayer);
                params.Add("autoplay", "false");
                auto playerCode = PlayerGenerator->GetPlayer(params);
                auto playerId = videoPlayer.GetPlayerId();
                result = QProxyGenerator->GenerateQProxyURL(pageUrl, playerCode, "");
            }
            return result;
        }
    private:
        THolder<NVideo::NPlayers::TGenerator> PlayerGenerator;
        THolder<NVideoPlayersRule::TQProxyUrlGenerator> QProxyGenerator;
    };
}

int main(int argc, const char* argv[]) {
    TString inputFile, outputFile;
    TString generatorsConfig;
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
    NLastGetopt::TOptsParseResult(&opts, argc, argv);
    NVideo::TStationPlayerGenerator generator(generatorsConfig, Strip(GetEnv("HMAC_SECRET")));
    TIFStream ifs(inputFile);
    TOFStream ofs(outputFile);
    TString line;
    while (ifs.ReadLine(line)) {
        auto fields = SplitString(line, "\t");
        if (fields.size() >= 2) {
            const auto& pageUrl = fields[0];
            const auto& playerParams = fields[1];
            auto url = generator.GetQProxyUrl(pageUrl, playerParams);
            if (!url.empty()) {
                ofs << pageUrl << "\t" << generator.GetQProxyUrl(pageUrl, playerParams) << Endl;
            }
        }
    }
}
