#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/service/include/handlers/organization.h>
#include <mail/spaniel/service/include/handlers/user.h>
#include <mail/spaniel/service/include/task_params.h>
#include <mail/spaniel/tests/unit/include/matchers.h>
#include <mail/spaniel/tests/unit/include/sequence_response.h>
#include <mail/ymod_queuedb_worker/tests/mock_queue.h>
#include <mail/spaniel/ymod_db/tests/mock_repository.h>
#include <mail/http_getter/client/mock/mock.h>
#include <mail/ymod_queuedb_worker/include/task_control.h>


using namespace ::testing;
namespace spaniel::tests {

const Uid adminUid(1000);
const Uid noPddUid(1001);
const Uid pddUid(1002);
const Uid justUid(1003);

const http_getter::TypedEndpoint blackbox;
const http_getter::TypedEndpoint directory;
const http_getter::TypedEndpoint departments;
const http_getter::TypedEndpoint settings;
const http_getter::TypedEndpoint mopsPurge;
const http_getter::TypedEndpoint mopsCreate;
const http_getter::TypedEndpoint enableUser;

const OrganizationParams common { .orgId=OrgId(2), .requestId="requestId" };

const std::string adminUidBbResponse = (boost::format(R"(
{ "users": [{
    "uid": { "value": "%1%", "hosted": true }
}] }
)") % adminUid.t).str();

const std::string adminAndPddUidBbResponse = (boost::format(R"(
{ "users": [
    { "uid": { "value": "%1%", "hosted": true } },
    { "uid": { "value": "%2%", "hosted": true } }
  ]
})") % adminUid.t % pddUid.t).str();

const std::string noPddUidBbResponse = (boost::format(R"(
{ "users": [{
    "uid": { "value": "%1%", "hosted": false }
}] }
)") % noPddUid.t).str();

struct WithSpawn {
    std::shared_ptr<yplatform::reactor> reactor;

    void SetUp() {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

struct WithHttpSequence {
    http_getter::ResponseSequencePtr httpSequence;

    void SetUp() {
        httpSequence = std::make_shared<http_getter::ResponseSequence>();
    }
};

struct OrganizationUpdateHandlerTest: public WithSpawn, public WithHttpSequence, public Test {
    WorkerConfigPtr cfg;
    std::shared_ptr<StrictMock<MockRepository>> repo;
    std::shared_ptr<StrictMock<ymod_queuedb::MockQueue>> queuedb;

    void SetUp() override {
        WithSpawn::SetUp();
        WithHttpSequence::SetUp();

        repo = std::make_shared<StrictMock<MockRepository>>();
        queuedb = std::make_shared<StrictMock<ymod_queuedb::MockQueue>>();
        cfg = std::make_shared<WorkerConfig>(WorkerConfig {
            .resolverConfig = corgi::ResolverConfig {
                .blackbox = blackbox,
                .directoryUsers = directory,
                .directoryDepartments = departments,
            },
            .repo = repo,
            .queuedb = queuedb,
        });
    }

    auto call(boost::asio::yield_context yield) const {        
        return organizationUpdate(
            common,
            OrganizationResolverWithoutAdminUid(cfg->resolverConfig, http_getter::createTypedDummy(httpSequence), common),
            cfg, boost::make_shared<yplatform::task_context>(), yield
        );
    }
};

TEST_F(OrganizationUpdateHandlerTest, shouldNotActWithNonActiveOrganization) {
    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::disabled }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<void>());
    });
}

TEST_F(OrganizationUpdateHandlerTest, shouldNotActWithMissingOrganization) {
    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(boost::none));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<void>());
    });
}

TEST_F(OrganizationUpdateHandlerTest, shouldNotActWithOrganizationWithoutNewUsers) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::active }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=(boost::format(R"(
                { "result": [{
                    "department_id": null, "id": %1%, "groups": [], "is_admin": true
                }, {
                    "department_id": null, "id": %2%, "groups": [], "is_admin": false
                }], "links": {} }
            )") % adminUid.t % pddUid.t).str()
        }))
        .WillOnce(Return(yhttp::response { .status=200, .body=(boost::format(R"(
                { "users": [{
                    "uid": { "value": "%1%", "hosted": true }
                }, {
                    "uid": { "value": "%2%", "hosted": true }
                }] }
            )") % adminUid.t % pddUid.t).str()
        }))
        .WillOnce(Return(yhttp::response { .status=200, .body=R"({ "result": [], "links": {} })"}));

    EXPECT_CALL(*repo, asyncOrganizationUids(common, _))
        .WillOnce(WithArg<1>(
            Invoke([=] (OnOrganizationUids cb) {
                responseAsSequence(std::vector<Uid> {adminUid, pddUid}, cb);
            })
        ));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<void>());
    });
}

