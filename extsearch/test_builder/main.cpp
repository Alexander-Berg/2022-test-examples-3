#include <extsearch/video/kernel/delayed_view/lib/builder.h>
#include <extsearch/video/kernel/delayed_view/lib/delayed_view.h>
#include <extsearch/video/kernel/delayed_view/lib/env.h>
#include <extsearch/video/kernel/delayed_view/lib/filter.h>
#include <extsearch/video/kernel/delayed_view/lib/params.h>

#include <ysite/yandex/pers/userdata/io/userprofile/io.h>
#include <ysite/yandex/pers/userdata/types/userprofile/userprofile.h>

#include <kernel/relev_locale/relev_locale.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/object_factory/object_factory.h>

#include <util/datetime/base.h>
#include <util/folder/path.h>
#include <util/generic/strbuf.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/generic/yexception.h>
#include <util/memory/blob.h>
#include <util/stream/file.h>

#include <functional>
#include <utility>

namespace {
    using TDelayedViewWorker = std::function<
        NVideo::NDelayedView::TDelayedViews (const NUserProfiles::TUserProfile&)
    >;

    class TDelayedViewWorkersFactory {
    public:
        explicit TDelayedViewWorkersFactory(const NVideo::NDelayedView::TEnvironment& environment)
            : Environment(environment)
        {
        }

        TDelayedViewWorker GetWorker(const NVideo::NDelayedView::TWorkerParams& params) const {
            auto builder = GetBuilder(params, Environment);
            auto filter = GetFilter(params);
            return [builder, filter] (const NUserProfiles::TUserProfile& userProfile) {
                auto delayedViews = builder.BuildDelayedViews(userProfile);
                return filter.FilterDelayedViews(std::move(delayedViews));
            };
        }

    private:
        const NVideo::NDelayedView::TEnvironment& Environment;

        static NVideo::NDelayedView::TDelayedViewBuilder GetBuilder(
            const NVideo::NDelayedView::TWorkerParams& params,
            const NVideo::NDelayedView::TEnvironment& environment)
        {
            return NVideo::NDelayedView::TDelayedViewBuilder()
                .SetEnvironment(environment)
                .SetRelevLocale(NRl::ERelevLocale::RL_RU)
                .SetParams(params);
        }

        static NVideo::NDelayedView::TDelayedViewFilter GetFilter(
            const NVideo::NDelayedView::TWorkerParams& params)
        {
            return NVideo::NDelayedView::TDelayedViewFilter().SetParams(params);
        }
    };

    class TBaseConfig {
    public:
        TBaseConfig() {
            Options.AddLongOption("entity_trie", "-- entity base trie file path")
                .Optional()
                .RequiredArgument("FILE_PATH")
                .StoreResult(&EntityBaseTrieFilePath);

            Options.AddLongOption("serial_trie", "-- serial base trie file path")
                .Optional()
                .RequiredArgument("FILE_PATH")
                .StoreResult(&SerialBaseTrieFilePath);
        }

        void Initialize(int argc, const char** argv) {
            NLastGetopt::TOptsParseResult parsingResult(&Options, argc, argv);
        }

        NVideo::NDelayedView::TEnvironment GetEnvironment() const {
            NVideo::NDelayedView::TEnvironmentParams environmentParams;
            environmentParams.EntityBaseTriePath = EntityBaseTrieFilePath;
            environmentParams.SerialBaseTriePath = SerialBaseTrieFilePath;

            NVideo::NDelayedView::TEnvironment environment;
            environment.InitFromParams(environmentParams);

            return environment;
        }

    protected:
        NLastGetopt::TOpts Options;

    private:
        TString EntityBaseTrieFilePath;
        TString SerialBaseTrieFilePath;
    };

    class TManualTestsConfig : public TBaseConfig {
    public:
        TManualTestsConfig()
            : TBaseConfig()
        {
            Options.AddLongOption("test_name", "-- test name")
                .Required()
                .RequiredArgument("TEST_NAME")
                .StoreResult(&TestName);
        }

        TString GetTestName() const {
            return TestName;
        }
    private:
        TString TestName;
    };

    class TAutoTestsConfig : public TBaseConfig {
    public:
        TAutoTestsConfig()
            : TBaseConfig()
        {
            Options.AddLongOption("profiles", "-- user profiles file path")
                .Required()
                .RequiredArgument("FILE_PATH")
                .StoreResult(&ProfilesFilePath);
        }

