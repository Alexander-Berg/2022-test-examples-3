#include <util/stream/output.h>
#include <util/generic/yexception.h>

#include <search/tools/idx_urlnorm/url_comparator.h>
#include <search/tools/idx_util/tsv_input.h>
#include <search/tools/idx_util/verbose.h>

#include <library/cpp/streams/factory/factory.h>

int main() { try {
    using namespace NIdxOps;

    const char ratingsFileName[] = "./ratings.tsv.gz";
    const char errorLogFileName[] = "./output-errors.log";

    TFixedBufferFileOutput errorLog(errorLogFileName);
    TSmartUrlComparator urlComparator(VL_NORMAL);
    ConfigureUrlComparator(urlComparator, errorLog);

    TAutoPtr<IInputStream> pfRatings = OpenInput(ratingsFileName);
    IInputStream& fRatings = *pfRatings;
    TEstimateInfo est;
    TString buffer;

    while (FastRead(fRatings, buffer, est)) {
        TVector<TString> normUrls;
        urlComparator.AddToFilterTest(est.Url, normUrls);
        errorLog << "IN |" << est.Url << "|" << Endl;
        for (size_t i = 0; i < normUrls.size(); ++i) {
            if (normUrls[i] != est.Url) {
                errorLog << "OUT|" << normUrls[i] << "|" << Endl;
            }
        }
    }
    return 0;
} catch(const yexception&) {
    Cerr << "ERROR: " << CurrentExceptionMessage() << Endl;
    return 1;
} }