TEST_F(OrganizationUpdateHandlerTest, shouldSetTaskInCaseOfNewUsersInOgranization) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::active }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=(boost::format(R"(
                { "result": [
                    { "department_id": null, "id": %1%, "groups": [], "is_admin": true },
                    { "department_id": null, "id": %2%, "groups": [], "is_admin": false }
                ], "links": {} }
            )") % adminUid.t % pddUid.t).str()
        }))
        .WillOnce(Return(yhttp::response { .status=200, .body=adminAndPddUidBbResponse }))
        .WillOnce(Return(yhttp::response { .status=200, .body=R"({ "result": [], "links": {} })"}));

    EXPECT_CALL(*repo, asyncOrganizationUids(common, _))
        .WillOnce(WithArg<1>(
            Invoke([=] (OnOrganizationUids cb) {
                responseAsSequence(std::vector<Uid> {noPddUid, pddUid}, cb);
            })
        ));

    OrganizationUpdateParams expected { .all = {adminUid, pddUid}, .newbie={adminUid} };
    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationUpdateType(), UpdateTaskArgsAre(expected), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<void>());
    });
}

struct OrganizationUpdateTaskTest: public WithSpawn, public WithHttpSequence, public Test {
    WorkerConfigPtr cfg;
    std::shared_ptr<StrictMock<MockRepository>> repo;

    void SetUp() override {
        WithSpawn::SetUp();
        WithHttpSequence::SetUp();

        repo = std::make_shared<StrictMock<MockRepository>>();
        cfg = std::make_shared<WorkerConfig>(WorkerConfig {
            .enableUser = enableUser,
            .repo = repo,
        });
    }

    auto call(const std::vector<Uid>& newbie, const std::vector<Uid>& all, boost::asio::yield_context yield) const {
        return organizationUpdate(
            common, OrganizationUpdateParams{ .all=all, .newbie=newbie },
            WorkerRequestContext(http_getter::createTypedDummyWithRequest(httpSequence), common),
            cfg, boost::make_shared<yplatform::task_context>(), yield
        );
    }
};

TEST_F(OrganizationUpdateTaskTest, shouldNotActWithDisabledOrganization) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::disabled }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call({}, {}, yield), yamail::expected<void>());
    });
}

TEST_F(OrganizationUpdateTaskTest, shouldEnableAllUsersAndUpdateState) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::active }));

    EXPECT_CALL(*httpSequence, get(WithUidInUrl(adminUid)))
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get(WithUidInUrl(noPddUid)))
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get(WithUidInUrl(pddUid)))
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*repo, asyncUpdateOrganizationUids(_, UnorderedElementsAre(adminUid, noPddUid, pddUid, justUid), _))
        .WillOnce(InvokeArgument<2>(1));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call({adminUid, noPddUid, pddUid}, {adminUid, noPddUid, pddUid, justUid}, yield), yamail::expected<void>());
    });
}

TEST_F(OrganizationUpdateTaskTest, shouldReturnErrorInCaseOfZeroUpdatedRows) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::active }));

    EXPECT_CALL(*repo, asyncUpdateOrganizationUids(_, _, _))
        .WillOnce(InvokeArgument<2>(0));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(static_cast<bool>(call({}, {}, yield)));
    });
}

TEST_F(OrganizationUpdateTaskTest, shouldReturnErrorIfHandlerDoesNotReturn200Ok) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization { .state=OrganizationState::active }));

    EXPECT_CALL(*httpSequence, get(WithUidInUrl(adminUid)))
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get(WithUidInUrl(noPddUid)))
        .WillOnce(Return(yhttp::response { .status=500, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(static_cast<bool>(call({adminUid, noPddUid}, {adminUid, noPddUid}, yield)));
    });
}

template<bool enable>
struct EnableDisableUserTest: public WithSpawn, public WithHttpSequence, public Test {
    ConfigPtr cfg;

    void SetUp() override {
        WithSpawn::SetUp();
        WithHttpSequence::SetUp();

        cfg = std::make_shared<Config>(Config {
            .resolverConfig = corgi::ResolverConfig {
               .blackbox = blackbox,
            },
            .settings = settings,
            .createHiddenTrash = mopsCreate,
            .purgeHiddenTrash = mopsPurge,
        });
    }

    auto call(boost::asio::yield_context yield) const {
        return userDisableEnable(
            enable, SettingParams{ .uid=adminUid },
            http_getter::createTypedDummy(httpSequence),
            cfg, yield
        );
    }
};

