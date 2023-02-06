#include <iostream>
#define PROFILER_LOG_PATH "prof.log"
#include <pa/stack.h>
#include <unistd.h>

using namespace std;
using namespace pa;

void async_test ()
{
  async_profiler::init(1000000, 500,"prof.log");
  {
    async_stack_profiler profiler(pa::js);
    profiler.set_request("call");
    usleep(50000);
    {
      async_stack_profiler profiler(pa::mulca);
      profiler.set_request("get");
      usleep(150000);
    }
  }
}

void sync_test ()
{
  sync_stack_profiler profiler(pa::js);
  profiler.set_request("call");
  usleep(50000);
  {
    sync_stack_profiler profiler(pa::mulca);
    profiler.set_request("get");
    usleep(150000);
  }
}

int main() {
  async_test ();
  sync_test ();
  return 0;
}
