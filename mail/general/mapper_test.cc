#include "simple_mapper.h"
#include "consistent_mapper.h"
#include "tinyint.h"

#ifndef SEED
#define SEED 1
#endif

#ifndef MAXINPUT
#define MAXINPUT 10
#endif

#define MAPPER_TYPE consistent_mapper<tinyint<0, MAXINPUT>, int>
#define MAPPER_DEF MAPPER_TYPE(MAPPER_TYPE::c_dots_per_output_default, SEED)
/*
#define MAPPER_TYPE simple_mapper<tinyint<0, MAXINPUT>, int>
#define MAPPER_DEF MAPPER_TYPE()
*/

#include <memory>
#include <iostream>

/*
#define OUT_TEST(mapper) \
    { \
        for (int i = 0; i <= MAXINPUT; ++i) \
            std::cout << i << " -> " << mapper->map(i) << std::endl; \
        std::cout << std::endl; \
    }
*/
#define OUT_TEST(mapper) \
    { \
        std::map<MAPPER_TYPE::output_type, size_t> counts; \
        for (unsigned int i = 0; i <= MAXINPUT; ++i) \
        { \
            if (i % 1000000 == 0) \
                std::cerr << i/1000000 << std::endl; \
            counts[mapper->map(i)]++; \
        } \
        std::cout << "Stats:\n"; \
        for (std::map<MAPPER_TYPE::output_type, size_t>::iterator it = counts.begin(); it != counts.end(); ++it) \
            std::cout << "\t" << it->first << ": " << it->second << "\n"; \
    }

int main()
{
    std::auto_ptr<MAPPER_TYPE> m(new MAPPER_DEF);
    OUT_TEST(m);
    m->set_output_range(0, 3);
    OUT_TEST(m);

    m->mask(2);
    OUT_TEST(m);
    m->mask(1);
    OUT_TEST(m);
    m->mask(3);
    OUT_TEST(m);
    m->mask(0);
    std::cout << "all masked: " << (m->all_masked() ? "true" : "false") << std::endl;

    m->set_output_range(-1, 4);
    OUT_TEST(m);
    m->unmask(1);
    OUT_TEST(m);
    m->set_output_range(0, 1);
    OUT_TEST(m);
    m->set_output_range(0, 3);
    OUT_TEST(m);

    m.reset(new MAPPER_DEF);
    m->set_output_range(0, 8);
    OUT_TEST(m);
}
