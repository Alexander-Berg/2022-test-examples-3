#include <market/access/adapter/env/env.h>
#include <market/access/adapter/lib/resource.h>

#include <google/protobuf/util/message_differencer.h>

#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/protobuf/util/pb_io.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>

#include <util/stream/file.h>

using namespace NMarket::NAccessAdapter;

struct TResourceUpdaterTestSuite: ::testing::Test {
    TResources Resources;

    TResources AddResource(const TString& name, const NMarket::NAccessAgent::TInstalledVersion& newVersion) const {
        auto resourcesNew = Resources;
        const auto& version = newVersion.spec().version().number();

        auto& resources = *resourcesNew.mutable_resources();
        auto& versions = *resources[name].mutable_versions();

        auto& resourceVersion = versions[version];
        resourceVersion.set_name(name);
        resourceVersion.set_path(newVersion.install_path());
        resourceVersion.set_version(version);

        return resourcesNew;
    }

    TResources RemoveResource(const TString& name, const TString& versionNumber) const {
        auto resourcesNew = Resources;
        auto resources = resourcesNew.mutable_resources();
        if (resources->find(name) == resources->end()) {
            return resourcesNew;
        }

        auto versions = (*resources)[name].mutable_versions();
        versions->erase(versionNumber);

        if (versions->empty()) {
            resources->erase(name);
        }

        return resourcesNew;
    }

    TString ResourcesAsJson(const TResources& versions) const {
        return NProtobufJson::Proto2Json(versions, NProtobufJson::TProto2JsonConfig().SetMapAsObject(true));
    }

    TString ResourcesAsJson() const {
        return ResourcesAsJson(Resources);
    }

    TString ResourcesAsProto(const TResources& versions) const {
        return NProtoBuf::SerializeToBase64String(versions);
    }

    TString ResourcesAsProto() const {
        return ResourcesAsProto(Resources);
    }

    static TString ToolToRun(const TString args = "", const TString& outFilename = "out.txt") {
        TStringStream cmd;
        cmd << BinaryPath("market/access/adapter/ut/tool_to_run/tool_to_run")
            << ' ' << args
            << " > " << outFilename;
        return cmd.Str();
    }

};

THashMap<TString, TString> ParseOut(const TString& filename = "out.txt") {
    THashMap<TString, TString> ret;

    TFileInput f(filename);
    TString line;
    while (f.ReadLine(line)) {
        TStringBuf value(line);
        TStringBuf key = value.NextTok(':');
        ret[key] = value;
    }
    return ret;
}

TEST_F(TResourceUpdaterTestSuite, TestLoadUpdateCmd) {
    TEnv env;
    env.UpdateCmd = ToolToRun();

    TResource r("r", env);
    NMarket::NAccessAgent::TInstalledVersion ver;
    ver.mutable_spec()->mutable_version()->set_number("1");
    ver.mutable_spec()->mutable_version()->set_resource_name("r");
    ver.set_install_path("path/to/r");

    Resources = AddResource("r", ver);
    auto expectedResourcesJson = ResourcesAsJson();
    auto expectedResourcesProto = ResourcesAsProto();

    r.Load(ver);
    auto out = ParseOut();
    EXPECT_EQ(NEnvVal::ACCESS_RESOURCE_ACTION_LOAD, out[NEnvKey::ACCESS_RESOURCE_ACTION]);
    EXPECT_EQ("r", out[NEnvKey::ACCESS_RESOURCE_NAME]);
    EXPECT_EQ("path/to/r", out[NEnvKey::ACCESS_RESOURCE_PATH]);
    EXPECT_EQ(expectedResourcesJson, out[NEnvKey::ACCESS_RESOURCES_JSON]);
    EXPECT_EQ(expectedResourcesProto, out[NEnvKey::ACCESS_RESOURCES_PROTO]);

    EXPECT_TRUE(google::protobuf::util::MessageDifferencer::Equals(Resources, env.Resources));
}

TEST_F(TResourceUpdaterTestSuite, TestResourceUpdateFail) {
    TEnv env;
    env.UpdateCmd = ToolToRun("--retcode 100");

    TResource r("r", env);
    NMarket::NAccessAgent::TInstalledVersion ver;
    ver.mutable_spec()->mutable_version()->set_number("1");

    auto newVersions = AddResource("r", ver);
    // old versions
    auto expectedResourcesJson = ResourcesAsJson(newVersions);
    auto expectedResourcesProto = ResourcesAsProto(newVersions);

    EXPECT_THROW(r.Load(ver), yexception);

    auto out = ParseOut();
    EXPECT_EQ(expectedResourcesJson, out[NEnvKey::ACCESS_RESOURCES_JSON]);
    EXPECT_EQ(expectedResourcesProto, out[NEnvKey::ACCESS_RESOURCES_PROTO]);

    // Check that versions in env not updated
    EXPECT_TRUE(google::protobuf::util::MessageDifferencer::Equals(Resources, env.Resources));
}

TEST_F(TResourceUpdaterTestSuite, TestOtherErrorFail) {
    TEnv env;
    env.UpdateCmd = ToolToRun("--retcode 101 --failcount 3"); // Cmd will fail 3 times and then returns 0 retcode

    TResource r("r", env);
    NMarket::NAccessAgent::TInstalledVersion ver;
    ver.mutable_spec()->mutable_version()->set_number("1");

    Resources = AddResource("r", ver);
    auto expectedResourcesJson = ResourcesAsJson();
    auto expectedResourcesProto = ResourcesAsProto();

    r.Load(ver);
    auto out = ParseOut();
    EXPECT_EQ("3", out["fail_count"]);
    EXPECT_EQ(expectedResourcesJson, out[NEnvKey::ACCESS_RESOURCES_JSON]);
    EXPECT_EQ(expectedResourcesProto, out[NEnvKey::ACCESS_RESOURCES_PROTO]);

    // Check that versions in env is updated
    EXPECT_TRUE(google::protobuf::util::MessageDifferencer::Equals(Resources, env.Resources));
}

TEST_F(TResourceUpdaterTestSuite, TestUnloadUpdateCmd) {
    TEnv env;
    env.UpdateCmd = ToolToRun();

    TResource r("r", env);

    Resources = RemoveResource("r", "1");
    auto expectedResourcesJson = ResourcesAsJson();
    auto expectedResourcesProto = ResourcesAsProto();

    r.Unload("1");
    auto out = ParseOut();
    EXPECT_EQ(NEnvVal::ACCESS_RESOURCE_ACTION_UNLOAD, out[NEnvKey::ACCESS_RESOURCE_ACTION]);
    EXPECT_EQ("r", out[NEnvKey::ACCESS_RESOURCE_NAME]);
    EXPECT_EQ("", out[NEnvKey::ACCESS_RESOURCE_PATH]);
    EXPECT_EQ(expectedResourcesJson, out[NEnvKey::ACCESS_RESOURCES_JSON]);
    EXPECT_EQ(expectedResourcesProto, out[NEnvKey::ACCESS_RESOURCES_PROTO]);

    EXPECT_TRUE(google::protobuf::util::MessageDifferencer::Equals(Resources, env.Resources));
}