        TString GetProfilesFilePath() const {
            return ProfilesFilePath;
        }

    private:
        TString ProfilesFilePath;
    };

    template <typename TDelayedViewWorkersContainer, typename TProfilesContainer>
    void Invoke(const TDelayedViewWorkersContainer& workers, const TProfilesContainer& profiles) {
        for (const auto& profile : profiles) {
            for (const auto& worker : workers) {
                auto delayedViews = worker(profile);
                const auto delayedViewsJson = DelayedViewsToJson(std::move(delayedViews));
                const auto delayedViewsStr = NJson::WriteJson(delayedViewsJson, true, true, false);
                Cout << delayedViewsStr << Endl;
            }
        }
    }

    class TUserProfileBuilder {
    public:
        virtual ~TUserProfileBuilder() {
        }

        void InsertEntity(NUserProfiles::TEntityVideoViewData entityVideoViewData) {
            InsertWithUniqReqId(
                std::move(entityVideoViewData),
                UserProfile.VideoViewsStats.EntityVideoViews
            );
        }

        void InsertSerial(NUserProfiles::TSerialVideoViewData serialVideoViewData) {
            InsertWithUniqReqId(
                std::move(serialVideoViewData),
                UserProfile.VideoViewsStats.SerialVideoViews
            );
        }

        NUserProfiles::TUserProfile Get() const {
            return UserProfile;
        }

    private:
        NUserProfiles::TUserProfile UserProfile;

        template <typename TVideoViewData, typename TVideoViewDataContainer>
        void InsertWithUniqReqId(
            TVideoViewData videoViewData,
            TVideoViewDataContainer& container)
        {
            THashSet<TString> usedReqIds;
            for (const auto& keyValuePair : container) {
                usedReqIds.insert(keyValuePair.first.first);
            }
            ui32 reqId = 0;
            while (usedReqIds.find(ToString(reqId)) != usedReqIds.end()) {
                ++reqId;
            }
            videoViewData.ReqId = ToString(reqId);
            const auto key = NUserProfiles::TReqIdAndUrlPair(ToString(reqId), videoViewData.Url);
            container[key] = std::move(videoViewData);
        }
    };

    NVideo::NDelayedView::TWorkerParams GetDefaultParams() {
        NVideo::NDelayedView::TWorkerParams result;
        result.ExpirationTime = TDuration::Max();
        return result;
    }

    class TDelayedViewParamsBuilder {
    public:
        TDelayedViewParamsBuilder()
            : Params(GetDefaultParams())
        {
        }

        virtual ~TDelayedViewParamsBuilder() {
        }

        NVideo::NDelayedView::TWorkerParams Get() const {
            return Params;
        }

    protected:
        NVideo::NDelayedView::TWorkerParams Params;
    };

    using TManualTestProfileBuildersRegistry = NObjectFactory::TObjectFactory<
        TUserProfileBuilder, TString
    >;

    using TManualTestParamsBuildersRegistry = NObjectFactory::TObjectFactory<
        TDelayedViewParamsBuilder, TString
    >;

    #define REGISTER_MANUAL_TEST_PROFILE(TestName, ProfileClassName)\
        TManualTestProfileBuildersRegistry::TRegistrator<ProfileClassName>\
            TestName ## ProfilesRegistrator{#TestName}

    #define REGISTER_MANUAL_TEST_PARAMS(TestName, ParamsClassName)\
        TManualTestParamsBuildersRegistry::TRegistrator<ParamsClassName>\
            TestName ## ParamsRegistrator{#TestName}

    #define PROFILE_IMPL(TestName, ProfileClassName)\
        class ProfileClassName : public TUserProfileBuilder {\
        public:\
            ProfileClassName();\
        };\
        REGISTER_MANUAL_TEST_PROFILE(TestName, ProfileClassName);\
        ProfileClassName::ProfileClassName()

    #define PARAMS_IMPL(TestName, ParamsClassName)\
        class ParamsClassName : public TDelayedViewParamsBuilder {\
        public:\
            ParamsClassName();\
        };\
        REGISTER_MANUAL_TEST_PARAMS(TestName, ParamsClassName);\
        ParamsClassName::ParamsClassName()\
            : TDelayedViewParamsBuilder()

    #define PROFILE(TestName) PROFILE_IMPL(TestName, T ## TestName ## UserProfileBuilder)

    #define PARAMS(TestName) PARAMS_IMPL(TestName, T ## TestName ## DelayedViewParamsBuilder)

