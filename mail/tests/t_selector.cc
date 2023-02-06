#include <generator/selector.h>
#include <catch.hpp>
#include <vector>

using std::vector;
using std::string;
using namespace yxiva::equalizer;

boost::asio::io_service t_selector_io;

class t_selector : public selector
{
public:
    t_selector(std::size_t start_id = 0)
        : selector(t_selector_io, selector_settings{}, "", start_id, nullptr)
        , last_retry(false)
        , retry_started(false)
    {
    }

    operations_list_ptr make_operations_for_ids(std::vector<int> ids)
    {
        auto operations = std::make_shared<operations_list_t>();
        for (int id : ids)
        {
            operations->push_back(make_operation(id));
        }
        return operations;
    }

    operation_ptr make_operation(int operation_id)
    {
        operation_ptr op = std::make_shared<operation>();
        op->operation_id = operation_id;
        return op;
    }

    operations_list_ptr test_process_operations(operations_list_ptr operations)
    {
        auto processed_list = fix_put_sequence(operations);
        if (processed_list && !processed_list->empty())
            update_start_position(processed_list->back()->operation_id);
        return processed_list;
    }

    bool is_retry_started() const override
    {
        return retry_started;
    }

    bool is_last_retry_interval() const override
    {
        return last_retry;
    }

    bool last_retry;
    bool retry_started;
};

TEST_CASE_METHOD(t_selector, "test_process_operations/1", "ok: 1-2-3")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 3 });
    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 3);
    REQUIRE(this->start_select_position() == 3);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/2", "ok: empty")
{
    auto select1 = this->make_operations_for_ids({});
    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 0);
    REQUIRE(this->start_select_position() == 0);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/3", "eternal hole: 1-2-X-4-5")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 4, 5 });
    auto select2 = this->make_operations_for_ids({ 4, 5 });

    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 2);
    REQUIRE(this->start_select_position() == 2);

    insert = this->test_process_operations(select2);
    REQUIRE((!insert));
    REQUIRE(this->start_select_position() == 2);

    this->retry_started = true;
    this->last_retry = true;
    insert = this->test_process_operations(select2);
    REQUIRE(insert->size() == 2);
    REQUIRE(this->start_select_position() == 5);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/4", "eternal holes: 1-2-X-4-5-X-7-X-10")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 4, 5, 7, 10 });
    auto select2 = this->make_operations_for_ids({ 4, 5, 7, 10 });

    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 2);
    REQUIRE(this->start_select_position() == 2);

    this->last_retry = true;
    insert = this->test_process_operations(select2);
    REQUIRE(insert->size() == 4);
    REQUIRE(this->start_select_position() == 10);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/5", "temporary hole: 1-2-X-4-5")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 4, 5 });
    auto select2 = this->make_operations_for_ids({ 3, 4, 5 });

    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 2);
    REQUIRE(this->start_select_position() == 2);

    this->last_retry = true;
    insert = this->test_process_operations(select2);
    REQUIRE(insert->size() == 3);
    REQUIRE(this->start_select_position() == 5);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/6", "temporary holes: 1-2-X-4-5-X-7-X-10")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 4, 5, 7, 10 });
    auto select2 = this->make_operations_for_ids({ 3, 4, 5, 6, 7, 8, 9, 10 });

    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 2);
    REQUIRE(this->start_select_position() == 2);

    insert = this->test_process_operations(select2);
    REQUIRE(insert->size() == 8);
    REQUIRE(this->start_select_position() == 10);
}

TEST_CASE_METHOD(
    t_selector,
    "test_process_operations/7",
    "temporary holes, eternal hole: 1-2-X-4-5-XX-7-X-10")
{
    auto select1 = this->make_operations_for_ids({ 1, 2, 4, 5, 7, 10 });
    auto select2 = this->make_operations_for_ids({ 4, 5, 7, 10 });
    auto select3 = this->make_operations_for_ids({ 3, 4, 5, 7, 8, 9, 10 });

    auto insert = this->test_process_operations(select1);
    CHECK(insert->size() == 2);
    REQUIRE(this->start_select_position() == 2);

    insert = this->test_process_operations(select2);
    CHECK(this->retrying_sequence_max_id() == 10);
    REQUIRE((!insert));

    this->retry_started = true;
    this->last_retry = true;
    insert = this->test_process_operations(select3);
    CHECK(this->retrying_sequence_max_id() == 10);
    CHECK(insert->size() == 7);
    REQUIRE(this->start_select_position() == 10);
}

