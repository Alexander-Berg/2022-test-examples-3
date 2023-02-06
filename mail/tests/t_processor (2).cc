#include "../src/database/data_processor.h"
#include <yplatform/reactor.h>
#include <catch.hpp>
#include <boost/make_shared.hpp>
#include <iostream>
#include <vector>
#include <unistd.h>

using std::vector;
using std::string;

#define UID1 "001"
#define UID2 "002"
#define UID3 "003"
#define SERVICE1 "service1"
#define SERVICE2 "service2"
#define WORKER1 "worker1"
#define WORKER2 "worker2"

namespace ymod_xtasks {

struct T_processor
{
    T_processor() {
        data_ = std::make_shared<data>();
        index_ = std::make_shared<data_index>();
        proc_ = boost::make_shared<data_processor>(data_, index_, domain_settings());
        time_ = 10000;
    }
    boost::shared_ptr<data_processor> proc_;
    data_ptr data_;
    data_index_ptr index_;
    time_t time_;

    task_id_t make_id(const string& uid, const string& service)
    {
        return uid + "##" + service;
    }

    void create_task_with_check(const task_draft& draft)
    {
        auto result = proc_->apply( create_task_args{draft.uid, draft.service, draft.local_id, "", draft.delay_flags} );
        REQUIRE(!result.error);

        REQUIRE(data_->tasks[make_id(draft.uid, draft.service)].is_pending());
    }

    void fin_task_with_check(const string& worker, const task_id_t& id, const time_t time)
    {
        auto tasks_count = data_->workers[worker].active_tasks;
        auto result_fin = proc_->apply(fin_task_args{worker, id, time});
        REQUIRE(!result_fin.error);
        REQUIRE(result_fin.data.success);
        REQUIRE(data_->workers[worker].active_tasks == tasks_count - 1);
    }

