#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/corgi/include/types_error.h>
#include <mail/webmail/corgi/include/resolve/directory.h>
#include <mail/http_getter/client/mock/mock.h>
#include <yplatform/reactor.h>


using namespace ::testing;
using namespace std::string_literals;

namespace corgi::tests {

std::ostream& operator<<(std::ostream& out, const std::optional<DepartmentId>& id) {
    if (id) {
        out << id->t;
    } else {
        out << "-";
    }
    return out;
}

std::ostream& operator<<(std::ostream& out, const GroupId& id) {
    out << id.t;
    return out;
}

std::ostream& operator<<(std::ostream& out, const std::optional<GroupsSet>& ss) {
    if (ss) {
        std::for_each(ss->begin(), ss->end(), [&] (auto&& s) { out << s; });
    } else {
        out << "-";
    }
    return out;
}

using boost::fusion::operators::operator<<;

namespace tests {

const Uid UID(15);

MATCHER(WithNextLink, "") {
    return arg.request.url.find("next") != std::string::npos;
}

MATCHER(WithUid, "") {
    return arg.request.url.find("/"s + std::to_string(UID.t)) != std::string::npos;
}

std::string singleUser(Uid uid, bool admin, const std::string& depId, const std::string& groupId) {
    const std::string temp = R"(
{{  "department_id": {depId},
    "id": {uid},
    "groups": [ {{ "id": {groupId} }} ],
    "is_admin": {admin}
}})";
    return fmt::format(
        temp, fmt::arg("uid", uid.t), fmt::arg("admin", admin ? "true" : "false"),
        fmt::arg("depId", depId), fmt::arg("groupId", groupId)
    );
}

yhttp::response departments() {
    const std::string json = R"(
{
    "page": 1,
    "per_page": 20,
    "pages": 1,
    "total": 8,
    "links": {},
    "result": [{
        "id": 1,
        "parent": null
    }, {
        "id": 2,
        "parent": {
            "id": 1
        }
    }, {
        "id": 3,
        "parent": {
            "id": 1
        }
    }, {
        "id": 4,
        "parent": {
            "id": 3
        }
    }, {
        "id": 5,
        "parent": {
            "id": 3
        }
    }, {
        "id": 6,
        "parent": {
            "id": 1
        }
    }, {
        "id": 7,
        "parent": {
            "id": 6
        }
    }, {
        "id": 8,
        "parent": {
            "id": 1
        }
    }]
}
)";

    return yhttp::response { .status=200, .body=json };
}

yhttp::response singleUserResponse(Uid uid, bool admin, const std::string& depId, const std::string& groupId) {
    return yhttp::response { .status=200, .body=singleUser(uid, admin, depId, groupId) };
}

std::string users(std::string link, const std::vector<std::string>& users) {
    if (!link.empty()) {
        link = R"( "next" : ")"s + link + '"';
    }

    std::ostringstream str;
    str << R"( { "links": { )"s   << link
        << R"( }, "result": [ )"s << boost::algorithm::join(users, ", ")
        << " ] }"s;

    return str.str();
}

yhttp::response usersResponse(const std::string& link, const std::vector<std::string>& resps) {
    return yhttp::response { .status=200, .body=users(link, resps) };
}

struct DirectoryBaseTest: public Test {
    std::shared_ptr<yplatform::reactor> reactor;
    ResolverConfig config;
    http_getter::ResponseSequencePtr responses;
    http_getter::TypedClientPtr getter;
    OrgId orgId = OrgId(1);

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);

        responses = std::make_shared<StrictMock<http_getter::ResponseSequence>>();

        const auto ep = http_getter::TypedEndpoint::fromData("/{uid}", "", http_getter::Executor());

        config.directorySingleUser = ep;
        config.directoryDepartments = ep;
        config.directoryUsers = ep;

        getter = http_getter::createTypedDummyWithRequest(responses);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

struct DepartmentsTest: public DirectoryBaseTest { };

TEST_F(DepartmentsTest, shouldParseDepartments) {
    EXPECT_CALL(*responses, get(_))
        .WillOnce(Return(departments()))
    ;

    const DepartmentsTree tree {
        { DepartmentId(1), Departments{ DepartmentId(2), DepartmentId(3), DepartmentId(6), DepartmentId(8) } },
        { DepartmentId(3), Departments{ DepartmentId(4), DepartmentId(5) } },
        { DepartmentId(6), Departments{ DepartmentId(7) } }
    };

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(resolveDepartmentsTree(orgId, *getter, config, yield).value(), tree);
    });
}

struct IsAdmin: public DirectoryBaseTest { };

TEST_F(IsAdmin, shouldCheckIfUidIsAdmin) {
    EXPECT_CALL(*responses, get(WithUid()))
        .WillOnce(Return(singleUserResponse(Uid(1), true, "1", "2")))
        .WillOnce(Return(singleUserResponse(Uid(2), false, "1", "2")))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        CommonParams common{.adminUid=UID, .orgId=orgId};
        EXPECT_TRUE(isAdmin(common, *getter, config, yield).value());
        EXPECT_FALSE(isAdmin(common, *getter, config, yield).value());
    });
}

struct ResolveOrgIdTest: public DirectoryBaseTest { };

TEST_F(ResolveOrgIdTest, shouldFollowNextLink) {
    const std::string departmentIdStr = "1";
    const std::string groupIdStr = "2";
    const auto departmentId = std::make_optional(DepartmentId(std::stoll(departmentIdStr)));
    const auto groupsId = GroupsSet{GroupId(std::stoll(groupIdStr))};

    DirectoryUser user1{Uid(1), false, departmentId, groupsId};
    DirectoryUser user2{Uid(2), false, departmentId, groupsId};

    EXPECT_CALL(*responses, get(Not(WithNextLink())))
        .WillOnce(Return(usersResponse("next", {singleUser(user1.uid, user1.admin, departmentIdStr, groupIdStr)})))
        .WillOnce(Return(usersResponse("", {singleUser(user2.uid, user2.admin, departmentIdStr, groupIdStr)})))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_THAT(resolveOrgId(orgId, *getter, config, yield).value(),
                    UnorderedElementsAre(std::make_pair(user1.uid, user1), std::make_pair(user2.uid, user2)));
    });
}

TEST_F(ResolveOrgIdTest, shouldReturnError) {
    EXPECT_CALL(*responses, get(_))
        .WillOnce(Return(yhttp::response { .status=500, .body="" }))
    ;

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(resolveOrgId(orgId, *getter, config, yield).error(),
                  make_error(RemoteServiceError::directory));
    });
}

}
}
