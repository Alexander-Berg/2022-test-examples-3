#include <yplatform/net/dns/resolver.h>
#include <yplatform/net/client_session_strand.h>

using boost::system::error_code;

int main()
{
    return 0;

    // net
    {
        using namespace yplatform::net;

        // client session strand
        io_data fake_io_data;
        client_session_strand<> session(&fake_io_data, client_settings());
        session.connect(
            "localhost", 80, [](const error_code&) {}, []() {});
        session.connect(
            "localhost",
            80,
            boost::bind<void>([](const error_code&) {}, _1),
            boost::bind<void>([]() {}));
        session.connect(
            "localhost",
            80,
            std::bind([](const error_code&) {}, std::placeholders::_1),
            std::bind([]() {}));

        struct handler_with_error_code
        {
            void operator()(const error_code&) const
            {
            }
        };
        struct simple_handler
        {
            void operator()() const
            {
            }
        };
        session.connect("localhost", 80, handler_with_error_code(), simple_handler());
    }

    return 0;
}
