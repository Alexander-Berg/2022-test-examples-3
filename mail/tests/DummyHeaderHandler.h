#ifndef EDP_DUMMY_HEADER_HANDLER
#define EDP_DUMMY_HEADER_HANDLER

template <class ForwardTraversalIterator>
class DummyHeaderHandler {
public:
    typedef ForwardTraversalIterator Iterator;
    typedef boost::iterator_range<Iterator> Range;
public:
    DummyHeaderHandler()
        : m_headerFieldCount(0)
    {}
    bool beginHeader(const Iterator& /*it*/) {
        //std::cerr << " == BEGIN HEADER ==" << std::endl;
        return true;
    }
    bool endHeader(const Iterator& /*it*/) {
        //std::cerr << " == END HEADER ==" << std::endl;
        return true;
    }
    bool headerField(const Range& /*range*/, const Range& /*eol*/) {
        //std::cerr << "HeaderField: [ " << std::endl;
        //std::cerr << range << std::endl;
        //std::cerr << eol << std::endl;
        //std::cerr << "]" << std::endl;
        ++m_headerFieldCount;
        return true;
    }
    unsigned int headerFieldCount() const {
        return m_headerFieldCount;
    }
private:
    unsigned int m_headerFieldCount;
};

#endif
