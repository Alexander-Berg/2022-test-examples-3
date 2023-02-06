#pragma once

#include <io_result/io_result.h>


namespace ymod_taskmaster::testing {

template<class Range, class Callback>
struct SequenceResponse {
    std::shared_ptr<Range> range;
    Callback cb;
    mutable typename Range::const_iterator iter;

    SequenceResponse(Range r, Callback cb)
        : range(std::make_shared<Range>(r))
        , cb(cb)
        , iter(range->begin())
    { }

    void operator()() const {
        if (iter != range->end()) {
            cb(
                mail_errors::error_code(),
                io_result::hooks::detail::Cursor(*iter++, *this)
            );
        } else {
            cb(mail_errors::error_code());
        }
    }
};

template<class Range, class Callback>
void responseAsSequence(Range r, Callback cb) {
    SequenceResponse sr(r, cb);
    sr();
}

}