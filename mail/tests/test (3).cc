#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/ymod_switchbox/include/impl/impl.h>

using namespace ::testing;
namespace ymod_switchbox::tests {

inline yplatform::ptree makeTestHandler(std::string strategy, std::string name, std::string path) {
    yplatform::ptree tree;

    tree.add("strategy", strategy);
    tree.add("name", name);
    tree.add("path", path);

    return tree;
}

struct Fake : public FileAndHttp {
    void fakeInitStrategyHandlers(const yplatform::ptree& cfg) {
        initStrategyHandlers(cfg);
    }

    bool fakeChangeValueForStrategy(bool newValue, const std::string& name, Strategy strategy) {
        return changeValueForStrategy(newValue, name, strategy);
    }
};

const yplatform::ptree http_handler = makeTestHandler("http", "module_ready", "module/ready");
const yplatform::ptree file_handler = makeTestHandler("file", "module_ready", "/var/tmp/module_ready.txt");

TEST_F(Test, shouldGetFalseValueByDefault) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);
    tree.add_child("handlers", file_handler);

    Fake fake;
    fake.fakeInitStrategyHandlers(tree);

    EXPECT_EQ(fake.getValue("module_ready"), false);
}

TEST_F(Test, shouldGetTrueValueIfOneOfTheHandlersHaveTrueValue) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);
    tree.add_child("handlers", file_handler);

    Fake fake;
    fake.fakeInitStrategyHandlers(tree);
    fake.fakeChangeValueForStrategy(true, "module_ready", Strategy::http);

    EXPECT_EQ(fake.getValue("module_ready"), true);
}

TEST_F(Test, shouldRaiseExceptionWhenEntityTryToGetNonExistentValue) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);

    Fake fake;
    fake.fakeInitStrategyHandlers(tree);
    EXPECT_THROW(fake.getValue("fake"), std::runtime_error);
}

TEST_F(Test, shouldRaiseExceptionWhenConfigHaveTwoHandlersWithSameStrategyForSameName) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);
    tree.add_child("handlers", http_handler);

    Fake fake;
    EXPECT_THROW(fake.fakeInitStrategyHandlers(tree), std::runtime_error);
}

TEST_F(Test, shouldGetTrueWhenChangingExistingValue) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);

    Fake fake;
    fake.fakeInitStrategyHandlers(tree);
    EXPECT_EQ(fake.fakeChangeValueForStrategy(true, "module_ready", Strategy::http), true);
}

TEST_F(Test, shouldGetFalseWhenChangingNonExistingValue) {
    yplatform::ptree tree;

    tree.add_child("handlers", http_handler);

    Fake fake;
    fake.fakeInitStrategyHandlers(tree);
    EXPECT_EQ(fake.fakeChangeValueForStrategy(true, "fake", Strategy::http), false);
}

}
