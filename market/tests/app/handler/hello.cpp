#include <market/library/shiny/server/gen/lib/tests/app/handler/hello.h>
#include <market/library/shiny/log/level.h>

using namespace NMarket::NApp;

void THello::TRequest::Declare(NShiny::TCgiInputMetadata<TRequest>& args) {
    /// Declaration of possible input arguments
    args.Required("name", "name of user", &TRequest::Username);
}

void THello::TRequest::SetExtensions(const NShiny::TRequestExtensions&) {
    /// Till this moment all fields are filled from corresponding arguments
    /// You can perform some additional checks here and fill the body
    /// This method is not required: you are free to omit it if you don't need its argument data
}

NSc::TValue THello::TResponse::AsJson() const {
    NSc::TValue result;
    result["greetings"] = TString::Join("Hello, ", Username, "!");
    return result;
}

TString THello::TResponse::AsText() const {
    return TString::Join("Hello, ", Username, "!");
}

TStringBuf THello::Describe() {
    return "Say hello to given user";
}

THello::TResponse THello::Run(const TRequest& request) const {
    /// On each user request this method will be invoked, but its handler class (THello)
    /// stays alive along daemon works

    /// Logging is accessible through singleton
    NMarket::NShiny::Log().Info() << "Someone wants to say hello to " << request.Username;
    return {request.Username};
}