struct EnableUserTest:  public EnableDisableUserTest<true>  { };
struct DisableUserTest: public EnableDisableUserTest<false> { };

TEST_F(EnableUserTest, shouldEnableUser) {
    const InSequence s;

    EXPECT_CALL(*httpSequence, get())
            .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<OrganizationEnableResult>());
    });
}

TEST_F(EnableUserTest, shouldResponseWithErrorIfCaseOfFailedMops) {
    const InSequence s;
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=500, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(call(yield));
    });
}

TEST_F(EnableUserTest, shouldResponseWithErrorIfCaseOfFailedSettings) {
    const InSequence s;
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=500, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(call(yield));
    });
}

TEST_F(EnableUserTest, shouldResponseSuccessIfUidIsNotPdd) {
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=noPddUidBbResponse }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<OrganizationEnableResult>());
    });
}

TEST_F(DisableUserTest, shouldDisableUser) {
    const InSequence s;
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<OrganizationEnableResult>());
    });
}

TEST_F(DisableUserTest, shouldResponseWithErrorIfCaseOfFailedSettings) {
    const InSequence s;
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=500, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(call(yield));
    });
}

TEST_F(DisableUserTest, shouldResponseWithErrorIfCaseOfFailedMops) {
    const InSequence s;
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=adminUidBbResponse }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body="" }));

    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=500, .body="" }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_FALSE(call(yield));
    });
}

TEST_F(DisableUserTest, shouldResponseSuccessIfUidIsNotPdd) {
    EXPECT_CALL(*httpSequence, get())
        .WillOnce(Return(yhttp::response { .status=200, .body=noPddUidBbResponse }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(call(yield), yamail::expected<OrganizationEnableResult>());
    });
}

static const std::string BILLING_PATH = "/v1/groups/organization/{org_id}/feature/mail_b2b_letters_archive";
const SwitchOrganizationParams switchParams { .taskId=static_cast<ymod_queuedb::TaskId>(1ll<<33) };

yhttp::response billingSuccess() {
    return yhttp::response {
        .status=200,
        .body=""
    };
}

struct OrganizationSwitchTaskTest: public Test, public WithSpawn, public WithHttpSequence {
    std::shared_ptr<StrictMock<MockRepository>> repo;
    http_getter::TypedClientPtr getter;
    yplatform::task_context_ptr ctx;
    WorkerConfigPtr cfg;

    void SetUp() override {
        WithSpawn::SetUp();
        WithHttpSequence::SetUp();

        repo = std::make_shared<StrictMock<MockRepository>>();
        getter = http_getter::createTypedDummyWithRequest(httpSequence);
        ctx = boost::make_shared<yplatform::task_context>();

        cfg = std::make_shared<WorkerConfig>(WorkerConfig {
            .billing = http_getter::TypedEndpoint::fromData(BILLING_PATH, "", http_getter::Executor()),
            .repo = repo,
        });
    }
};

struct OrganizationActivateDeactivateTaskTest : public OrganizationSwitchTaskTest {
    auto callActivate(const OrganizationParams& common, const SwitchOrganizationParams& params, bool lastTry, boost::asio::yield_context yield) const {
        return organizationActivateDeactivateTask(
            true, common, params, WorkerRequestContext(getter, common), cfg,
            lastTry, ctx, yield
        );
    }
};

TEST_F(OrganizationActivateDeactivateTaskTest, shouldActivateOrganization) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncRegisterTaskId(common, switchParams.taskId, _))
        .WillOnce(InvokeArgument<2>(true));

    EXPECT_CALL(*repo, asyncActivateOrganization(common, _))
        .WillOnce(InvokeArgument<1>());

    EXPECT_CALL(*httpSequence, get(WithOrgId(common.orgId)))
            .WillOnce(Return(billingSuccess()));

    EXPECT_CALL(*repo, asyncRemoveTaskId(common, switchParams.taskId, _))
        .WillOnce(InvokeArgument<2>());

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, switchParams, false, yield), yamail::make_expected());
    });
}

TEST_F(OrganizationActivateDeactivateTaskTest, shouldDelayTaskIfOrganizationLocked) {
    EXPECT_CALL(*repo, asyncRegisterTaskId(common, switchParams.taskId, _))
        .WillOnce(InvokeArgument<2>(false));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, switchParams, false, yield), ymod_queuedb::make_unexpected(ymod_queuedb::TaskControl::delay));
    });
}

