#pragma once
#include <market/sre/services/cpp_test_service/env/env.h>
#include <market/library/shiny/server/request.h>

namespace NMarket::CppTestService {
    /// Describes how to process /hello user request
    class THello {
    public:
        /// Describes an incoming request to /hello, which holds parsed CGI-params, body, etc
        struct TRequest {
            /// Declaration of possible input arguments
            static void Declare(NShiny::TCgiInputMetadata<TRequest>& args);

            /// Extended request info
            void SetExtensions(const NShiny::TRequestExtensions&);

            /// Filled on user request, ex: /hello?name=vladimir-vladimirovich
            TString Username;
        };

        /// Describes form and content of answer
        struct TResponse {
            /// Output format as json
            NSc::TValue AsJson() const;

            /// Output format as plain text
            TString AsText() const;

            TString Username;
        };

        /// Short description of handler for help
        static TStringBuf Describe();

        /// Template environment is needed for break dependency of handler from heavy env struct
        template <typename TEnv>
        explicit THello(const TEnv&) {}

        /// Request processing
        TResponse Run(const TRequest& request) const;
    };
}
