#include <crypta/lib/native/juggler/juggler_client.h>
#include <crypta/lib/native/resource_service/client/client.h>
#include <crypta/lib/native/resource_service/provider/updatable_provider.h>
#include <crypta/lib/native/resource_service/updater/updater.h>
#include <crypta/lib/proto/resource_service/updater_options.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/string/split.h>

using namespace NCrypta;
using namespace NCrypta::NResourceService;

namespace {
    using TValue = ui64;

    using TResources = THashMap<TString, TValue>;
    struct TReport {
        TString Status;
        TString Description;

        TReport(const TString& status, const TString& description)
            : Status(status)
            , Description(description) {
        }

        bool operator==(const TReport& other) const {
            return Status == other.Status && Description == other.Description;
        }
    };
    using TReports = THashMap<TString, TVector<TReport>>;

    const TString FIRST = "first";
    const TString SECOND = "second";
    const TString OK = "OK";
    const TString WARN = "WARN";
    const TString CRIT = "CRIT";
    const TString LOCALHOST = "localhost";
    const TString NOT_FOUND = "not found";

    const int TIMEOUT_SEC = 1;
    const TValue DEFAULT_VALUE = 0;

    const TResources RESOURCES = {
        {FIRST, 1}
    };

    class TMockClient : public IClient {
    public:
        TMockClient(const TResources& resources)
            : Resources(resources) {
        }

        ui64 GetVersion(const TString& resourceName) override {
            const auto& it = Resources.find(resourceName);
            if (it != Resources.end()) {
                return it->second;
            }
            throw yexception() << NOT_FOUND;
        };

        TString GetResource(const TString& resourceName, ui64 version) override {
            const auto& it = Resources.find(resourceName);
            if (it == Resources.end() || it->second != version) {
                throw yexception() << NOT_FOUND;
            }

            return ToString(version);
        };

        void ReportOk(const TString& resourceName, ui64 version) override {
            Y_UNUSED(resourceName, version);
            ythrow yexception() << "Is not supposed to be used";
        };

    private:
        const TResources& Resources;
    };

    class TMockJugglerClient : public IJugglerClient {
    public:
        explicit TMockJugglerClient(TReports& reports)
            : Reports(reports) {
        }

        void ReportOk(const TString& host, const TString& service, const TString& description) override {
            ReportStatus(OK, host, service, description);
        }

        void ReportWarn(const TString& host, const TString& service, const TString& description) override {
            ReportStatus(WARN, host, service, description);
        }

        void ReportCrit(const TString& host, const TString& service, const TString& description) override {
            ReportStatus(CRIT, host, service, description);
        }

    private:
        void ReportStatus(const TString& status, const TString& host, const TString& service, const TString& description) {
            UNIT_ASSERT_EQUAL(host, LOCALHOST);
            Reports[service].emplace_back(status, description);
        }
        TReports& Reports;
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
        TVector<std::reference_wrapper<IUpdatable>> Updatables;
        TUpdater Updater;
        TReports Reports;

        TFixture(const TUpdaterOptions& options, const TResources& resources)
            : First(DEFAULT_VALUE, FIRST)
            , Second(DEFAULT_VALUE, SECOND)
            , Updatables({First, Second})
            , Updater(options, MakeHolder<TMockClient>(resources), MakeHolder<TMockJugglerClient>(Reports), Updatables, LOCALHOST, [](const TString& name){ return name; }) {
        }
    };

    void Test(TFixture& fixture, TValue firstRef, TValue secondRef) {
        UNIT_ASSERT_EQUAL(firstRef, fixture.First.GetResource()->Value);
        UNIT_ASSERT_EQUAL(secondRef, fixture.Second.GetResource()->Value);
    }

    TUpdaterOptions MakeOptions(int retries) {
        TUpdaterOptions options;
        options.SetUpdateIntervalSec(TIMEOUT_SEC);
        options.SetRetryIntervalSec(TIMEOUT_SEC);
        options.SetRetryCount(retries);
        return options;
    }
};

Y_UNIT_TEST_SUITE(TUpdater) {
    Y_UNIT_TEST(NoRetries) {
        TResources resources(RESOURCES);
        TUpdaterOptions options = MakeOptions(0);

        TFixture fixture(options, resources);
        Test(fixture, 0, 0);

        fixture.Updater.Update();
        Test(fixture, 1, 0);

        resources[FIRST] = 10;
        resources[SECOND] = 20;

        fixture.Updater.Update();
        Test(fixture, 10, 20);

        TReports refReports({
            {FIRST, {TReport(OK, TUpdater::JugglerOkDescription),  TReport(OK, TUpdater::JugglerOkDescription)}},
            {SECOND, {TReport(CRIT, NOT_FOUND),  TReport(OK, TUpdater::JugglerOkDescription)}}
        });
        UNIT_ASSERT_EQUAL(refReports, fixture.Reports);
    }

    Y_UNIT_TEST(Retries) {
        TResources resources;
        TUpdaterOptions options = MakeOptions(3);

        TFixture fixture(options, resources);
        Test(fixture, 0, 0);

        auto thread = SystemThreadFactory()->Run([&](){
            Sleep(TDuration::Seconds(TIMEOUT_SEC * 2));
            resources[FIRST] = 1;

            Sleep(TDuration::Seconds(TIMEOUT_SEC * 2));
            Test(fixture, 1, 0);
            resources[SECOND] = 2;
        });

        fixture.Updater.Update();
        Test(fixture, 1, 2);

        thread->Join();
    }

    Y_UNIT_TEST(RunInBackground) {
        TResources resources(RESOURCES);
        TUpdaterOptions options = MakeOptions(0);

        TFixture fixture(options, resources);
        Test(fixture, 0, 0);

        fixture.Updater.Start();
        Sleep(TDuration::Seconds(TIMEOUT_SEC * 2));

        Test(fixture, 1, 0);
        resources[SECOND] = 2;
        Sleep(TDuration::Seconds(TIMEOUT_SEC * 2));

        Test(fixture, 1, 2);
    }
}
