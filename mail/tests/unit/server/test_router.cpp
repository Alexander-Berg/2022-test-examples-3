#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/router/router.hpp>

#include <memory>
#include <string_view>
#include <vector>

namespace router {

static inline std::ostream& operator <<(std::ostream& stream, Error value) {
    switch (value) {
        case Error::method_not_found:
            return stream << "method_not_found";
        case Error::location_not_found:
            return stream << "location_not_found";
    }
    return stream << static_cast<std::underlying_type_t<Error>>(value);
}

} // namespace router

namespace {

namespace hana = boost::hana;

using namespace testing;
using namespace hana::literals;
using namespace router;
using namespace router::literals;

struct F {
    void operator ()() const {}
};

struct X {
    std::string_view value;

    static X make(const std::string& value) {
        return X {value};
    }
};

bool operator ==(const X& lhs, const X& rhs) {
    return lhs.value == rhs.value;
}

struct F2 {
    void operator ()(X) const {}
};

struct Y {
    std::string_view value;

    static Y make(const std::string& value) {
        return Y {value};
    }
};

bool operator ==(const Y& lhs, const Y& rhs) {
    return lhs.value == rhs.value;
}

struct F3 {
    void operator ()(X, Y) const {}
};

struct Monostate {
    std::monostate value;

    static std::variant<Monostate, std::string_view> make(const std::string& value) {
        if (value == "monostate") {
            return Monostate {std::monostate {}};
        } else {
            return "invalid monostate value";
        }
    }
};

bool operator ==(const Monostate& lhs, const Monostate& rhs) {
    return lhs.value == rhs.value;
}

static_assert(IsParameter<decltype(parameter<X>)>::value, "decltype(parameter<X>) is parameter");
static_assert(!IsParameter<X>::value, "X is not parameter");

static_assert(IsHandler<decltype(handler(F {}))>::value, "decltype(handler(F {})) is handler");
static_assert(!IsHandler<F>::value, "F is not handler");

static_assert(IsLocation<decltype("a"_location)>::value, "decltype(\"a\"_location) is location");
static_assert(!IsLocation<decltype("a"_s)>::value, "decltype(\"a\"_s) is not location");

static_assert(IsMethod<decltype("GET"_method)>::value, "decltype(\"GET\"_method) is method");
static_assert(!IsMethod<decltype("GET"_s)>::value, "decltype(\"GET\"_s) is not method");

constexpr const auto m = [] {};
static_assert(IsMiddleware<decltype(middleware(m))>::value, "decltype(middleware(m)) is middleware");
static_assert(!IsMiddleware<decltype(m)>::value, "decltype(m) is not middleware");

static_assert(hana::equal("a"_location, "a"_location), "Locations with same types should be equal");
static_assert(!hana::equal("a"_location, "b"_location), "Locations with different types should be not equal");

static_assert(hana::equal("GET"_method, "GET"_method), "Methods with same types should be equal");
static_assert(!hana::equal("GET"_method, "POST"_method), "Methods with different types should be not equal");

static_assert(hana::equal(handler(F {}), handler(F {})), "Handlers with same types should be equal");
static_assert(!hana::equal(handler(F {}), handler(F2 {})), "Handlers with different types should not be equal");

static_assert(hana::equal(parameter<X>, parameter<X>), "Parameters with same types should be equal");
static_assert(!hana::equal(parameter<X>, parameter<Y>), "Parameters with different types should be equal");

static_assert(
    "a"_l / "GET"_m
    == tree(hana::make_tuple(hana::make_pair(location<hana::string<'a'>>,
                                             method<hana::string<'G', 'E', 'T'>>))),
    "operator / produce pair for location and method"
);

static_assert(
    ("a"_location / "GET"_method(F {}))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair("GET"_method, handler(F {}))
        ))
    )),
    "every handler is wrapped by handler type"
);

static_assert(
    ("GET"_method(F {}))
    == tree(hana::make_tuple(hana::make_pair("GET"_method, handler(F {})))),
    "method is wrapped by hana::tuple and tree"
);

static_assert(
    ("GET"_method(F {}) | "POST"_method(F {}))
    == tree(hana::make_tuple(
        hana::make_pair("GET"_method, handler(F {})),
        hana::make_pair("POST"_method, handler(F {}))
    )),
    "operator | produce tuple of pairs"
);

