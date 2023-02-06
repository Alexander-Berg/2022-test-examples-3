#include <assert.h>
#include <yplatform/net/byte_order.h>

int main()
{
    int8_t t1 = 123;
    int16_t t2 = 32700;
    int32_t t3 = 2000000000;
    int64_t t4 = 62313123123121;
    assert(yplatform::net::network_2_host(yplatform::net::host_2_network(t1)) == t1);
    assert(yplatform::net::network_2_host(yplatform::net::host_2_network(t2)) == t2);
    assert(yplatform::net::network_2_host(yplatform::net::host_2_network(t3)) == t3);
    assert(yplatform::net::network_2_host(yplatform::net::host_2_network(t4)) == t4);
    return 0;
}
