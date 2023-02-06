#include "mocks.h"

#include "processor/catalogue.h"
#include "processor/interface.h"
#include "yxiva/core/types.h"

#include <catch.hpp>
#include <vector>

struct t_catalogue
{
    t_catalogue()
        : catalogue_(new yxiva::catalogue())
        , subscriber1(new mock_subscriber())
        , subscriber2(new mock_subscriber())
    {
    }

    ~t_catalogue()
    {
    }

    shared_ptr<yxiva::catalogue> catalogue_;

    shared_ptr<mock_subscriber> subscriber1;
    shared_ptr<mock_subscriber> subscriber2;

    string service = "mail";
    string uid = "user001";
    string sub_id = "sub001";
};

TEST_CASE_METHOD(t_catalogue, "catalogue/add", "")
{
    auto added = catalogue_->add(uid, service, sub_id, subscriber1);
    REQUIRE(added);
    REQUIRE(catalogue_->stat()->get_subscribers() == 1);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/add/same", "")
{
    catalogue_->add(uid, service, sub_id, subscriber1);
    auto added = catalogue_->add(uid, service, sub_id, subscriber1);
    REQUIRE_FALSE(added);
    REQUIRE(catalogue_->stat()->get_subscribers() == 1);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/del", "")
{
    catalogue_->add(uid, service, sub_id, subscriber1);
    auto deleted = catalogue_->del(uid, service, sub_id);
    REQUIRE(deleted);
    REQUIRE(catalogue_->stat()->get_subscribers() == 0);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/del/negative", "")
{
    auto deleted = catalogue_->del(uid, service, sub_id);
    REQUIRE_FALSE(deleted);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/find_by_id", "")
{
    catalogue_->add(uid, service, sub_id, subscriber1);

    auto subscriber_by_id = catalogue_->find_by_id(uid, service, sub_id);
    REQUIRE(subscriber_by_id);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/find_by_id/negative", "")
{
    auto subscriber_by_id = catalogue_->find_by_id(uid, service, sub_id);
    REQUIRE(!subscriber_by_id);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/find_by_uid_service", "")
{
    catalogue_->add(uid, service, "sub001", subscriber1);
    catalogue_->add(uid, service, "sub002", subscriber2);

    auto subscribers = catalogue_->find_by_uid_service(uid, service);
    REQUIRE(subscribers.size() == 2);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/find_by_uid_service/negative", "")
{
    auto subscribers = catalogue_->find_by_uid_service(uid, service);
    REQUIRE(subscribers.empty());
}

TEST_CASE_METHOD(t_catalogue, "catalogue/del_by_subscriber_id", "")
{
    catalogue_->add(uid, service, "sub001", subscriber1);
    catalogue_->add(uid, service, "sub002", subscriber1);
    catalogue_->add(uid, service, "sub003", subscriber2);

    auto deleted = catalogue_->del_by_subscriber_id(uid, service, subscriber1->ctx()->uniq_id());
    REQUIRE(deleted == 2);
    REQUIRE(catalogue_->stat()->get_subscribers() == 1);
}

TEST_CASE_METHOD(t_catalogue, "catalogue/del_by_subscriber_id/negative", "")
{
    auto deleted = catalogue_->del_by_subscriber_id(uid, service, "SOME_ID");
    REQUIRE(deleted == 0);
}