static_assert(
    ("a"_location / ("GET"_method(F {}) | "POST"_method(F {})))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair("GET"_method, handler(F {})),
            hana::make_pair("POST"_method, handler(F {}))
        ))
    )),
    "composition of operators / and | should produce pair with nested tuple"
);

static_assert(
    ("a"_location / ("GET"_method(F {}) | "POST"_method(F {}) | "DELETE"_method(F {})))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair("DELETE"_method, handler(F {})),
            hana::make_pair("GET"_method, handler(F {})),
            hana::make_pair("POST"_method, handler(F {}))
        ))
    )),
    "operator | should produce plain tuple for any number of arguments"
);

static_assert(
    ("a"_location / "b"_location)
    == tree(hana::make_tuple(hana::make_pair("a"_location, "b"_location))),
    "operator / produce pair for two locations"
);

static_assert(
    ("a"_location / "b"_location / "POST"_method)
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair("b"_location, "POST"_method)
        ))
    )),
    "operator / produce nested pairs for any number of arguments"
);

static_assert(
    ("a"_location / "b"_location / "GET"_method(F {}))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair("b"_location, hana::make_tuple(
                hana::make_pair("GET"_method, handler(F {}))
            ))
        ))
    )),
    "handler is put at bottom"
);

static_assert(
    ("a"_location / parameter<X>)
    == tree(hana::make_tuple(hana::make_pair("a"_location, parameter<X>))),
    "operator / produce pair for location and parameter"
);

static_assert(
    ("a"_location / parameter<X> / "GET"_method)
    == tree(hana::make_tuple(hana::make_pair("a"_location,
        hana::make_tuple(hana::make_pair(parameter<X>, "GET"_method)))
    )),
    "operator / produce pair for parameter and location"
);

static_assert(
    ("a"_location / parameter<X> / "GET"_method(F2 {}))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair(parameter<X>, hana::make_tuple(
                hana::make_pair("GET"_method, handler(F2 {}))
            ))
        ))
    )),
    ""
);

static_assert(
    (parameter<X> / parameter<Y> / "GET"_method(F3 {}))
    == tree(hana::make_tuple(
        hana::make_pair(parameter<X>, hana::make_tuple(
            hana::make_pair(parameter<Y>, hana::make_tuple(
                hana::make_pair("GET"_method, handler(F3 {}))
            ))
        ))
    )),
    ""
);

static_assert(
    ("a"_location / parameter<X> / "b"_location / "GET"_method(F2 {}))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair(parameter<X>, hana::make_tuple(
                hana::make_pair("b"_location, hana::make_tuple(
                    hana::make_pair("GET"_method, handler(F2 {}))
                ))
            ))
        ))
    )),
    ""
);

static_assert(
    ("a"_location / parameter<X> / ("GET"_method(F2 {}) | "POST"_method(F2 {})))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair(parameter<X>, hana::make_tuple(
                hana::make_pair("GET"_method, handler(F2 {})),
                hana::make_pair("POST"_method, handler(F2 {}))
            ))
        ))
    )),
    ""
);

static_assert(
    ("b"_location / "GET"_method(F {}) | "c"_location / "GET"_method(F {}))
    == tree(hana::make_tuple(
        hana::make_pair("b"_location, hana::make_tuple(
            hana::make_pair("GET"_method, handler(F {}))
        )),
        hana::make_pair("c"_location, hana::make_tuple(
            hana::make_pair("GET"_method, handler(F {}))
        ))
    )),
    ""
);

static_assert(
    ("a"_location / parameter<X> / ("b"_location / "GET"_method(F2 {})
        | "c"_location / "GET"_method(F2 {})))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair(parameter<X>, hana::make_tuple(
                hana::make_pair("b"_location, hana::make_tuple(
                    hana::make_pair("GET"_method, handler(F2 {}))
                )),
                hana::make_pair("c"_location, hana::make_tuple(
                    hana::make_pair("GET"_method, handler(F2 {}))
                ))
            ))
        ))
    )),
    ""
);

