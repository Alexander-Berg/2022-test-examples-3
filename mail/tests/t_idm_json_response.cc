#include "idm_helpers.h"
#include "web/idm/json_response.h"
#include <catch.hpp>

namespace yxiva::web::idm {

TEST_CASE("generate_tree/0_services")
{
    services_type services;

    REQUIRE(generate_tree(services).pretty_stringify() == R"({
    "code": 0,
    "roles": {
        "slug": "project",
        "name": "project",
        "values": {}
    }
})");
}

TEST_CASE("generate_tree/1_service")
{
    services_type services{ make_service_data("service123", "music") };

    REQUIRE(generate_tree(services).pretty_stringify() == R"({
    "code": 0,
    "roles": {
        "slug": "project",
        "name": "project",
        "values": {
            "service123": {
                "name": "service123",
                "roles": {
                    "slug": "environment",
                    "name": "environment",
                    "values": {
                        "sandbox": {
                            "name": "sandbox",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "corp": {
                            "name": "corp",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "production": {
                            "name": "production",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        }
                    }
                },
                "aliases": [
                    {
                        "type": "default",
                        "name": "service123%%music"
                    }
                ]
            }
        }
    }
})");
}

TEST_CASE("generate_tree/2_services")
{
    services_type services{ make_service_data("service123", "music"),
                            make_service_data("service456", "kinopoisk") };

    REQUIRE(generate_tree(services).pretty_stringify() == R"({
    "code": 0,
    "roles": {
        "slug": "project",
        "name": "project",
        "values": {
            "service123": {
                "name": "service123",
                "roles": {
                    "slug": "environment",
                    "name": "environment",
                    "values": {
                        "sandbox": {
                            "name": "sandbox",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "corp": {
                            "name": "corp",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "production": {
                            "name": "production",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        }
                    }
                },
                "aliases": [
                    {
                        "type": "default",
                        "name": "service123%%music"
                    }
                ]
            },
            "service456": {
                "name": "service456",
                "roles": {
                    "slug": "environment",
                    "name": "environment",
                    "values": {
                        "sandbox": {
                            "name": "sandbox",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "corp": {
                            "name": "corp",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        },
                        "production": {
                            "name": "production",
                            "roles": {
                                "slug": "role",
                                "name": "role",
                                "values": {
                                    "subscriber": "subscriber",
                                    "publisher": "publisher"
                                }
                            }
                        }
                    }
                },
                "aliases": [
                    {
                        "type": "default",
                        "name": "service456%%kinopoisk"
                    }
                ]
            }
        }
    }
})");
}

TEST_CASE("list_roles/0_services")
{
    services_type services;

    REQUIRE(list_roles(services).pretty_stringify() == R"({
    "code": 0,
    "users": []
})");
}

TEST_CASE("list_roles/1_service-0_users")
{
    services_type services{ make_service_data("service123", "music") };

    REQUIRE(list_roles(services).pretty_stringify() == R"({
    "code": 0,
    "users": []
})");
}

TEST_CASE("list_roles/1_service-1_user")
{
    services_type services{ make_service_data(
        "service123", "music", { { "sandbox", { 1, "app1" } } }) };

    REQUIRE(list_roles(services).pretty_stringify() == R"({
    "code": 0,
    "users": [
        {
            "login": "1",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service123",
                    "environment": "sandbox",
                    "role": "publisher"
                }
            ]
        }
    ]
})");
}

TEST_CASE("list_roles/1_service-2_users")
{
    services_type services{ make_service_data(
        "service123", "music", { { "sandbox", { 1, "app1" } }, { "corp", { 2, "app2" } } }) };

    REQUIRE(list_roles(services).pretty_stringify() == R"({
    "code": 0,
    "users": [
        {
            "login": "2",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service123",
                    "environment": "corp",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "1",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service123",
                    "environment": "sandbox",
                    "role": "publisher"
                }
            ]
        }
    ]
})");
}

TEST_CASE("list_roles/2_services-4_users")
{
    services_type services{
        make_service_data(
            "service123", "music", { { "sandbox", { 1, "app1" } }, { "corp", { 2, "app2" } } }),
        make_service_data(
            "service456",
            "cisum",
            { { "sandbox", { 1, "app1" } } },
            { { "production", { 3, "app3" } } })
    };

    REQUIRE(list_roles(services).pretty_stringify() == R"({
    "code": 0,
    "users": [
        {
            "login": "2",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service123",
                    "environment": "corp",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "1",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service123",
                    "environment": "sandbox",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "1",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service456",
                    "environment": "sandbox",
                    "role": "publisher"
                }
            ]
        },
        {
            "login": "3",
            "subject_type": "tvm_app",
            "roles": [
                {
                    "project": "service456",
                    "environment": "production",
                    "role": "subscriber"
                }
            ]
        }
    ]
})");
}

}
