#define CATCH_CONFIG_MAIN
#include "test_log.h"
#include "stream_strand.h"
#include <catch.hpp>

using namespace pipeline;

TEST_CASE("stream_strand/overflow", "")
{
  boost::asio::io_service io;
  StreamSettings settings(5);
  settings.set_window(5);

  auto stream = std::make_shared<StreamStrand<int>>(io, settings);
  auto v1 = std::make_shared<std::vector<int>>(std::initializer_list<int>{1, 2, 3});
  auto v2 = std::make_shared<std::vector<int>>(std::initializer_list<int>{4, 5, 6});

  stream->put_range(v1);

  size_t fail_cnt = 0;
  stream->put_range(v2,
    [&fail_cnt](std::shared_ptr<std::vector<int>> data) {
      fail_cnt += data->size();
    });

  io.run();

  REQUIRE(fail_cnt == 3);
}