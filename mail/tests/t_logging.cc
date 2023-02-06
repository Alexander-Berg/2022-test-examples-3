#include <yplatform/log.h>
#include <yplatform/log/typed.h>
#include <yplatform/log/detail/spd.h>
#include <spdlog/common.h>
#include <catch.hpp>
#include <boost/filesystem.hpp>
#include <stdexcept>
#include <iostream>
#include <thread>
#include <fstream>

using ptree = yplatform::ptree;
using spdlog::level::level_enum;
using yplatform::log::detail::spd::level_from_str;

struct tmp_dir
{
    tmp_dir()
    {
        path = create_tmp_dir();
    }
    ~tmp_dir()
    {
        remove_tmp_dir(path);
    }

    const std::string& get() const
    {
        return path.native();
    }

private:
    boost::filesystem::path path;

    static boost::filesystem::path create_tmp_dir()
    {
        auto path = boost::filesystem::temp_directory_path();
        path /= boost::filesystem::unique_path();
        boost::filesystem::create_directories(path);
        return path;
    }

    static void remove_tmp_dir(const boost::filesystem::path& path)
    {
        boost::filesystem::remove_all(path);
    }
};

TEST_CASE("log/global/init_with_empty_ptree", "")
{
    yplatform::ptree conf;
    REQUIRE_NOTHROW(yplatform::log::log_init(conf));
}

TEST_CASE("log/level_from_str/names_map_to_correct_levels")
{
    CHECK(level_from_str("trace") == level_enum::trace);
    CHECK(level_from_str("debug") == level_enum::debug);
    CHECK(level_from_str("info") == level_enum::info);
    CHECK(level_from_str("notice") == level_enum::notice);
    CHECK(level_from_str("warning") == level_enum::warn);
    CHECK(level_from_str("error") == level_enum::err);
    CHECK(level_from_str("critical") == level_enum::critical);
    CHECK(level_from_str("alert") == level_enum::alert);
    CHECK(level_from_str("emerg") == level_enum::emerg);
    CHECK(level_from_str("off") == level_enum::off);
}

TEST_CASE("log/level_from_str/exceptional_cases/aliases_recognized_by_spdlog_upstream")
{
    CHECK_THROWS_AS(level_from_str("warn"), std::logic_error);
    CHECK_THROWS_AS(level_from_str("err"), std::logic_error);
}

TEST_CASE("log/level_from_str/exceptional_cases/unknown_names")
{
    CHECK_THROWS_AS(level_from_str(""), std::logic_error);
    CHECK_THROWS_AS(level_from_str("null"), std::logic_error);
}

struct T_logger
{
    using log_lines = std::vector<std::string>;

    ::tmp_dir tmp_dir;
    std::string filename;
    std::string filename2;
    std::shared_ptr<yplatform::log::detail::service> log_service;

    T_logger()
    {
        filename = tmp_dir.get() + "/test.log";
        filename2 = tmp_dir.get() + "/test2.log";
        log_service = std::make_shared<yplatform::log::detail::service>();
    }

    ~T_logger()
    {
    }

    ptree create_async_file_cfg(
        const std::string& filename,
        const std::size_t queue_size,
        const std::string& level)
    {
        ptree conf;
        conf.put("async", true);
        conf.put("level", level);
        conf.put("format", "%v");
        conf.put("queue_size", queue_size);
        conf.put("overflow_policy", "discard_log_msg");
        ptree sink;
        sink.put("type", "reopenable_file");
        sink.put("path", filename);
        sink.put("force_flush", false);
        conf.add_child("sinks", sink);
        return conf;
    }

    ptree create_sync_file_cfg(const std::string& filename, const std::string& level)
    {
        ptree conf;
        conf.put("async", false);
        conf.put("level", level);
        conf.put("format", "%v");
        ptree sink;
        sink.put("type", "reopenable_file");
        sink.put("path", filename);
        sink.put("force_flush", true);
        conf.add_child("sinks", sink);
        return conf;
    }

    void create_async_file_logger(
        const std::string& name,
        const std::string& filename,
        const std::size_t queue_size,
        const std::string& level)
    {
        ptree root;
        root.add_child(name, create_async_file_cfg(filename, queue_size, level));
        log_service = std::make_shared<yplatform::log::detail::service>(root);
    }

    void create_sync_file_logger(
        const std::string& name,
        const std::string& filename,
        const std::string& level)
    {
        ptree root;
        root.add_child(name, create_sync_file_cfg(filename, level));
        log_service = std::make_shared<yplatform::log::detail::service>(root);
    }

    void create_sync_file_loggers(
        const std::vector<std::pair<std::string, std::string>> names_filenames,
        const std::string& level)
    {
        ptree root;
        for (auto& pair : names_filenames)
            root.add_child(pair.first, create_sync_file_cfg(pair.second, level));
        log_service = std::make_shared<yplatform::log::detail::service>(root);
    }

    void update_sync_file_logger_level(
        const std::string& name,
        const std::string& filename,
        const std::string& level)
    {
        ptree root;
        root.add_child(name, create_sync_file_cfg(filename, level));
        log_service->update_log_levels_from(root);
    }

