#include "mock_clock.h"

mock_clock::time_point mock_clock::tp_now = std::chrono::system_clock::now();
