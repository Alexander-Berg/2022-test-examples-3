#ifndef MAIL_QUOTE_TEST_VERIFIED_VISITOR_H
#define MAIL_QUOTE_TEST_VERIFIED_VISITOR_H

#include <internal/mail_quote/visitor.h>

#include "base_verified_visitor.h"

namespace msg_body {
namespace mail_quote {

class VerifiedVisitor : public Visitor, public BaseVerifiedVisitor {
public:
    using Base = Visitor;

    VerifiedVisitor(AbstractParagraphVisitor& paragraphVisitor)
        : Visitor(paragraphVisitor), BaseVerifiedVisitor(false) {}

    void visit(int quotationLevel, Iterator current) override final {
        beforeVisit(__PRETTY_FUNCTION__);
        EXPECT_EQ(quotationLevel, 0);
        Base::visit(quotationLevel, current);
    }

    void leave(int quotationLevel, const Range &range) override final {
        EXPECT_EQ(quotationLevel, 0);
        EXPECT_TRUE(range.begin() <= range.end());
        Base::leave(quotationLevel, range);
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitQuotation(int quotationLevel, Iterator current) override final {
        EXPECT_GT(quotationLevel, 0);
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitQuotation(quotationLevel, current);
    }

    void leaveQuotation(int quotationLevel, const Range& range) override final {
        EXPECT_GT(quotationLevel, 0);
        EXPECT_TRUE(range.begin() < range.end());
        Base::leaveQuotation(quotationLevel, range);
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitText(int quotationLevel, Iterator current) override final {
        EXPECT_EQ(quotationLevel, 0);
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitText(quotationLevel, current);
    }

    void leaveText(int quotationLevel, const Range& range) override final {
        EXPECT_EQ(quotationLevel, 0);
        EXPECT_TRUE(range.begin() < range.end());
        Base::leaveText(quotationLevel, range);
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitSignature(int quotationLevel, Iterator current) override final {
        EXPECT_EQ(quotationLevel, 0);
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitSignature(quotationLevel, current);
    }

    void leaveSignature(int quotationLevel, const Range& range) override final {
        EXPECT_EQ(quotationLevel, 0);
        EXPECT_TRUE(range.begin() < range.end());
        Base::leaveSignature(quotationLevel, range);
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitSymbol(int quotationLevel, Iterator current) override final {
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitSymbol(quotationLevel, current);
    }

    void leaveSymbol(int quotationLevel, const Range& range) override final {
        EXPECT_EQ(range.end() - range.begin(), 1);
        Base::leaveSymbol(quotationLevel, range);
        afterLeave(__PRETTY_FUNCTION__);
    }
};

} // namespace mail_quote
} // namespace msg_body

#endif // MAIL_QUOTE_TEST_VERIFIED_VISITOR_H
