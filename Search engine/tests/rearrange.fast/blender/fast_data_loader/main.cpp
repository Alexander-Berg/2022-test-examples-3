#include <search/meta/data_registry/data_registry.h>
#include <search/web/rearrange/apply_blender/fast_data/fast_data.h>

#include <library/cpp/getopt/last_getopt.h>
#include <util/folder/path.h>

using namespace NLastGetopt;
using namespace NApplyBlender;

int main(int argc, char* argv[]) {
    TOpts opts = TOpts::Default();
    TString path;
    opts.AddCharOption('p', "path").Required().StoreResult(&path);
    TOptsParseResult parseResult(&opts, argc, argv);
    TFastData fd;
    NRearr::TDataRegistry reg;
    TFastData::LoadFromDirectory(fd, path, reg);
    return 0;
}
