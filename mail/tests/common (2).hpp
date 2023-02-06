#ifndef _YMOD_IMAPCLIENT_TESTS_COMMON_H_
#define _YMOD_IMAPCLIENT_TESTS_COMMON_H_

#include <yplatform/find.h>
#include <ymod_imapclient/call.h>
#include <ymod_imapclient/imap_client.h>
#include <yplatform/log.h>
#include <yplatform/loader.h>

#include <stdlib.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>

using namespace ymod_imap_client;

extern const string prefix;

struct TestContext : public yplatform::task_context
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

inline void startYplatform(const std::string& configPath)
{
    yplatform_start(configPath);
}

inline void stopYplatform()
{
    auto pid = getpid();
    kill(pid, SIGTERM);
}

#endif // _YMOD_IMAPCLIENT_TESTS_COMMON_H_
