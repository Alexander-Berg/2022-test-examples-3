#include <search/web/util/banner/uniformat/adapter.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/scheme/scheme.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

void Uniformat(const TString& fileName) {
    TFileInput inputFile(fileName);
    const NSc::TValue uniformatBanners = NSc::TValue::FromJsonThrow(inputFile.ReadAll());
    Cout << NBannerAttrs::FromUniformat(uniformatBanners)
        .ToJson(NSc::TValue::JO_SORT_KEYS | NSc::TValue::JO_PRETTY) << Endl;
}

int main(int argc, const char* argv[]) {
    auto opts = NLastGetopt::TOpts::Default();
    opts.SetFreeArgsNum(1);
    opts.SetFreeArgTitle(0, "uniformat_banners", "json with banners in uniformat or path with jsons");
    NLastGetopt::TOptsParseResult optsParsed(&opts, argc, argv);

    auto args = optsParsed.GetFreeArgs();

    TFsPath utData{args[0]};
    if (utData.IsDirectory()) {
        TVector<TFsPath> banners;
        utData.List(banners);
        Sort(banners.begin(), banners.end(), [](const auto& f1, const auto& f2) { return f1.GetPath() < f2.GetPath(); });
        for (TVector<TFsPath>::const_iterator it = banners.begin(); it != banners.end(); ++it) {
            Uniformat(it->GetPath());
        }
    } else {
        Uniformat(args[0]);
    }

    return 0;
}
