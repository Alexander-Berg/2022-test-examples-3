package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CppStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "__cxa_throw+307 (0x161B5AB3)\n" +
                "??+0 (0x1C66922F)\n" +
                "NTskvFormat::NDetail::DeserializeKvToStrings(TBasicStringBuf<char, std::__y1::char_traits<char> > " +
                "const&, TBasicString<char, std::__y1::char_traits<char> >&, TBasicString<char, " +
                "std::__y1::char_traits<char> >&, bool)+54 (0x1C6692E6)\n" +
                "TNewsWizardFromQuickUpperRearrangeContext::MoveWizardUpIfTrending" +
                "(IMetaRearrangeContext::TRearrangeParams&)+591 (0x1572647F)\n" +
                "TNewsWizardFromQuickUpperRearrangeContext::DoRearrangeAfterFetch" +
                "(IMetaRearrangeContext::TRearrangeParams&)+5404 (0x15721EAC)\n" +
                "NRearr::TWebRearrangeContext::DoUnsafeRearrange(NRearr::IRearrRuleWrapper const&, " +
                "IMetaRearrangeContext::TRearrangeParams&)+825 (0x18AC9E09)\n",
            new StackFrame("__cxa_throw+307", "", 0, 0),
            new StackFrame("??+0", "", 0, 0),
            new StackFrame("NTskvFormat::NDetail::DeserializeKvToStrings", "", 0, 0),
            new StackFrame("TNewsWizardFromQuickUpperRearrangeContext::MoveWizardUpIfTrending", "", 0, 0),
            new StackFrame("TNewsWizardFromQuickUpperRearrangeContext::DoRearrangeAfterFetch", "", 0, 0),
            new StackFrame("NRearr::TWebRearrangeContext::DoUnsafeRearrange", "", 0, 0)
        ),

        new TestData(
            "__cxa_throw+307 (0x161B5AB3)\n" +
                "TSafeVector<TMetaGroup*, std::__y1::allocator<TMetaGroup*> >::CheckIter(TMetaGroup**) const+1123 " +
                "(0x14BF0943)\n" +
                "??+0 (0x15B37DD4)\n" +
                "NThreading::NImpl::TFutureState<bool>::Wait (0x15B37DD4)\n" +
                "NRearr::TWebRearrangeContext::DoUnsafeRearrange(NRearr::IRearrRuleWrapper const&, " +
                "IMetaRearrangeContext::TRearrangeParams&)+825 (0x18AC9E09)\n",
            new StackFrame("__cxa_throw+307", "", 0, 0),
            new StackFrame("TSafeVector<TMetaGroup*, std::__y1::allocator<TMetaGroup*> >::CheckIter", "", 0, 0),
            new StackFrame("??+0", "", 0, 0),
            new StackFrame("NThreading::NImpl::TFutureState<bool>::Wait", "", 0, 0),
            new StackFrame("NRearr::TWebRearrangeContext::DoUnsafeRearrange", "", 0, 0)
        ),

        // Empty
        new TestData(
            ""
        ),
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new CppStackParser(stack).getStackFrames();
            assertArrayEquals(
                frames,
                actualFrames,
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message"
            );
        }
    }
}