static_assert(
    ("a"_location / parameter<X> / "b"_location / parameter<Y> / "GET"_method(F3 {}))
    == tree(hana::make_tuple(
        hana::make_pair("a"_location, hana::make_tuple(
            hana::make_pair(parameter<X>, hana::make_tuple(
                hana::make_pair("b"_location, hana::make_tuple(
                    hana::make_pair(parameter<Y>, hana::make_tuple(
                        hana::make_pair("GET"_method, handler(F3 {}))
                    ))
                ))
            ))
        ))
    )),
    ""
);

struct TestServerRouterRoute : Test {};

struct OnlyMove {
    constexpr OnlyMove() = default;
    constexpr OnlyMove(const OnlyMove&) = delete;
    constexpr OnlyMove(OnlyMove&&) = default;
};

bool operator ==(const OnlyMove&, const OnlyMove&) {
    return true;
}

struct CallbackMock {
    MOCK_METHOD(void, call, (), (const));
};

struct IntResultCallbackMock {
    MOCK_METHOD(int, call, (), (const));
};

struct XCallbackMock {
    MOCK_METHOD(void, call, (X), (const));
};

struct XYCallbackMock {
    MOCK_METHOD(void, call, (X, Y), (const));
};

struct MonostateCallbackMock {
    MOCK_METHOD(void, call, (Monostate), (const));
};

struct HandlerWithoutArgs {
    std::shared_ptr<StrictMock<CallbackMock>> impl = std::make_shared<StrictMock<CallbackMock>>();

    OnlyMove operator ()() const {
        impl->call();
        return OnlyMove {};
    }
};

struct HandlerX {
    std::shared_ptr<StrictMock<XCallbackMock>> impl = std::make_shared<StrictMock<XCallbackMock>>();

    OnlyMove operator ()(X x) const {
        impl->call(x);
        return OnlyMove {};
    }
};

struct HandlerXY {
    std::shared_ptr<StrictMock<XYCallbackMock>> impl = std::make_shared<StrictMock<XYCallbackMock>>();

    OnlyMove operator ()(X x, Y y) const {
        impl->call(x, y);
        return OnlyMove {};
    }
};

struct HandlerWithoutArgsWithIntResult {
    std::shared_ptr<StrictMock<IntResultCallbackMock>> impl = std::make_shared<StrictMock<IntResultCallbackMock>>();

    int operator ()() const {
        return impl->call();
    }
};

struct HandlerWithoutArgsWithVoidResult {
    std::shared_ptr<StrictMock<CallbackMock>> impl = std::make_shared<StrictMock<CallbackMock>>();

    void operator ()() const {
        return impl->call();
    }
};

struct HandlerMonostate {
    std::shared_ptr<StrictMock<MonostateCallbackMock>> impl = std::make_shared<StrictMock<MonostateCallbackMock>>();

    void operator ()(Monostate x) const {
        return impl->call(x);
    }
};

enum class Location {
    a,
};

using UriPath = std::vector<std::string>;
using Token = std::variant<Location, std::string>;
using Tokens = std::vector<Token>;
using OnlyMoveResult = std::variant<OnlyMove, Error>;
using MonostateResult = std::variant<std::variant<std::monostate, std::string_view>, Error>;

TEST(TestServerRouterRoute, for_empty_tree_should_return_error) {
    const Tree<hana::tuple<>> tree {};
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), ""_s), OnlyMoveResult(Error::method_not_found));
}

TEST(TestServerRouterRoute, for_empty_tree_and_not_empty_location_should_return_error) {
    const Tree<hana::tuple<>> tree {};
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), ""_s), OnlyMoveResult(Error::location_not_found));
}

TEST(TestServerRouterRoute, for_tree_with_method_and_same_method_should_call_handler) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return());
    const auto tree = make_method("GET"_s)(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, for_tree_with_method_and_same_method_should_call_handler_and_return_void) {
    const HandlerWithoutArgsWithVoidResult handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return());
    const auto tree = make_method("GET"_s)(handler);
    EXPECT_EQ(route<void>(tree, UriPath(), "GET"_s), (std::variant<std::monostate, Error>(std::monostate {})));
}

TEST(TestServerRouterRoute, for_tree_with_method_and_same_method_should_call_handler_and_return_value_using_map_router) {
    const HandlerWithoutArgsWithIntResult handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return(42));
    const auto tree = make_method("GET"_s)(handler);
    EXPECT_EQ(route<int>(tree, UriPath(), "GET"_s), (std::variant<int, Error>(42)));
}

