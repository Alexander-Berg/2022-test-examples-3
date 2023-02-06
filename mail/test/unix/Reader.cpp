#include <util/generic/string.h>
#include <library/cpp/getopt/last_getopt.h>
#include <mail/so/spamstop/sp/setrules.h>
#include <mail/so/spamstop/sp/spamrule.h>

int main(int argc, char** argv) try {
    TVector<TFsPath> workDir;
    TMaybe<TFsPath> hsCache;
    {
        TFsPath hsCacheTmp;
        TString workDirTmp;
        {
            NLastGetopt::TOpts opts;
            opts.AddCharOption('R', "path to rules folder").StoreResult(&workDirTmp).Required();
            opts.AddCharOption('C', "path to hyperscan rules cache file").StoreResult(&hsCacheTmp).Optional();
            NLastGetopt::TOptsParseResult(&opts, argc, argv);
        }

        for(auto tok : StringSplitter(workDirTmp).SplitBySet(" ,;\t").SkipEmpty()) {
            workDir.emplace_back(TFsPath(tok.Token()).RealPath());
            NFs::EnsureExists(workDir.back());
        }

        if(hsCacheTmp)
            hsCache = std::move(hsCacheTmp);
    }

    TSpLogger logger;
    logger.Openlog(SO_LOG_EVERYTHING, "stdout");
    TSetRules setRules(workDir, &logger, NRegexp::TSettings{});

    if (setRules.IsOk() != ecOK) {
        printf("%s\n", "Couldn't open filter! Please check file spam.ini");
        pFilterLogger->splog (TLOG_ERR, "%s\n", "Couldn't open filter! Please check file spam.ini");
        return -1;
    }

    TVector<THolder<TRuleDef>> ppprules;
    setRules.GetRules(ppprules, hsCache);

    return 0;
} catch (...) {
    Cerr << __FILE__ << ':' << __LINE__ << ' ' << CurrentExceptionMessageWithBt() << Endl;
    return 1;
}
