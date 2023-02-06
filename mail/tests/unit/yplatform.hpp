#ifndef _YIMAP_TESTS_YPLATFORM_H_
#define _YIMAP_TESTS_YPLATFORM_H_

#include <common/imap_context.h>

#include <yplatform/find.h>
#include <yplatform/log.h>
#include <yplatform/task_context.h>
#include <yplatform/loader.h>

#include <stdlib.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>
#include <thread>
#include <fstream>

struct TestContext : public yimap::Context
{
    TestContext()
    {
    }
    ~TestContext()
    {
    }
    virtual const std::string& get_name() const
    {
        static const std::string NAME = "test_imap_session";
        return NAME;
    }
};

extern bool hasYplatform();

inline void startYplatform(const std::string& configPath)
{
    if (hasYplatform())
    {
        yplatform_start(configPath);
    }
}

inline void stopYplatform()
{
    if (hasYplatform())
    {
        auto pid = getpid();
        kill(pid, SIGTERM);
    }
}

inline std::string readFile(const std::string& fname)
{
    std::ifstream ifs(fname);
    return std::string(std::istreambuf_iterator<char>(ifs), std::istreambuf_iterator<char>());
}

#endif // _YIMAP_TESTS_YPLATFORM_H_