    constexpr TStringBuf GREEN_MILE_ENTITY_ID = "ruw274446";
    constexpr TStringBuf SERIAL_ENTITY_TYPE = "Film/Series@on";
    constexpr ui32 GAME_OF_THRONES_SERIAL_ID = 427276432;
    constexpr ui32 BREAKING_BAD_SERIAL_ID = 843037282;
    constexpr ui32 STAN_AGAINST_EVIL_FORCES_SERIAL_ID = 100241249;

    enum class ETicksCountRelativeValue {
        TCRV_SHORT,
        TCRV_MEDIUM,
        TCRV_LONG
    };

    void FillCommon(
        const ui32 idx,
        const ETicksCountRelativeValue ticksCountRelativeValue,
        NUserProfiles::TVideoViewData& videoViewData)
    {
        videoViewData.Url = "url" + ToString(idx);
        videoViewData.Title = "title" + ToString(idx);
        videoViewData.ThumbUrl = "thumburl" + ToString(idx);
        videoViewData.QueryText = "query" + ToString(idx);
        videoViewData.Duration = TDuration::Hours(2);
        if (ticksCountRelativeValue == ETicksCountRelativeValue::TCRV_SHORT) {
            videoViewData.TicksCount = videoViewData.Duration / 10;
        } else if (ticksCountRelativeValue == ETicksCountRelativeValue::TCRV_MEDIUM) {
            videoViewData.TicksCount = videoViewData.Duration / 2;
        } else if (ticksCountRelativeValue == ETicksCountRelativeValue::TCRV_LONG) {
            videoViewData.TicksCount = videoViewData.Duration * 9 / 10;
        }
        videoViewData.Timestamp = TInstant::Hours(5);
    }

    NUserProfiles::TEntityVideoViewData GetGreenMile(
        const ui32 idx,
        const ETicksCountRelativeValue ticksCountRelativeValue)
    {
        NUserProfiles::TEntityVideoViewData result;
        FillCommon(idx, ticksCountRelativeValue, result);
        result.EntityId = GREEN_MILE_ENTITY_ID;
        result.EntityType = "Film/Film";
        return result;
    }

    NUserProfiles::TSerialVideoViewData GetSerial(
        const ui32 idx,
        const ETicksCountRelativeValue ticksCountRelativeValue,
        const ui32 serialId,
        const ui32 seasonId,
        const ui32 episodeId)
    {
        NUserProfiles::TSerialVideoViewData result;
        FillCommon(idx, ticksCountRelativeValue, result);
        result.SerialId = serialId;
        result.SeasonId = seasonId;
        result.EpisodeId = episodeId;
        return result;
    }

