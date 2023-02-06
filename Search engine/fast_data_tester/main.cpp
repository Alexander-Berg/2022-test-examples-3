#include <search/web/rearrange/special_event/common/fast_data.h>

#include <library/cpp/getopt/last_getopt.h>
#include <util/folder/path.h>

using namespace NLastGetopt;

void Ensure(bool value, TStringBuf str) {
    if (!value) {
        ythrow yexception() << str;
    }
}

void TestMiniwizardItem(const NSc::TValue& item) {
    Ensure(item["id"].StringSize(), "miniwizards item id");
    Ensure(item["cols"].GetIntNumber() > 0, "miniwizards item cols");
    Ensure(item["tableType"] == "thumb", "miniwizards item tableType");
    Ensure(item["thumb"].DictSize(), "miniwizards item thumb");
}

void TestSerpData(const TSpecialEventFastData& fd, TString serpDataTemplate) {
    const NSc::TValue& serpData = fd.GetValue(serpDataTemplate);
    for (const auto& [platform, data] : serpData.GetDict()) {
        Ensure(IsIn({"desktop", "tablet", "touch"}, platform), serpDataTemplate + " platform");

        Ensure(data["parent_collection"]["colorScheme"].DictSize(), serpDataTemplate + " colorScheme");
        Ensure(data["parent_collection"]["arrowTheme"].StringSize(), serpDataTemplate + " arrowTheme");
        Ensure(data["parent_collection"]["header"].DictSize(), serpDataTemplate + " header");
        Ensure(data["parent_collection"]["share"].DictSize(), serpDataTemplate + " share");
        Ensure(data["parent_collection"]["id"].StringSize(), serpDataTemplate + " id");
        Ensure(data["parent_collection"]["tabs"].ArraySize(), serpDataTemplate + " tabs");

        for (const NSc::TValue& tab : data["parent_collection"]["tabs"].GetArray()) {
            Ensure(tab["_tab_id"].StringSize(), serpDataTemplate + " tab id");
            Ensure(tab["blocks"].ArraySize(), serpDataTemplate + " tab blocks");
            Ensure(tab["title"].StringSize(), serpDataTemplate + " tab title");
        }
    }
}

void TestFastData(const TString& path) {
    TSpecialEventFastData fd;
    Ensure(fd.Load(path), "load fastdata");

    const NSc::TValue& countryData = fd.GetValue("country_data");
    Ensure(countryData.DictSize(), "country_data size");
    for (const auto& [name, data] : countryData.GetDict()) {
        Ensure(name.Size() == 2, "country_data key");

        Ensure(data["restricted_links"].ArraySize(), "country_data restricted_links");
        Ensure(data["restricted_links"][0].StringSize(), "country_data restricted_links[0]");

        Ensure(data["docs"].ArraySize(), "country_data docs");
        for (const NSc::TValue& doc : data["docs"].GetArray()) {
            Ensure(doc["title"].StringSize(), "country_data doc title");
            Ensure(doc["pda_url"].StringSize(), "country_data doc pda_url");
        }
    }

    const NSc::TValue& miniwizardsData = fd.GetValue("miniwizards");
    Ensure(miniwizardsData.DictSize(), "miniwizards size");
    for (const auto& [name, data] : miniwizardsData.GetDict()) {
        for (const auto& [platform, item] : data.GetDict()) {
            Ensure(IsIn({"desktop", "tablet", "touch"}, platform), "miniwizards platform");
            if (platform == "touch") {
                Ensure(item["blockType"] == "columns", "miniwizards touch blockType");
                Ensure(item["items"].ArraySize(), "miniwizards touch items");
                for (const NSc::TValue& touchItem : item["items"].GetArray()) {
                    TestMiniwizardItem(touchItem);
                }
            } else {
                TestMiniwizardItem(item);
            }
        }
    }

    TestSerpData(fd, "serp_data_kuub");
    TestSerpData(fd, "serp_data_ru");
}

int main(int argc, char* argv[]) {
    TOpts opts = TOpts::Default();
    TString path;
    opts.AddCharOption('p', "path").Required().StoreResult(&path);
    TOptsParseResult parseResult(&opts, argc, argv);

    try {
        TestFastData(path);
    } catch (...) {
        Cerr << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
