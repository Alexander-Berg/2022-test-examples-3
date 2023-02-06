#include <boost/asio.hpp>
#include <boost/thread.hpp>

#include "test_log.h"

//#define DEBUG
#include "stream_strand.h"
#include "generator.h"
#include "processor.h"
#include "commit_listener.h"
#include "plan.h"

using namespace std;
using namespace pipeline;

struct data_t {
    data_t(int v) : value(v) {}
    int value;
};

typedef std::shared_ptr<data_t> data_ptr;
typedef StreamStrand<data_ptr> stream_t;

std::ostream& operator<<(std::ostream& o, data_ptr data)
{
    if (data) {
        o << data->value;
    } else {
        o << "null";
    }
    return o;
}

class IntsGenerator : public Generator<stream_t>
{
public:
    IntsGenerator(boost::asio::io_service& io, std::vector<data_ptr> input_sequence,
            std::size_t put_size, std::size_t min_free_space = 1U
    ) :
        Generator<stream_t>(io, min_free_space), offset_(0), put_size_(put_size), sequence_(input_sequence)
    {
    }

protected:
    void generate(std::size_t free_space)
    {
        if (sequence_.size() <= offset_) {
            stop();
            return;
        }

        std::size_t available_size = sequence_.size() - offset_;
        std::size_t put_size = std::min({free_space, put_size_, available_size});

        auto begin = sequence_.begin() + offset_;
        auto end = begin + put_size;
        YLOG_G(debug) << "[generator] put range: " << offset_ << "-" << offset_ + put_size  - 1;
        offset_ += put_size;

        on_generated(std::make_shared<std::vector<data_ptr>>(begin, end));
    }

private:
    std::size_t offset_;
    std::size_t put_size_;
    std::vector<data_ptr> sequence_;
};

class IntsProcessor : public Processor<stream_t>
{
public:
    IntsProcessor(boost::asio::io_service& io, std::string label, std::size_t commit_count,
                  std::size_t capacity = 100, bool commit_separately = true) :
        Processor<stream_t>(io, StreamSettings(capacity)),
        commit_count_(commit_count),
        commit_separately_(commit_separately)
    {
        input()->label(label);
    }

protected:
    void on_data(stream_ptr stream, std::size_t begin_id, std::size_t end_id)
    {
        std::size_t current = begin_id;
        if (commit_separately_) {
            for (std::size_t i = begin_id; i < end_id; i++) {
                stream->commit(i);
            }
        } else {
            while (current < end_id) {
                std::size_t commit_id = current + commit_count_;
                commit_id = commit_id >= end_id ? end_id - 1 : commit_id;
                stream->commit_until(commit_id);
                current = commit_id + 1;
            }
        }
    }

private:
    std::size_t commit_count_;
    bool commit_separately_;
};


class CheckOutput : public CommitListener<stream_t>
{
public:
    CheckOutput(std::vector<data_ptr> sequence)
    : sequence_(sequence),
      last_committed_(sequence_.begin())
    {}

    void final_check()
    {
        assert(last_committed_ == sequence_.end());
    }

protected:
    void on_commit(stream_t::temp_collection_ptr committed_range)
    {
        for (auto it = committed_range->begin(); it != committed_range->end(); it++) {
            assert(last_committed_ != sequence_.end());
            auto jt = last_committed_;
            YLOG_G(debug) << "check: " << *jt << " == " << *it;
            assert(*jt == *it);
            last_committed_++;
        }
    }

private:
    std::vector<data_ptr> sequence_;
    std::vector<data_ptr>::iterator last_committed_;
};

int main() {
    boost::asio::io_service io(2);
    boost::thread_group workers;

    std::vector<int> test_data(1005);
    std::iota(test_data.begin(), test_data.end(), 1);
    std::vector<data_ptr> test_sequence;
    for (std::size_t i = 0; i < test_data.size(); i++)
    {
        test_sequence.push_back(std::make_shared<data_t>(test_data[i]));
    }

    auto generator = std::make_shared<IntsGenerator>(io, test_sequence, 20, 5);
    auto proc1 = std::make_shared<IntsProcessor>(io, "A", 3);
    auto proc2 = std::make_shared<IntsProcessor>(io, "B", 5, 50);
    auto proc3 = std::make_shared<IntsProcessor>(io, "B-3/4", 2, 50, false);
    auto proc4 = std::make_shared<IntsProcessor>(io, "C", 2, 10);
    auto proc5 = std::make_shared<IntsProcessor>(io, "D", 10);
    auto checker = std::make_shared<CheckOutput>(test_sequence);

    Plan<stream_t> plan = from<stream_t>(generator) | proc1 | proc2 | proc3 | proc4 | proc5 | checker;
    plan.start();

    io.run();
    std::atomic_bool* run = new std::atomic_bool(true);
    workers.create_thread([&io, run] {
        while (run->load()) {
            try {
                io.reset();
                io.run();
            } catch (...) {
                assert(false);
            }
        }
    });
    workers.create_thread([&plan, run] {
        plan.stop();
        run->store(false);
    });

    workers.join_all();

    checker->final_check();
    return 0;
}