TEST(TestServerRouterRoute, for_tree_with_only_method_but_different_method_should_return_error) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).Times(0);
    const auto tree = "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), "POST"_s), OnlyMoveResult(Error::method_not_found));
}

TEST(TestServerRouterRoute, for_tree_with_only_method_but_not_empty_location_should_return_error) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).Times(0);
    const auto tree = "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(Error::location_not_found));
}

TEST(TestServerRouterRoute, for_tree_with_location_and_method_should_call_handler_by_location_and_method) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return());
    const auto tree = "a"_location / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, for_tree_with_location_and_middleware_should_call_middleware_with_continuation_and_args) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return());
    const auto m = [] (auto&& next, auto&& params, auto&& ... args) {
        return next(std::move(params), std::move(args) ...);
    };
    const auto tree = "a"_location / middleware(m) / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, use_first_variant_type_as_location) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).WillOnce(Return());
    const auto tree = location<std::integral_constant<Location, Location::a>> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, Tokens({Token(Location::a)}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, second_variant_type_cant_be_location) {
    const HandlerWithoutArgs handler;
    const auto tree = location<std::integral_constant<Location, Location::a>> / "GET"_method(handler);
    EXPECT_EQ(
        route<OnlyMove>(tree, Tokens({Token(std::string("foo"))}), "GET"_s),
        OnlyMoveResult(Error::location_not_found)
    );
}

TEST(TestServerRouterRoute, for_tree_with_location_and_method_but_invalid_path_should_return_error) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).Times(0);
    const auto tree = "a"_location / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"b"}), "GET"_s), OnlyMoveResult(Error::location_not_found));
}

TEST(TestServerRouterRoute, for_tree_with_parameter_and_method_should_call_handler_with_set_parameter) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto tree = parameter<X> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"foo"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, for_tree_with_parameter_and_middleware_should_call_middleware_with_continuation_and_args) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto m = [] (auto&& next, auto&& params, auto&& ... args) {
        return next(std::move(params), std::move(args) ...);
    };
    const auto tree = parameter<X> / middleware(m) / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"foo"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, for_tree_with_middleware_and_parameter_after_should_call_middleware_with_continuation_and_args) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto m = [] (auto&& next, auto&& params, auto&& ... args) {
        return next(std::move(params), std::move(args) ...);
    };
    const auto tree = middleware(m) / parameter<X> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"foo"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, use_second_variant_type_as_parameter) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto tree = parameter<X> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, Tokens({Token(std::string("foo"))}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, first_variant_type_cant_be_parameter) {
    const HandlerX handler;
    const auto tree = parameter<X> / "GET"_method(handler);
    EXPECT_EQ(
        route<OnlyMove>(tree, Tokens({Token(Location::a)}), "GET"_s),
        OnlyMoveResult(Error::location_not_found)
    );
}

TEST(TestServerRouterRoute, location_can_be_before_parameter) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto tree = "a"_location / parameter<X> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "foo"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, location_can_be_after_parameter) {
    const HandlerX handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"})).WillOnce(Return());
    const auto tree = parameter<X> / "a"_location / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"foo", "a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, handler_should_have_all_arguments_from_parameters_in_path_to_it) {
    const HandlerXY handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"}, Y {"bar"})).WillOnce(Return());
    const auto tree = parameter<X> / parameter<Y> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"foo", "bar"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, handler_has_being_selected_by_method) {
    const HandlerWithoutArgs handler_get;
    const HandlerWithoutArgs handler_post;
    const HandlerWithoutArgs handler_delete;
    const InSequence s;
    EXPECT_CALL(*handler_post.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handler_get.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handler_delete.impl, call()).WillOnce(Return());
    const auto tree =
        "POST"_method(handler_post)
        | "GET"_method(handler_get)
        | "DELETE"_method(handler_delete)
    ;
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), "POST"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath(), "DELETE"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, handler_has_being_selected_by_location) {
    const HandlerWithoutArgs handlerA;
    const HandlerWithoutArgs handlerB;
    const HandlerWithoutArgs handlerC;
    const InSequence s;
    EXPECT_CALL(*handlerA.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerB.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerC.impl, call()).WillOnce(Return());
    const auto tree =
        ("a"_location / "GET"_method(handlerA))
        | ("b"_location / "GET"_method(handlerB))
        | ("c"_location / "GET"_method(handlerC))
    ;
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"b"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"c"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, for_longer_path_should_return_error) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).Times(0);
    const auto tree = "a"_location / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "b"}), "GET"_s), OnlyMoveResult(Error::location_not_found));
}

