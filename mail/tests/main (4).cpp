#include <gtest/gtest.h>
#include <yplatform/application.h>

class YplatformEnvironment : public ::testing::Environment {
public:
    explicit YplatformEnvironment(const std::string& configPath)
        : configPath(configPath)
        , app(nullptr) {}

    void SetUp() override {
        yplatform::configuration config;
        config.load_from_file(configPath);
        app = std::make_unique<yplatform::application>(config);
        app->run();
    }

    void TearDown() override {
        if (app) {
            app->stop(SIGTERM);
            app.reset();
        }
    }
private:
    std::string configPath;
    std::unique_ptr<yplatform::application> app;
};

int main(int argc, char* argv[]) {
    ::testing::InitGoogleTest(&argc, argv);
    ::testing::AddGlobalTestEnvironment(new YplatformEnvironment("test-conf.yml"));
    return RUN_ALL_TESTS();
}
