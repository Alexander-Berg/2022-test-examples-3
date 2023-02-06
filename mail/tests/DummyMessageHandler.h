#ifndef EDP_DUMMY_MESSAGE_HANDLER
#define EDP_DUMMY_MESSAGE_HANDLER

template <class ForwardTraversalIterator>
class DummyMessageHandler {
public:
    typedef ForwardTraversalIterator Iterator;
    typedef boost::iterator_range<Iterator> Range;
public:
    DummyMessageHandler()
        : m_partNumber(0)
    {}
    bool beginMessage(const Iterator& /*position*/) {
        return true;
    }
    bool endMessage(const Iterator& /*position*/) {
        return true;
    }

    bool beginPart(const Iterator& /*position*/,
                   const HeaderHandler<Iterator>& /*headerHandler*/) {
        ++m_partNumber;
        return true;
    }
    bool endPart(const Iterator& /*position*/) {
        return true;
    }

    bool beginHeader(const Iterator& /*position*/) {
        return true;
    }
    bool endHeader(const Iterator& /*position*/) {
        return true;
    }
    bool handleBodyLine(const Range& /*position*/) {
        return true;
    }
    void addError(const std::string&) {
    }
public:
    unsigned int partNumber() {
        return m_partNumber;
    }
private:
    unsigned int m_partNumber;
};

#endif