TEST(TestServerRouterRoute, for_shorter_path_should_return_error) {
    const HandlerWithoutArgs handler;
    EXPECT_CALL(*handler.impl, call()).Times(0);
    const auto tree = "a"_location / "b"_location / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(Error::method_not_found));
}

TEST(TestServerRouterRoute, support_nested_selection) {
    const HandlerWithoutArgs handlerAAGet;
    const HandlerWithoutArgs handlerAAPost;
    const HandlerWithoutArgs handlerABGet;
    const HandlerWithoutArgs handlerABPost;
    const InSequence s;
    EXPECT_CALL(*handlerAAGet.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerAAPost.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerABGet.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerABPost.impl, call()).WillOnce(Return());
    const auto tree =
        "a"_location / (
            "a"_location / (
                "GET"_method(handlerAAGet)
                | "POST"_method(handlerAAPost)
            )
            | "b"_location / (
                "GET"_method(handlerABGet)
                | "POST"_method(handlerABPost)
            )
        )
    ;
    static_assert(
        std::is_same_v<
            std::decay_t<decltype(tree)>,
            decltype(router::tree(hana::make_tuple(
                hana::make_pair("a"_location, hana::make_tuple(
                    hana::make_pair("a"_location, hana::make_tuple(
                        hana::make_pair("GET"_method, handler(HandlerWithoutArgs {})),
                        hana::make_pair("POST"_method, handler(HandlerWithoutArgs {}))
                    )),
                    hana::make_pair("b"_location, hana::make_tuple(
                        hana::make_pair("GET"_method, handler(HandlerWithoutArgs {})),
                        hana::make_pair("POST"_method, handler(HandlerWithoutArgs {}))
                    ))
                ))
            )))
        >,
        ""
    );
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "a"}), "POST"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "b"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "b"}), "POST"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, support_selecting_between_different_types) {
    const HandlerWithoutArgs handlerAAGet;
    const HandlerX handlerAXGet;
    const HandlerWithoutArgs handlerAGet;
    const InSequence s;
    EXPECT_CALL(*handlerAAGet.impl, call()).WillOnce(Return());
    EXPECT_CALL(*handlerAXGet.impl, call(X {"foo"})).WillOnce(Return());
    EXPECT_CALL(*handlerAGet.impl, call()).WillOnce(Return());
    const auto tree =
        "a"_location / (
            "b"_location / "GET"_method(handlerAAGet)
            | parameter<X> / "GET"_method(handlerAXGet)
            | "GET"_method(handlerAGet)
        )
    ;
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "b"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "foo"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a"}), "GET"_s), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, should_pass_additional_args_to_handler) {
    const HandlerXY handler;
    EXPECT_CALL(*handler.impl, call(X {"foo"}, Y {"bar"})).WillOnce(Return());
    const auto tree = "a"_location / parameter<X> / "GET"_method(handler);
    EXPECT_EQ(route<OnlyMove>(tree, UriPath({"a", "foo"}), "GET"_s, Y {"bar"}), OnlyMoveResult(OnlyMove {}));
}

TEST(TestServerRouterRoute, support_error_code_return_from_parameter_make) {
    const HandlerMonostate handler;
    EXPECT_CALL(*handler.impl, call(Monostate {std::monostate {}})).WillOnce(Return());
    const auto tree = parameter<Monostate> / "GET"_method(handler);
    EXPECT_EQ(
        (route<std::variant<std::monostate, std::string_view>>(tree, UriPath({"monostate"}), "GET"_s)),
        MonostateResult(std::variant<std::monostate, std::string_view>(std::monostate {}))
    );
}

TEST(TestServerRouterRoute, should_return_error_when_parameter_make_return_error) {
    const HandlerMonostate handler;
    const auto tree = parameter<Monostate> / "GET"_method(handler);
    EXPECT_EQ(
        (route<std::variant<std::monostate, std::string_view>>(tree, UriPath({"foo"}), "GET"_s)),
        MonostateResult(std::variant<std::monostate, std::string_view>("invalid monostate value"))
    );
}

} // namespace
