#include <crypta/lib/native/resource_service/client/client.h>
#include <crypta/lib/native/resource_service/provider/updatable_provider.h>
#include <crypta/lib/native/resource_service/reporter/reporter.h>
#include <crypta/lib/proto/resource_service/reporter_options.pb.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;
using namespace NCrypta::NResourceService;

namespace {
    using TValue = ui64;
    using TReports = THashMap<TString, ui64>;
    using TFailures = THashMap<TString, ui64>;

    const TString FIRST = "first";
    const TString SECOND = "second";

    const int REPORT_INTERVAL_SEC = 1;
    const int RESOURCE_OK_AGE_SEC = 2;
    const int RESOURCE_OK_REPEAT_SEC = 3;
    const TValue DEFAULT_VALUE = 0;

    class TMockClient : public IClient {
    public:
        TMockClient(TReports& reports, TFailures& failures, bool& failRequests)
            : Reports(reports)
            , Failures(failures)
            , FailRequests(failRequests) {
        }

        ui64 GetVersion(const TString& resourceName) override {
            Y_UNUSED(resourceName);
            ythrow yexception() << "Is not supposed to be used";
        };

        TString GetResource(const TString& resourceName, ui64 version) override {
            Y_UNUSED(resourceName, version);
            ythrow yexception() << "Is not supposed to be used";
        };

        void ReportOk(const TString& resourceName, ui64 version) override {
            if (FailRequests) {
                Failures[resourceName]++;
                ythrow yexception() << "Fail";
            }
            Reports.emplace(resourceName, version);
        };

    private:
        TReports& Reports;
        TFailures& Failures;
        bool& FailRequests;
    };

    class TParser {
    public:
        static TValue Parse(const TString& input) {
            return FromString<TValue>(input);
        }
    };

    struct TFixture {
        TUpdatableProvider<TValue, TParser> First;
        TUpdatableProvider<TValue, TParser> Second;
        TVector<std::reference_wrapper<IMetaProvider>> MetaProviders;
        TReporter Reporter;
        TReports Reports;
        TFailures Failures;
        bool FailRequests = false;

        TFixture(const TReporterOptions& options)
                : First(DEFAULT_VALUE, FIRST)
                , Second(DEFAULT_VALUE, SECOND)
                , MetaProviders({First, Second})
                , Reporter(options, MakeHolder<TMockClient>(Reports, Failures, FailRequests), MetaProviders) {
        }

        void Test(TReports reference, TFailures failures = {}) {
            UNIT_ASSERT_EQUAL(reference, Reports);
            UNIT_ASSERT_EQUAL(failures, Failures);
        }

        void ReportAndTest(TReports reference, TFailures failures = {}) {
            Reporter.Report();
            Test(std::move(reference), std::move(failures));
        }
    };

    TReporterOptions MakeOptions(int retries) {
        TReporterOptions options;
        options.SetReportIntervalSec(REPORT_INTERVAL_SEC);
        options.SetResourceOkAgeSec(RESOURCE_OK_AGE_SEC);
        options.SetResourceOkRepeatSec(RESOURCE_OK_REPEAT_SEC);

        auto& retryOptions = *options.MutableRetryOptions();
        retryOptions.SetMaxTries(retries);
        retryOptions.SetInitialSleepMs(REPORT_INTERVAL_SEC * 1000);
        retryOptions.SetExponentalMultiplierMs(0);

        return options;
    }
};

Y_UNIT_TEST_SUITE(TUpdater) {
    Y_UNIT_TEST(NoRetries) {
        auto options = MakeOptions(0);

        TFixture fixture(options);
        fixture.ReportAndTest({});

        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC));
        fixture.ReportAndTest({});

        fixture.First.Update("1", 1);
        fixture.ReportAndTest({});

        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC) / 2);
        fixture.Second.Update("2", 2);
        fixture.ReportAndTest({});

        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC) / 2);
        fixture.ReportAndTest({{FIRST, 1}});

        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC) / 2);
        fixture.ReportAndTest({{FIRST, 1}, {SECOND, 2}});
    }

    Y_UNIT_TEST(Retries) {
        auto options = MakeOptions(3);

        TFixture fixture(options);
        fixture.ReportAndTest({});

        fixture.FailRequests = true;
        fixture.First.Update("1", 1);
        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC));
        fixture.ReportAndTest({}, {{FIRST, 4}});

        auto thread = SystemThreadFactory()->Run([&](){
            Sleep(TDuration::Seconds(REPORT_INTERVAL_SEC * 2.5));
            fixture.FailRequests = false;
        });

        fixture.ReportAndTest({{FIRST, 1}}, {{FIRST, 7}});

        thread->Join();
    }

    Y_UNIT_TEST(RunInBackground) {
        auto options = MakeOptions(0);

        TFixture fixture(options);
        fixture.Test({});

        fixture.Reporter.Start();
        Sleep(TDuration::Seconds(REPORT_INTERVAL_SEC));
        fixture.Test({});

        fixture.First.Update("1", 2);
        fixture.Test({});

        Sleep(TDuration::Seconds(RESOURCE_OK_AGE_SEC + REPORT_INTERVAL_SEC));
        fixture.Test({{FIRST, 2}});
        fixture.Reports.clear();
        fixture.Test({});

        Sleep(TDuration::Seconds(REPORT_INTERVAL_SEC));
        fixture.Test({});

        Sleep(TDuration::Seconds(RESOURCE_OK_REPEAT_SEC));
        fixture.Test({{FIRST, 2}});
    }
}
