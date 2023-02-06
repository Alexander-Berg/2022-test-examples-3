#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#define PROFILER_LOG_PATH "test_profiler.log"
#include <pa/async.h>

using namespace std;
using namespace pa;

#define PROFILER_STRINGIFICATOR(TYPE) #TYPE,
const char* service_name[] = {
    "unknown",
    PROFILER_REM_TYPES(PROFILER_STRINGIFICATOR)
};

const size_t num_services = sizeof(service_name)/sizeof(service_name[0]);

const char *hosts[] = {
    "pampers2", "pampers", "lego", "hampers", "hampers64", "ariel", "vax", "palmolive", "arc"
};

const char *reqs[] = {
    "Bubonic Plague", "Foot Gangrene", "Multiple Myeloma", "Dwarfism", "Patau Syndrome", "Leprosy"
};

int main() {
    async_profiler::init(1000000, 500);
    unlink("test_profiler.log");
    time_t tt = time(nullptr);
    struct tm *t = localtime(&tt);
    t->tm_sec = t->tm_min = t->tm_hour = 0;
    time_t begin_time = mktime(t);
    const size_t iterations = 100000;
    srand(unsigned(time(nullptr)));
    for(size_t i=1; i<=iterations; ++i) {
        async_profiler prof;
        char buf[20];
        sprintf(buf, "%u", rand());
        prof.add_internal_time(pa::rem_type(rand()%num_services),
                hosts[rand()%(sizeof(hosts)/sizeof(hosts[0]))],
                reqs[rand()%sizeof(reqs)/sizeof(reqs[0])],
                buf, 
                begin_time + ((24*3600*(i-1))/iterations));
        if(i%1000==0) printf("%zd profiler records generated\r", i);
    }
    printf("\n");
    return 0;
}