TEST_CASE_METHOD(
    t_selector,
    "test_process_operations/8",
    "eternal holes, new data: 1-XX-3 --4-5-X-7")
{
    auto select1 = this->make_operations_for_ids({ 1, 3 });
    auto select2 = this->make_operations_for_ids({ 3 });
    auto select3 = this->make_operations_for_ids({ 3, 4, 5, 7 });

    auto insert = this->test_process_operations(select1);
    CHECK(insert->size() == 1);
    REQUIRE(this->start_select_position() == 1);

    insert = this->test_process_operations(select2);
    CHECK(this->retrying_sequence_max_id() == 3);
    REQUIRE((!insert));

    this->retry_started = true;
    this->last_retry = true;
    insert = this->test_process_operations(select3);
    CHECK(insert->size() == 3);
    REQUIRE(this->start_select_position() == 5);
}

TEST_CASE_METHOD(
    t_selector,
    "test_process_operations/9",
    "temporary holes, changed data: 1-3-7 ; 3-4-7; 2-3-4")
{
    auto select1 = this->make_operations_for_ids({ 1, 3, 7 });
    auto select2 = this->make_operations_for_ids({ 3, 4, 7 });
    auto select3 = this->make_operations_for_ids({ 2, 3, 4 });

    auto insert = this->test_process_operations(select1);
    CHECK(insert->size() == 1);
    REQUIRE(this->start_select_position() == 1);

    insert = this->test_process_operations(select2);
    CHECK(this->retrying_sequence_max_id() == 7);
    REQUIRE((!insert));

    this->retry_started = true;
    this->last_retry = true;
    insert = this->test_process_operations(select3);
    CHECK(insert->size() == 3);
    CHECK(this->retrying_sequence_max_id() == 7);
    REQUIRE(this->start_select_position() == 4);
}

TEST_CASE_METHOD(
    t_selector,
    "test_process_operations/10",
    "simulate empty data retries: 1; --; 3-4; 2-3-4")
{
    auto select1 = this->make_operations_for_ids({ 1 });
    auto select2 = this->make_operations_for_ids({});
    auto select3 = this->make_operations_for_ids({ 3, 4 });
    auto select4 = this->make_operations_for_ids({ 2, 3, 4 });

    auto insert = this->test_process_operations(select1);
    REQUIRE(insert->size() == 1);
    REQUIRE(this->start_select_position() == 1);
    REQUIRE(this->retrying_sequence_max_id() == 1);

    insert = this->test_process_operations(select2);
    REQUIRE(insert->size() == 0);
    REQUIRE(this->start_select_position() == 1);
    this->retry_started = true;

    insert = this->test_process_operations(select3);
    REQUIRE((!insert));
    REQUIRE(this->start_select_position() == 1);
    REQUIRE(this->retrying_sequence_max_id() == 4);

    insert = this->test_process_operations(select4);
    REQUIRE(insert->size() == 3);
    REQUIRE(this->start_select_position() == 4);
    REQUIRE(this->retrying_sequence_max_id() == 4);
}

TEST_CASE_METHOD(t_selector, "test_process_operations/11", "")
{
    this->update_start_position(399357829);

    auto select1 = this->make_operations_for_ids({ 399357834, 399357835, 399357836 });

    this->retry_started = true;
    this->last_retry = true;

    auto insert = this->test_process_operations(select1);

    REQUIRE(insert->size() == 3);
    REQUIRE(this->start_select_position() == 399357836);
    REQUIRE(this->retrying_sequence_max_id() == 399357836);
}