    log_lines load_log_lines(const std::string& filename)
    {
        log_lines result;
        std::ifstream logfile(filename);
        for (std::string line; std::getline(logfile, line);)
        {
            result.push_back(line);
        }
        return result;
    }
};

TEST_CASE_METHOD(T_logger, "log/init_with_file_and_write_a_line_gives_a_file_with_this_line", "")
{
    create_sync_file_logger("global", filename, "debug");
    auto logger = yplatform::log::source(*log_service, "global");
    YLOG(logger, debug) << "test";
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "test");
}

TEST_CASE_METHOD(T_logger, "log/unknown_logger_writes_to_global", "")
{
    create_sync_file_logger("global", filename, "debug");
    auto logger = yplatform::log::source(*log_service, "unknown");
    YLOG(logger, debug) << "test";
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "test");
}

TEST_CASE_METHOD(T_logger, "log/log_levels", "")
{
    create_sync_file_logger("global", filename, "warning");
    auto logger = yplatform::log::source(*log_service, "global");
    YLOG(logger, debug) << "test";
    YLOG(logger, info) << "test";
    YLOG(logger, warning) << "test";
    YLOG(logger, error) << "test";
    YLOG(logger, emerg) << "test";
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 3);
}

TEST_CASE_METHOD(T_logger, "log/update_log_levels", "")
{
    create_sync_file_logger("global", filename, "info");
    auto logger = yplatform::log::source(*log_service, "global");
    REQUIRE_FALSE(logger.should_log(yplatform::log::severity_level::debug));
    update_sync_file_logger_level("global", filename, "debug");
    REQUIRE(logger.should_log(yplatform::log::severity_level::debug));
}

TEST_CASE_METHOD(T_logger, "log/multithread_test", "")
{
    create_sync_file_logger("global", filename, "debug");
    auto logger = yplatform::log::source(*log_service, "global");
    auto thread_func = [&logger]() {
        for (auto i = 0; i < 100; ++i)
            YLOG(logger, debug) << "test";
    };
    std::vector<std::thread> threads;
    for (auto i = 0; i < 10; ++i)
        threads.push_back(std::thread(thread_func));
    for (auto i = 0; i < 10; ++i)
        threads[i].join();
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1000);
}

TEST_CASE_METHOD(T_logger, "log/two_loggers_write_to_different_files", "")
{
    create_sync_file_loggers({ { "a", filename }, { "b", filename2 } }, "debug");
    auto a = yplatform::log::source(*log_service, "a");
    auto b = yplatform::log::source(*log_service, "b");

    YLOG(a, debug) << "test";
    YLOG(b, debug) << "test";
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "test");
    auto log_lines2 = load_log_lines(filename2);
    REQUIRE(log_lines2.size() == 1);
    REQUIRE(log_lines2[0] == "test");
}

TEST_CASE_METHOD(T_logger, "log/tskv/logger_writes_correct_attributes", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    YLOG(logger, debug) << typed::make_attr("key1", "value1") << typed::make_attr("key2", "value2");
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=value1\tkey2=value2");
}

TEST_CASE_METHOD(T_logger, "log/tskv/tabs_and_crlns_are_erased", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    YLOG(logger, debug) << typed::make_attr("key\t1", "valu\re1")
                        << typed::make_attr("k\rey2", "val\nue\t2");
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=value1\tkey2=value2");
}

TEST_CASE_METHOD(T_logger, "log/tskv/empty_keys_are_ignored", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    YLOG(logger, debug) << typed::make_attr("", "abc") << typed::make_attr("", "")
                        << typed::make_attr("key1", "") << typed::make_attr("key2", "value2");
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=\tkey2=value2");
}

TEST_CASE_METHOD(T_logger, "log/tskv/write_integral", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    YLOG(logger, debug) << typed::make_attr("key1", 0.3f) << typed::make_attr("key2", 5);
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=0.3\tkey2=5");
}

TEST_CASE_METHOD(T_logger, "log/tskv/write_map", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    typed::attributes_map am;
    am << typed::make_attr("key1", "value1") << typed::make_attr("key2", "value2");
    YLOG(logger, debug) << am << typed::make_attr("key3", "value3");
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=value1\tkey2=value2\tkey3=value3");
}

TEST_CASE_METHOD(T_logger, "log/tskv/control_characters_are_erased_for_writing_from_map", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    typed::attributes_map am;
    am << typed::make_attr("key\n1", "\tvalue1") << typed::make_attr("key\r2", "value2\n");
    YLOG(logger, debug) << am;
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=value1\tkey2=value2");
}

TEST_CASE_METHOD(T_logger, "log/tskv/write_bool", "")
{
    create_sync_file_logger("global", filename, "debug");
    namespace typed = yplatform::log::typed;
    auto logger = yplatform::log::typed::logger(*log_service, "global");
    YLOG(logger, debug) << typed::make_attr("key1", true) << typed::make_attr("key2", false);
    auto log_lines = load_log_lines(filename);
    REQUIRE(log_lines.size() == 1);
    REQUIRE(log_lines[0] == "key1=1\tkey2=0");
}