TEST_F(OrganizationActivateDeactivateTaskTest, shouldDelayTaskIfRemoveTaskIdRaisesExceptionOnLastTry) {
    EXPECT_CALL(*repo, asyncRegisterTaskId(common, switchParams.taskId, _))
        .WillOnce(InvokeArgument<2>(true));

    EXPECT_CALL(*repo, asyncActivateOrganization(common, _))
        .WillOnce(InvokeArgument<1>());

    EXPECT_CALL(*httpSequence, get(WithOrgId(common.orgId)))
            .WillOnce(Return(billingSuccess()));

    EXPECT_CALL(*repo, asyncRemoveTaskId(common, switchParams.taskId, _))
        .WillOnce(Throw(std::runtime_error{""}));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, switchParams, true, yield), ymod_queuedb::make_unexpected(ymod_queuedb::TaskControl::delay));
    });
}

struct OrganizationSwitchTest: public Test, public WithSpawn, public WithHttpSequence {
    std::shared_ptr<StrictMock<MockRepository>> repo;
    std::shared_ptr<StrictMock<ymod_queuedb::MockQueue>> queuedb;
    ConfigPtr cfg;

    void SetUp() override {
        WithSpawn::SetUp();
        WithHttpSequence::SetUp();

        repo = std::make_shared<StrictMock<MockRepository>>();
        queuedb = std::make_shared<StrictMock<ymod_queuedb::MockQueue>>();

        cfg = std::make_shared<Config>(Config {
            .repo = repo,
            .queuedb = queuedb,
        });
    }
};

struct OrganizationSwitchHandlerTest : public OrganizationSwitchTest {
    auto callEnable(const OrganizationParams& params, boost::asio::yield_context yield) const {
        return organizationEnable(
            params, cfg, yield
        );
    }

    auto callActivate(const OrganizationParams& params, boost::asio::yield_context yield) const {
        const CommonParams common { .orgId=params.orgId, .requestId = params.requestId };
        return organizationActivate(
            common, cfg, yield
        );
    }

    auto callDeactivate(const OrganizationParams& params, boost::asio::yield_context yield) const {
        const CommonParams common { .orgId=params.orgId, .requestId = params.requestId };
        return organizationDeactivate(
            common, cfg, yield
        );
    }
};

TEST_F(OrganizationSwitchHandlerTest, shouldCreateActivateTaskWhenOrganizationIsFrozenInEnableCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::frozen
        }));

    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationActivateType(), dumpOrganizationParams(common), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callEnable(common, yield), OrganizationEnableResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldCreateActivateTaskWhenOrganizationIsDisabledInActivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::disabled
        }));

    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationActivateType(), dumpOrganizationParams(common), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, yield), OrganizationActivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldCreateActivateTaskWhenOrganizationIsNotCreatedInActivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>());

    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationActivateType(), dumpOrganizationParams(common), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, yield), OrganizationActivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldCreateDeactivateTaskWhenOrganizationIsActiveInDeactivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::active
        }));

    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationDeactivateType(), dumpOrganizationParams(common), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callDeactivate(common, yield), OrganizationDeactivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldCreateDeactivateTaskWhenOrganizationIsFrozenInDeactivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::frozen
        }));

    EXPECT_CALL(*queuedb, addTaskAsync(_, organizationDeactivateType(), dumpOrganizationParams(common), _, _, _))
        .WillOnce(InvokeArgument<5>(ymod_queuedb::TaskId()));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callDeactivate(common, yield), OrganizationDeactivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateActivateTaskWhenOrganizationIsNotCreatedInEnableCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>());

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callEnable(common, yield), OrganizationEnableResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateActivateTaskWhenOrganizationIsDisabledInEnableCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::disabled
        }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callEnable(common, yield), OrganizationEnableResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateActivateTaskWhenOrganizationIsActiveInEnableCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::active
        }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callEnable(common, yield), OrganizationEnableResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateActivateTaskWhenOrganizationIsActiveInActivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::active
        }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, yield), OrganizationActivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateActivateTaskWhenOrganizationIsFrozenInActivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::frozen
        }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callActivate(common, yield), OrganizationActivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateDeactivateTaskWhenOrganizationIsDisabledInDeactivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>(Organization {
            .state=OrganizationState::disabled
        }));

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callDeactivate(common, yield), OrganizationDeactivateResult());
    });
}

TEST_F(OrganizationSwitchHandlerTest, shouldNotCreateDeactivateTaskWhenOrganizationIsNotCreatedInDeactivateCall) {
    const InSequence s;

    EXPECT_CALL(*repo, asyncGetOrganization(common, _))
        .WillOnce(InvokeArgument<1>());

    spawn([=] (boost::asio::yield_context yield) {
        EXPECT_EQ(callDeactivate(common, yield), OrganizationDeactivateResult());
    });
}

}