    PROFILE(FilmAndNextEpisode) {
        InsertEntity(GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 1, 1
        ));
    }

    PROFILE(TitleFromEntityBase) {
        auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
        result.Title = "";
        InsertEntity(std::move(result));
    }

    PROFILE(AggregateViews) {
        {
            auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
            result.TicksCount = TDuration::Hours(2) / 3;
            result.Timestamp = TInstant::Hours(1);
            InsertEntity(std::move(result));
        }
        {
            auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
            result.TicksCount = TDuration::Hours(2) / 3;
            result.Timestamp = TInstant::Days(10) + TDuration::Hours(1);
            InsertEntity(std::move(result));
        }
        {
            auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
            result.TicksCount = TDuration::Hours(2) / 3;
            result.Timestamp = TInstant::Days(10) + TDuration::Hours(2);
            InsertEntity(std::move(result));
        }
    }

    PROFILE(ShortView) {
        InsertEntity(GetGreenMile(1, ETicksCountRelativeValue::TCRV_SHORT));
    }

    PROFILE(LongView) {
        InsertEntity(GetGreenMile(1, ETicksCountRelativeValue::TCRV_LONG));
    }

    PROFILE(LastNextEpisode) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 1, 1
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 1, 3
        ));
        InsertSerial(GetSerial(
            3, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 1, 5
        ));
    }

    PROFILE(NextEpisodeInNextSeason) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 1, 10
        ));
    }

    PROFILE(DuplicateNextEpisodes) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 2, 1
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 2, 1
        ));
    }

    PROFILE(DifferentSerialsNextEpisodes) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 5, 1
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, BREAKING_BAD_SERIAL_ID, 3, 5
        ));
    }

    PROFILE(LongViewWithoutNextEpisode) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 7, 7
        ));
    }

    PROFILE(SerialBeforeNextEpisode) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, GAME_OF_THRONES_SERIAL_ID, 3, 3
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 3, 3
        ));
    }

    PROFILE(SerialAfterNextEpisode) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, GAME_OF_THRONES_SERIAL_ID, 3, 4
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 3, 3
        ));
    }

    PROFILE(NextEpisodeWithoutSeason) {
        auto result = GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, STAN_AGAINST_EVIL_FORCES_SERIAL_ID, 1, 1
        );
        result.SeasonId = NUserProfiles::UNKNOWN_SEASON_ID;
        InsertSerial(std::move(result));
    }

    PROFILE(NextEpisodeWithoutEpisode) {
        auto result = GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 5, 1
        );
        result.EpisodeId = NUserProfiles::UNKNOWN_EPISODE_ID;
        InsertSerial(std::move(result));
    }

    PROFILE(LongViewWithoutNextEpisodeAfterNextEpisode) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 5, 5
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_LONG, GAME_OF_THRONES_SERIAL_ID, 7, 7
        ));
    }

    PROFILE(TitleFromSerialBase) {
        auto result = GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, GAME_OF_THRONES_SERIAL_ID, 1, 1
        );
        result.Title = "";
        InsertSerial(std::move(result));
    }

    PROFILE(QueryTextFromTitle) {
        auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
        result.QueryText = "";
        InsertEntity(std::move(result));
    }

    PROFILE(FilterRelatedRequestQueryText) {
        auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
        result.IsRelatedRequest = true;
        InsertEntity(std::move(result));
    }

    PROFILE(FilterVideoMordaRequestQueryText) {
        auto result = GetGreenMile(1, ETicksCountRelativeValue::TCRV_MEDIUM);
        result.IsMordaRequest = true;
        InsertEntity(std::move(result));
    }

    PROFILE(SerialEntityBasesPriority) {
        auto result = GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, GAME_OF_THRONES_SERIAL_ID, 1, 1
        );
        result.EntityId = GREEN_MILE_ENTITY_ID;
        result.EntityType = SERIAL_ENTITY_TYPE;
        result.Title = "";
        InsertSerial(std::move(result));
    }

    PROFILE(SerialInfoAggregation) {
        {
            auto result = GetSerial(
                1, ETicksCountRelativeValue::TCRV_SHORT, NUserProfiles::UNKNOWN_SERIAL_ID, 1, 1
            );
            result.Timestamp = TInstant::Days(10);
            result.Title = "";
            InsertSerial(std::move(result));
        }
        {
            auto result = GetSerial(
                1, ETicksCountRelativeValue::TCRV_SHORT, GAME_OF_THRONES_SERIAL_ID, 1, 1
            );
            result.Timestamp = TInstant::Days(11);
            result.Title = "";
            InsertSerial(std::move(result));
        }
    }

    PROFILE(UnknownSerial) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, NUserProfiles::UNKNOWN_SERIAL_ID, 1, 1
        ));
    }

    PROFILE(ZeroLongViewPercLength) {
        InsertEntity(GetGreenMile(1, ETicksCountRelativeValue::TCRV_LONG));
    }

    PARAMS(ZeroLongViewPercLength) {
        Params.LongViewPercLength = 0;
    }

    PROFILE(EntityAggregation) {
        InsertEntity(GetGreenMile(1, ETicksCountRelativeValue::TCRV_SHORT));
        InsertEntity(GetGreenMile(2, ETicksCountRelativeValue::TCRV_SHORT));
    }

    PARAMS(EntityAggregation) {
        Params.EntityAggregation = true;
    }

    PROFILE(SerialAggregationSameEpisodes) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_SHORT, GAME_OF_THRONES_SERIAL_ID, 1, 1
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_SHORT, GAME_OF_THRONES_SERIAL_ID, 1, 1
        ));
    }

    PARAMS(SerialAggregationSameEpisodes) {
        Params.SerialAggregation = true;
    }

    PROFILE(SerialAggregationDifferentEpisodes) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_SHORT, GAME_OF_THRONES_SERIAL_ID, 1, 1
        ));
        InsertSerial(GetSerial(
            2, ETicksCountRelativeValue::TCRV_SHORT, GAME_OF_THRONES_SERIAL_ID, 1, 2
        ));
    }

    PARAMS(SerialAggregationDifferentEpisodes) {
        Params.SerialAggregation = true;
    }

    PROFILE(SerialAggregationNotDefined) {
        InsertSerial(GetSerial(
            1, ETicksCountRelativeValue::TCRV_MEDIUM, GAME_OF_THRONES_SERIAL_ID,
            NUserProfiles::UNKNOWN_SEASON_ID, NUserProfiles::UNKNOWN_EPISODE_ID
        ));
    }

    PARAMS(SerialAggregationNotDefined) {
        Params.SerialAggregation = true;
    }

    #undef PARAMS
    #undef PROFILE
    #undef PARAMS_IMPL
    #undef PROFILE_IMPL
    #undef REGISTER_MANUAL_TEST_PROFILE
} // namespace

