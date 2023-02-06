#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include "test_log.h"

#define PIPELINE_DEBUG
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
        offset_ += put_size;
        on_generated(std::make_shared<vector<data_ptr>>(begin, end));
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
        Processor<stream_t>(io, capacity),
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

int main(int argc, char** argv) {

    if (argc < 5) {
        std::cout << "usage: ./loadtest <sequence length> <stream length> <min generate> <max generate>\n";
        return 1;
    }

    unsigned generate_count = 0;
    unsigned stream_size = 0;
    unsigned min_generate_size = 0;
    unsigned max_generate_size = 0;
    try {
        generate_count = std::stoi(argv[1]);
        stream_size = std::stoi(argv[2]);
        min_generate_size = std::stoi(argv[3]);
        max_generate_size = std::stoi(argv[4]);
        assert(generate_count > 0 && stream_size > 0);
        assert(min_generate_size > 0 && max_generate_size > 0
               && min_generate_size <= max_generate_size
               && min_generate_size <= stream_size);
    } catch (const std::exception& ex) {
        std::cout << "error: bad cast, exception: " << ex.what() << "\n";
        return 1;
    }

    boost::asio::io_service io(1);

    std::vector<int> test_data(generate_count);
    std::iota(test_data.begin(), test_data.end(), 1);
    std::vector<data_ptr> test_sequence;
    for (std::size_t i = 0; i < test_data.size(); i++)
    {
        test_sequence.push_back(std::make_shared<data_t>(test_data[i]));
    }

    auto generator = std::make_shared<IntsGenerator>(io, test_sequence, max_generate_size, min_generate_size);
    auto proc1 = std::make_shared<IntsProcessor>(io, "selector", stream_size, stream_size, false);
    auto proc2 = std::make_shared<IntsProcessor>(io, "fill", 1, stream_size/*, false*/);
    auto proc3 = std::make_shared<IntsProcessor>(io, "bb", 1, stream_size/*, false*/);
    auto proc4 = std::make_shared<IntsProcessor>(io, "wmi", 1, stream_size/*, false*/);
    auto proc5 = std::make_shared<IntsProcessor>(io, "equalizer", 1, stream_size/*, false*/);
    auto last_proc = proc5;

    Plan<stream_t> plan = from<stream_t>(generator) | proc1 | proc2 | proc3 | proc4 | proc5;
    std::cout << "running test with " << plan.processors().size() << " processors:"
              " stream_size=" << stream_size <<
              " sequence_size=" << generate_count <<
              " min_generates_size=" << min_generate_size <<
              " max_generate_size=" << max_generate_size <<
              "\n";
    plan.start();

    boost::posix_time::ptime start_time = boost::posix_time::microsec_clock::local_time();
    io.run();
    boost::posix_time::ptime end_time = boost::posix_time::microsec_clock::local_time();
    if (last_proc->input()->begin_id() != generate_count) {
        std::cout << "[error] not all data was fully processed: last_proc.id_offset=" << last_proc->input()->begin_id() << "\n";
        return 1;
    } else {
        std::cout << "win!\n";
    }
    boost::posix_time::time_duration duration = end_time - start_time;
    double time_seconds = static_cast<double>(duration.total_milliseconds()) / 1000.0;
    std::cout << "total time: " << time_seconds << "s\n";
    std::cout << "slots per second: " << (generate_count / time_seconds) << "\n";
    return 0;
}
