#pragma once

#define T_HOST_IP "127.0.0.1"
#define T_HOST_IP_6 "::"
#define T_HOST make_host_info("host:8080")
#define T_HOST_RESOLVED make_host_info("host.ru:8080")
#define T_BAD_HOST make_host_info("bad:111")
#define T_HOST_6_ONLY make_host_info("host_6_only:8080")
#define T_HOST_4_ONLY make_host_info("host_4_only:8080")