int main_manual_test_names(int argc, const char** argv) {
    Y_UNUSED(argc);
    Y_UNUSED(argv);
    TSet<TString> testNames;
    TManualTestProfileBuildersRegistry::GetRegisteredKeys(testNames);
    for (const auto& testName : testNames) {
        Cout << testName << Endl;
    }
    return 0;
}

int main_manual_tests(int argc, const char** argv) {
    TManualTestsConfig config;
    config.Initialize(argc, argv);

    const auto testName = config.GetTestName();
    const auto environment = config.GetEnvironment();

    TVector<TDelayedViewWorker> workers;
    {
        const TDelayedViewWorkersFactory workersFactory(environment);
        const auto paramsBuilderPtr = THolder<TDelayedViewParamsBuilder>(
            TManualTestParamsBuildersRegistry::Construct(testName)
        );
        if (paramsBuilderPtr != nullptr) {
            workers.push_back(workersFactory.GetWorker(paramsBuilderPtr->Get()));
        } else {
            workers.push_back(workersFactory.GetWorker(GetDefaultParams()));
        }
    }

    TVector<NUserProfiles::TUserProfile> profiles;
    {
        const auto profileBuilderPtr = THolder<TUserProfileBuilder>(
            TManualTestProfileBuildersRegistry::Construct(testName)
        );
        if (profileBuilderPtr != nullptr) {
            profiles.push_back(profileBuilderPtr->Get());
        } else {
            ythrow yexception() << "Null profiles builder";
        }
    }

    Invoke(workers, profiles);

    return 0;
}

int main_auto_tests(int argc, const char** argv) {
    TAutoTestsConfig config;
    config.Initialize(argc, argv);

    const auto environment = config.GetEnvironment();

    TVector<TDelayedViewWorker> workers;
    {
        const TDelayedViewWorkersFactory workersFactory(environment);
        auto params = GetDefaultParams();
        for (const ui32 viewExpirationTimeDays : {1, 4, 7}) {
            params.ViewExpirationTime = TDuration::Days(viewExpirationTimeDays);
            for (const ui32 shortViewPercLength : {5, 25}) {
                params.ShortViewPercLength = shortViewPercLength;
                for (const ui32 longViewPercLength : {5, 25}) {
                    params.LongViewPercLength = longViewPercLength;

                    params.EntityAggregation = false;
                    params.SerialAggregation = false;
                    workers.push_back(workersFactory.GetWorker(params));

                    params.EntityAggregation = true;
                    params.SerialAggregation = false;
                    workers.push_back(workersFactory.GetWorker(params));

                    params.EntityAggregation = false;
                    params.SerialAggregation = true;
                    workers.push_back(workersFactory.GetWorker(params));
                }
            }
        }
    }

    TVector<NUserProfiles::TUserProfile> profiles;
    {
        const auto profilesFilePath = config.GetProfilesFilePath();

        if (!TFsPath(profilesFilePath).Exists()) {
            Cerr << "Failed to find profiles file" << Endl;
            return 1;
        }

        TUnbufferedFileInput profilesFile(profilesFilePath);

        TString profileStr;
        while (profilesFile.ReadLine(profileStr)) {
            const auto profilePtr = NUserProfiles::ParseProfile(profileStr);
            if (!profilePtr) {
                Cerr << "Failed to parse profile" << Endl;
                return 1;
            }
            profiles.push_back(std::move(*profilePtr));
        }
    }

    Invoke(workers, profiles);

    return 0;
}

int main(int argc, const char** argv) {
    TModChooser modChooser;

    modChooser.AddMode(
        "manual_test_names",
        main_manual_test_names,
        "-- output manual test names"
    );

    modChooser.AddMode(
        "manual",
        main_manual_tests,
        "-- invoke manual tests"
    );

    modChooser.AddMode(
        "auto",
        main_auto_tests,
        "-- invoke auto tests"
    );

    return modChooser.Run(argc, argv);
}