    void delay_task_with_check(const string& worker, const task_id_t& id,
            const time_t delay_sec, const time_t time, delay_flags_t delay_flags)
    {
        auto tasks_count = data_->workers[worker].active_tasks;
        auto result = proc_->apply(delay_task_args{worker, id, delay_sec, time, delay_flags});
        REQUIRE(!result.error);
        REQUIRE(result.data.success);
        REQUIRE(data_->tasks[id].is_delayed());
        REQUIRE(data_->workers[worker].active_tasks == tasks_count - 1);
    }
};


TEST_CASE_METHOD(T_processor, "processor/create_single_task", "")
{
    task_draft draft = {UID1, SERVICE1, 0, "", delay_flags::none};
    create_task_with_check(draft);

    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

TEST_CASE_METHOD(T_processor, "processor/create/duplicates", "")
{
    create_task_with_check( {UID1, SERVICE1, 0, "", delay_flags::none} );
    create_task_with_check( {UID1, SERVICE1, 1, "", delay_flags::none} );
    REQUIRE(data_->pending_queue.size() == 1);
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

TEST_CASE_METHOD(T_processor, "processor/create/different", "")
{
    create_task_with_check( {UID1, SERVICE1, 0, "", delay_flags::none} );
    create_task_with_check( {UID1, SERVICE2, 1, "", delay_flags::none} );
    create_task_with_check( {UID2, SERVICE2, 1, "", delay_flags::none} );

    REQUIRE(data_->pending_queue.size() == 3);
}

TEST_CASE_METHOD(T_processor, "processor/get/single", "")
{
    create_task_with_check( {UID1, SERVICE1, 4, "", delay_flags::none} );
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    REQUIRE(!result.error);
    REQUIRE(result.data.tasks.size() == 1);
    auto task = result.data.tasks.at(0);
    REQUIRE(task.id == make_id(UID1, SERVICE1));
    REQUIRE(task.uid == UID1);
    REQUIRE(task.service == SERVICE1);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_active());

    auto result2 = proc_->apply(get_tasks_args{WORKER2, 1, time_});
    REQUIRE(!result2.error);
    REQUIRE(result2.data.tasks.size() == 0);
}

TEST_CASE_METHOD(T_processor, "processor/get/skip_redunant", "")
{
    create_task_with_check( {UID1, SERVICE1, 4, "", delay_flags::none} );
    REQUIRE(data_->pending_queue.size());
    data_->pending_queue.push_back(data_->pending_queue.front());

    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    REQUIRE(!result.error);
    REQUIRE(result.data.tasks.size() == 1);

    auto result2 = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    REQUIRE(!result2.error);
    REQUIRE(result2.data.tasks.size() == 0);
}

TEST_CASE_METHOD(T_processor, "processor/fin_task/simple", "")
{
    task_draft draft = {UID1, SERVICE1, 4, "", delay_flags::none};
    create_task_with_check(draft);
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    REQUIRE(result.data.tasks.size() == 1);

    fin_task_with_check(WORKER1, result.data.tasks.at(0).id, time_+1);

    REQUIRE(data_->tasks.count(make_id(UID2, SERVICE2)) == 0);

    auto result_get2 = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    REQUIRE(!result_get2.error);
    REQUIRE(result_get2.data.tasks.size() == 0);
}

TEST_CASE_METHOD(T_processor, "processor/fin_task/wrong_worker", "")
{
    create_task_with_check( {UID1, SERVICE1, 4, "", delay_flags::none} );

    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    auto result_fin = proc_->apply(fin_task_args{WORKER2, result.data.tasks.at(0).id, time_+1});
    REQUIRE(result_fin.error);
    REQUIRE(result_fin.error.code == err_code_worker_mismatch);

    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_active());
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/attempt", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 4, "", delay_flags::none});
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    fin_task_with_check(WORKER1, result.data.tasks.at(0).id, time_+1);
    auto result_delay = proc_->apply(delay_task_args{WORKER1,
        result.data.tasks.at(0).id, 5, time_+1, delay_flags::none});
    REQUIRE(result_delay.error);
    REQUIRE(result_delay.error.code == err_code_no_such_task);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/wrong_worker", "")
{
    task_draft draft = {UID1, SERVICE1, 4, "", delay_flags::none};
    proc_->apply(draft);
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    auto result_delay = proc_->apply(delay_task_args{WORKER2,
        result.data.tasks.at(0).id, 5, time_+1, delay_flags::none});
    REQUIRE(result_delay.error);
    REQUIRE(result_delay.error.code == err_code_worker_mismatch);

    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_active());
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/simple", "")
{
    task_draft draft = {UID1, SERVICE1, 4, "", delay_flags::none};
    proc_->apply(draft);
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    delay_task_with_check(WORKER1, result.data.tasks.at(0).id, 5, time_+1, delay_flags::none);
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].delay_sec() == 5);
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/ignore_if_pending", "")
{
    task_draft draft = {UID1, SERVICE1, 4, "", delay_flags::none};
    proc_->apply(draft);
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    delay_task_with_check(WORKER1, result.data.tasks.at(0).id, 5, time_+1, delay_flags::ignore_if_pending);
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].delay_sec() == 5);
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/wakeup_on_create", "")
{
    task_draft draft = {UID1, SERVICE1, 4, "", delay_flags::none};
    proc_->apply(draft);
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});

    delay_task_with_check(WORKER1, result.data.tasks.at(0).id, 5, time_+1, delay_flags::wakeup_on_create);
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].delay_sec() == 5);

    proc_->apply(draft);

    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/pending/ignore_if_pending", "")
{
    create_task_with_check({UID1, SERVICE1, 4, "", delay_flags::none});
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    create_task_with_check({UID1, SERVICE1, 4, "", delay_flags::ignore_if_pending});
    auto delay_result = proc_->apply(delay_task_args{WORKER1, result.data.tasks.at(0).id, 5, time_+1, delay_flags::none});

    REQUIRE(!delay_result.error);
    REQUIRE(delay_result.data.success);
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
}

