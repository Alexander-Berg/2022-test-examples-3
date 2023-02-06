#include <mail/notsolitesrv/src/config/firstline.h>
#include <mail/notsolitesrv/src/errors.h>
#include <mail/notsolitesrv/src/firstline/firstline.h>
#include <mail/notsolitesrv/src/firstline/types/request.h>
#include <mail/notsolitesrv/src/firstline/types/response.h>
#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/firstline.h>
#include <mail/notsolitesrv/tests/unit/util/firstline.h>

#include <boost/asio.hpp>

#include <gtest/gtest.h>

namespace {

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NFirstline;

struct TTestFirstline : Test {
    TFirstlineRequest MakeRequest() {
        return {
            .IsHtml = true,
            .Part = "part",
            .Subject = "subject",
            .From = {
                .Local = "local",
                .Domain = "domain",
                .DisplayName = "display_name" 
            },
            .IsPeopleType = true
        };
    }

    void GenerateFirstline(TFirstlineRequest request, TFirstlineCallback callback) {
        Firstline->Firstline(GetContext(), request, callback, Io);
        Io.run();
    }

    const std::shared_ptr<StrictMock<NImpl::TFirstlineImplMock>> FirstlineImpl
        = std::make_shared<StrictMock<NImpl::TFirstlineImplMock>>();
    const std::shared_ptr<TFirstline> Firstline
        = std::make_shared<StrictMock<TFirstline>>(FirstlineImpl, Io);
    boost::asio::io_context Io;
};

TEST_F(TTestFirstline, for_successful_generate_firstline_should_return_response) {
    const InSequence s;
    auto firstline = "firstline";
    auto request = MakeRequest();
    auto response = TFirstlineResponse{.Firstline = firstline};

    EXPECT_CALL(*FirstlineImpl, GenerateFirstline(request))
        .WillOnce(Return(firstline));
    GenerateFirstline(request, [=](auto ec, auto resp) {
        ASSERT_FALSE(ec);
        EXPECT_EQ(response, resp);
    });
}

TEST_F(TTestFirstline, for_unsuccessful_generate_firstline_should_return_error) {
    const InSequence s;
    auto firstline = "firstline";
    auto request = MakeRequest();
    auto response = TFirstlineResponse{.Firstline = firstline};

    EXPECT_CALL(*FirstlineImpl, GenerateFirstline(request))
        .WillOnce(Throw(std::exception{}));
    GenerateFirstline(request, [=](auto ec, auto) {
        ASSERT_TRUE(ec);
        EXPECT_EQ(ec, EError::FirstlineError);
    });
}

} // namespace anonymous
