#include <backend/backend.h>

#include <sstream>

using namespace yimap;
using namespace yimap::backend;

struct TestSearchBackend : SearchBackend
{
    struct Request
    {
        string searchRequest;
        string originalRequest;
        string fid;
    };

    std::pair<ErrorCode, std::set<string>> response = {
        ymod_httpclient::http_error::code::server_status_error, // XXX
        {}
    };
    std::vector<Request> requests;

    void search(
        const string& searchRequest,
        const string& originalRequest,
        const string& fid,
        const SearchCallback& cb) override
    {
        requests.push_back(Request{ searchRequest, originalRequest, fid });
        cb(response.first, response.second);
    }
};