TEST_CASE_METHOD(T_processor, "processor/delay_task/pending/no_flags", "")
{
    create_task_with_check({UID1, SERVICE1, 4, "", delay_flags::none});
    auto result = proc_->apply(get_tasks_args{WORKER1, 1, time_});
    create_task_with_check({UID1, SERVICE1, 4, "", delay_flags::none});
    auto delay_result = proc_->apply(delay_task_args{WORKER1, result.data.tasks.at(0).id, 5, time_+1, delay_flags::none});
    REQUIRE(!delay_result.error);
    REQUIRE(delay_result.data.success);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    // pending is canceled
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

TEST_CASE_METHOD(T_processor, "processor/wakeup_delayed/simple", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 4, "", delay_flags::none} );
    proc_->apply(create_task_args{UID2, SERVICE2, 4, "", delay_flags::none} );
    proc_->apply(create_task_args{UID3, SERVICE1, 4, "", delay_flags::none} );
    auto result = proc_->apply(get_tasks_args{WORKER1, 3, time_});
    delay_task_with_check(WORKER1, make_id(UID1, SERVICE1), 5, time_+1, delay_flags::none);
    delay_task_with_check(WORKER1, make_id(UID2, SERVICE2), 20, time_+1, delay_flags::none);
    delay_task_with_check(WORKER1, make_id(UID3, SERVICE1), 8, time_+1, delay_flags::ignore_if_pending);

    auto result_delay = proc_->apply(wakeup_delayed_args{time_ + 10});
    REQUIRE(!result_delay.error);
    REQUIRE(result_delay.data.count == 2);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(!data_->tasks[make_id(UID2, SERVICE2)].is_active());
    REQUIRE(data_->tasks[make_id(UID2, SERVICE2)].is_delayed());
    REQUIRE(!data_->tasks[make_id(UID3, SERVICE1)].is_active());
    REQUIRE(!data_->tasks[make_id(UID3, SERVICE1)].is_delayed());
}

TEST_CASE_METHOD(T_processor, "processor/cleanup_active/simple", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );
    proc_->apply(create_task_args{UID2, SERVICE2, 1, "", delay_flags::none} );
    proc_->apply(create_task_args{UID3, SERVICE1, 1, "", delay_flags::none} );
    proc_->apply(get_tasks_args{WORKER1, 1, time_});
    proc_->apply(get_tasks_args{WORKER2, 1, time_ + 4});
    proc_->apply(get_tasks_args{WORKER2, 1, time_ + 10});

    auto result = proc_->apply(cleanup_active_args{time_ + 12, 6});
    REQUIRE(!result.error);
    REQUIRE(result.data.count == 2);
    REQUIRE(data_->workers[WORKER1].active_tasks == 0);
    REQUIRE(data_->workers[WORKER2].active_tasks == 1);
}

TEST_CASE_METHOD(T_processor, "processor/cleanup_workers/simple", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none});
    proc_->apply(create_task_args{UID2, SERVICE2, 1, "", delay_flags::none});
    proc_->apply(create_task_args{UID3, SERVICE1, 1, "", delay_flags::none});
    proc_->apply(get_tasks_args{WORKER1, 1, time_});
    proc_->apply(get_tasks_args{WORKER2, 1, time_});
    proc_->apply(get_tasks_args{WORKER2, 1, time_});
    proc_->apply(alive_args{WORKER2, time_ + 4});
    proc_->apply(alive_args{WORKER1, time_ + 10});

    auto result = proc_->apply(cleanup_workers_args{time_ + 15, 6});
    REQUIRE(!result.error);
    REQUIRE(result.data.count == 2);
    REQUIRE(data_->workers[WORKER1].active_tasks == 1);
    REQUIRE(data_->workers[WORKER2].active_tasks == 0);
}

TEST_CASE_METHOD(T_processor, "processor/create/while_active", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );
    proc_->apply(get_tasks_args{WORKER1, 1, time_});
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );

    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_active());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

TEST_CASE_METHOD(T_processor, "processor/create/while_delayed/wakeup", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );
    proc_->apply(get_tasks_args{WORKER1, 1, time_});
    delay_task_with_check(WORKER1, make_id(UID1, SERVICE1), 5, time_ + 1, delay_flags::none);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::wakeup_on_create} );
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

TEST_CASE_METHOD(T_processor, "processor/create/while_delayed/ignore", "")
{
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );
    proc_->apply(get_tasks_args{WORKER1, 1, time_});
    delay_task_with_check(WORKER1, make_id(UID1, SERVICE1), 5, time_ + 1, delay_flags::none);

    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
    proc_->apply(create_task_args{UID1, SERVICE1, 1, "", delay_flags::none} );
    REQUIRE(data_->tasks[make_id(UID1, SERVICE1)].is_delayed());
    REQUIRE(!data_->tasks[make_id(UID1, SERVICE1)].is_pending());
}

}
