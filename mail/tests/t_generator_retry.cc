#include <vector>
#include <boost/asio.hpp>

#include "test_log.h"

#define PIPELINE_DEBUG
#include "stream_strand.h"
#include "generator.h"
#include "processor.h"

#include "directed_buffer.h"

using namespace std;
using namespace pipeline;

typedef StreamStrand<int> stream_t;

class SkipGenerator : public Generator<stream_t>
{
public:
    SkipGenerator(boost::asio::io_service& io, std::vector<int> input_sequence)
    : Generator<stream_t>(io, 1U, boost::posix_time::milliseconds(1)),
      iteration_(0),
      sequence_(input_sequence)
    {}


protected:
    void generate(std::size_t /*free_space*/)
    {
        iteration_++;
        if (iteration_ % 2 == 0) {
            YLOG_G(debug) << "generating skipped";
            generate_failed();
        } else {
            std::shared_ptr<vector<int>> test_data = std::make_shared<vector<int>>(
                    sequence_.begin() + (iteration_ - iteration_ % 2),
                    sequence_.begin() + iteration_ + 1);
            on_generated(test_data);
        }
    }

private:
    std::size_t iteration_;
    std::vector<int> sequence_;
};

class NotFullGenerator : public Generator<stream_t>
{
public:
    NotFullGenerator(boost::asio::io_service& io, std::vector<int> input_sequence)
    : Generator<stream_t>(io),
      iteration_(0),
      sequence_(input_sequence)
    {}

protected:
    void generate(std::size_t free_space)
    {
        YLOG_G(debug) << "generating 1 element of " << free_space;
        iteration_++;
        on_generated(std::make_shared<vector<int>>(sequence_.begin() + iteration_ - 1, sequence_.begin() + iteration_));
    };

private:
    std::size_t iteration_;
    std::vector<int> sequence_;
};

class CheckInput : public Processor<stream_t>
{
public:
    CheckInput(boost::asio::io_service& io, std::size_t capacity, std::vector<int> sequence)
    : Processor<stream_t>(io, capacity),
      sequence_(sequence),
      is_finished_(false)
    {}

    bool is_finished() const
    {
        return is_finished_;
    }

protected:
    void on_data(stream_ptr stream, std::size_t begin_id, std::size_t end_id)
    {
        assert(end_id <= sequence_.size());
        for (std::size_t i = begin_id; i < end_id; i++) {
            YLOG_G(debug) << "check: " << stream->at(i) << " == " << sequence_[i];
            assert(stream->at(i) == sequence_[i]);
        }
        is_finished_ = end_id == sequence_.size();
    }

private:
    std::vector<int> sequence_;
    bool is_finished_;
};

template <typename GeneratorT>
void test(std::vector<int> test_data)
{
    boost::asio::io_service io(2);
    auto generator = std::make_shared<GeneratorT>(io, test_data);
    auto checker = std::make_shared<CheckInput>(io, 10, test_data);
    generator->output(checker->input());
    checker->start();
    generator->start();
    auto count = io.run();
    std::cout << "io.run executed " << count << " handlers\n";
    assert(checker->is_finished());
    //generator->stop();
    //checker->stop();
}

int main() {
    std::vector<int> test_data(10);
    std::iota(test_data.begin(), test_data.end(), 1);

    test<SkipGenerator>(test_data);
    test<NotFullGenerator>(test_data);
    return 0;
}
