#ifndef MAIL_QUOTE_TEST_VERIFIED_PARAGRAPH_VISITOR_H
#define MAIL_QUOTE_TEST_VERIFIED_PARAGRAPH_VISITOR_H

#include <internal/mail_quote/paragraph_visitor.h>

#include "base_verified_visitor.h"

namespace msg_body {
namespace mail_quote {

class VerifiedParagraphVisitor : public ParagraphVisitor, public BaseVerifiedVisitor {
public:
    using Base = ParagraphVisitor;

    VerifiedParagraphVisitor(Iterator end)
        : ParagraphVisitor(end), BaseVerifiedVisitor(false), end(end) {}

    void visit() override final {
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visit();
    }

    void leave() override final {
        Base::leave();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitParagraph() override final {
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitParagraph();
    }

    void leaveParagraph() override final {
        EXPECT_FALSE(getParagraphsStack().empty());
        Base::leaveParagraph();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitQuotation() override final {
        EXPECT_FALSE(getParagraphsStack().empty());
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitQuotation();
    }

    void leaveQuotation() override final {
        Base::leaveQuotation();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitText() override final {
        EXPECT_FALSE(getParagraphsStack().empty());
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitText();
    }

    void leaveText() override final {
        Base::leaveText();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitSignature() override final {
        ASSERT_FALSE(getParagraphsStack().empty());
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitSignature();
    }

    void leaveSignature() override final {
        Base::leaveSignature();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitLine() override final {
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitLine();
    }

    void leaveLine() override final {
        EXPECT_FALSE(getParagraphsStack().empty());
        Base::leaveLine();
        afterLeave(__PRETTY_FUNCTION__);
    }

    void visitSymbol(Iterator iterator) override final {
        EXPECT_TRUE(iterator < end);
        beforeVisit(__PRETTY_FUNCTION__);
        Base::visitSymbol(iterator);
    }

    void leaveSymbol(const Range& range) override final {
        EXPECT_EQ(range.end() - range.begin(), 1);
        Base::leaveSymbol(range);
        afterLeave(__PRETTY_FUNCTION__);
    }

private:
    const Iterator end;
};

} // namespace mail_quote
} // namespace msg_body

#endif // MAIL_QUOTE_TEST_VERIFIED_PARAGRAPH_VISITOR_H
