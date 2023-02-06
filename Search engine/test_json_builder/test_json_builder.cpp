#include <util/stream/file.h>
#include <library/cpp/archive/yarchive.h>
#include <library/cpp/scheme/scheme.h>
#include <library/cpp/geobase/lookup.hpp>
#include <library/cpp/getopt/last_getopt.h>
#include <kernel/relev_locale/serptld.h>

#include <search/web/blender/ut_utils/blender_test.h>
#include <search/web/rearrange/toponym_wizard/data_builder.h>

using NSc::TValue;

static const unsigned char TEST_DATA[] = {
    #include "toponym_wizard_test.inc"
};
static const TArchiveReader ARCHIVE(TBlob::NoCopy(TEST_DATA, sizeof(TEST_DATA)));

TValue ReadJsonFromArchive(const TString& file) {
    return TValue::FromJsonThrow(ARCHIVE.ObjectByKey(TString::Join("/", file))->ReadAll());
}

TString ReadFile(const TString& file) {
    return TFileInput(file).ReadAll();
}

TValue ReadJsonFromFile(const TString& file) {
    return TValue::FromJsonThrow(ReadFile(file));
}

TValue ConstructLocalScheme() {
    const TValue rearrConf = ReadJsonFromArchive("conf.json");
    for (const auto& ruleConf : rearrConf["rearrange_rules"].GetArray()) {
        if (ruleConf["name"] == "ToponymWizard") {
            auto res = ruleConf["options"];
            res["GeoJsonMode"] = 1;
            return res;
        }
    }
    return {};
}

void FillDocAttrs(const TString& docAttributesFile, TDocDataHolderPtr docRef) {
    TFetchedDocData* data = docRef->MutableFetchedData();
    TValue attrs = ReadJsonFromFile(docAttributesFile);

    for (const auto& key : attrs.DictKeys()) {
        data->AttrValues().Add(key, attrs[key]);
    }
    if (attrs.Has("Title")) {
        data->SetTitle(TString(attrs["Title"].GetString()));
    }
}

THolder<TSearcherProps> ConstructSearcherProps(const TString& file) {
    THolder<TSearcherProps> res(new TSearcherProps);
    (*res)["geojson"] = ReadFile(file);
    return res;
}

TFakeWizardResultHolder ConstructWizProperties(const TString& file) {
    TFakeWizardResultHolder res;
    TVector<TStringBuf> lines;
    TString fileContent = ReadFile(file);
    StringSplitter(fileContent).Split('\n').SkipEmpty().Collect(&lines);
    for (const auto& line: lines) {
        TVector<TStringBuf> fields;
        StringSplitter(line).Split('\t').SkipEmpty().Collect(&fields);
        Y_ENSURE(fields.size() == 3, TString::Join("wrong wiz properties file format: line [", line, "]").data());
        res.AddProperty(fields[0], fields[1], fields[2]);
    }
    return res;
}

TValue BuildJsonForWizard(const TString& geojsonFile,
    const TString& wizPropsFile,
    const TString& docAttributesFile,
    const TString& userRegion,
    TStringBuf tld,
    TStringBuf lang,
    const TString& geodataFile)
{
    THolder<NGeobase::TLookup> geoBase(new NGeobase::TLookup(geodataFile.data()));

    auto localScheme = ConstructLocalScheme();

    auto wizPropsHolder = ConstructWizProperties(wizPropsFile);
    TWizardResultWrapper wizProps;
    wizProps.Reset(&wizPropsHolder);

    TWizDataBuilder builder(geoBase.Get());

    TValue cfgScheme;
    TConfigStub cfg(cfgScheme);
    auto docRef = cfg.DocSource.GetDocStorage().ConstructDoc();
    FillDocAttrs(docAttributesFile, docRef);
    TVector<TMergedDoc> docs{TMergedDoc(cfg.DocSource.GetDocDataEnv(), docRef)};
    auto searcherProps = ConstructSearcherProps(geojsonFile);

    return builder.Build(docs,
        wizProps,
        FromString<EYandexSerpTld>(tld),
        lang,
        FromString<TCateg>(userRegion),
        localScheme,
        !searcherProps->Get<TStringBuf>("geojson"),
        searcherProps.Get()
    );
}

int main(int argc, char* argv[]) {
    auto opts = NLastGetopt::TOpts::Default();
    opts.SetFreeArgsNum(7);
    opts.SetFreeArgTitle(0, "geojson", "file with geojson");
    opts.SetFreeArgTitle(1, "wiz_properties", "file with wizard properties");
    opts.SetFreeArgTitle(2, "doc_attrs", "file with document attributes");
    opts.SetFreeArgTitle(3, "user_region", "user region");
    opts.SetFreeArgTitle(4, "tld", "tld");
    opts.SetFreeArgTitle(5, "lang", "lang");
    opts.SetFreeArgTitle(6, "geodata4-tree+ling.bin", "");
    NLastGetopt::TOptsParseResult r(&opts, argc, argv);

    try {
        auto args = r.GetFreeArgs();
        Cout << BuildJsonForWizard(args[0], args[1], args[2], args[3], args[4], args[5], args[6]).ToJson(NSc::TJsonOpts(NSc::TJsonOpts::JO_PRETTY)) << Endl;
    } catch (const std::exception& e) {
        Cout << "Exception:" << Endl << e.what() << Endl;
    }
    return 0;
}
