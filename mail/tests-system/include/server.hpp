#pragma once

#include <yplatform/module.h>

namespace apq_tester::server {

class Server : public yplatform::module
{
public:
    void init();

private:
    template <class Handler, class Uri>
    void bind();
};

}
