package ru.yandex.market.logshatter.parser.front.helpers;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.KeyValueExtractor;
import ru.yandex.market.logshatter.parser.ParserException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StackTraceProcessorTest {
    @Test
    public void processAndroidLong() throws ParserException {
        String stackTrace = "java.lang.IllegalStateException: ViewHolder views must not be attached when created. " +
            "Ensure that you are not passing \\'true\\' to the attachToRoot parameter of LayoutInflater.inflate(" +
            "..., boolean attachToRoot)\\n\\tat android.support.v7.widget.RecyclerView$Adapter.createViewHolder(" +
            "RecyclerView.java:6796)\\n\\tat android.support.v7.widget.RecyclerView$Recycler.tryGetViewHolderForP" +
            "ositionByDeadline(RecyclerView.java:5975)\\n\\tat android.support.v7.widget.GapWorker.prefetchPosi" +
            "tionWithDeadline(GapWorker.java:286)\\n\\tat android.support.v7.widget.GapWorker.flushTaskWithDeadli" +
            "ne(GapWorker.java:343)\\n\\tat android.support.v7.widget.GapWorker.flushTasksWithDeadline(GapWorker" +
            ".java:359)\\n\\tat android.support.v7.widget.GapWorker.prefetch(GapWorker.java:366)\\n\\tat android." +
            "support.v7.widget.GapWorker.run(GapWorker.java:397)\\n\\tat android.os.Handler.handleCallback(Handle" +
            "r.java:873)\\n\\tat android.os.Handler.dispatchMessage(Handler.java:99)\\n\\tat android.os.Looper.lo" +
            "op(Looper.java:214)\\n\\tat android.app.ActivityThread.main(ActivityThread.java:6981)\\n\\tat java.la" +
            "ng.reflect.Method.invoke(Native Method)\\n\\tat com.android.internal.os.RuntimeInit$MethodAndArgsCal" +
            "ler.run(RuntimeInit.java:493)\\n\\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1445)\\n";

        KeyValueExtractor processedStackTrace = StackTraceProcessor.processStackTrace(stackTrace, "android");

        assertEquals("java.lang.IllegalStateException", processedStackTrace.getString("code"));
        assertEquals("ViewHolder views must not be attached when created. Ensure that you are not passin" +
                "g \\'true\\' to the attachToRoot parameter of LayoutInflater.inflate(..., boolean attachToRoot)",
            processedStackTrace.getString("message"));
        assertEquals("RecyclerView.java", processedStackTrace.getString("file"));
        assertEquals("6796", processedStackTrace.getString("line"));
    }

    @Test
    public void processAndroidLongWithoutMessage() throws ParserException {
        String stackTrace = "java.lang.NullPointerException\\n\\tat com.yandex.auth.sync.AccountProvide" +
            "rHelper.yandexAccountsFromCursor(AccountProviderHelper." +
            "java:74)\\n\\tat com.yandex.auth.sync.AccountProviderHelper.getAccounts(AccountProviderHel" +
            "per.java:39)\\n\\tat com.yandex.auth.sync.BackupLogic.backup(BackupLogic.java:44)\\n\\tat " +
            "com.yandex.auth.sync.BackupLogic.perform(BackupLogic.java:36)\\n\\tat com.yandex.auth.sync" +
            ".BackupAccountsService.runBackupAction(BackupAccountsService.java:167)\\n\\tat com.yandex." +
            "auth.sync.BackupAccountsService.onHandleIntent(BackupAccountsService.java:54)\\n\\tat andr" +
            "oid.app.IntentService$ServiceHandler.handleMessage(IntentService.java:65)\\n\\tat android." +
            "os.Handler.dispatchMessage(Handler.java:110)\\n\\tat android.os.Looper.loop(Looper.java:19" +
            "3)\\n\\tat android.os.HandlerThread.run(HandlerThread.java:61)\\n";

        KeyValueExtractor processedStackTrace = StackTraceProcessor.processStackTrace(stackTrace, "android");

        assertEquals("java.lang.NullPointerException", processedStackTrace.getString("code"));
        assertNull(processedStackTrace.getString("message"));
        assertEquals("AccountProviderHelper.java", processedStackTrace.getString("file"));
        assertEquals("74", processedStackTrace.getString("line"));
    }

    @Test
    public void processAndroidShort() throws ParserException {
        String stackTrace = "java.lang.NullPointerException: OutOfMemoryError thrown while trying to throw OutOfMemor" +
            "yError; no stack trace available\\n";

        KeyValueExtractor processedStackTrace = StackTraceProcessor.processStackTrace(stackTrace, "android");

        assertEquals("java.lang.NullPointerException", processedStackTrace.getString("code"));
        assertEquals("OutOfMemoryError thrown while trying to throw OutOfMemoryError",
            processedStackTrace.getString("message"));
        assertEquals("", processedStackTrace.getString("file"));
        assertEquals("0", processedStackTrace.getString("line"));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void processIos() throws ParserException {
        String stackTrace = "Incident Identifier: E3211827-5D2C-4DB6-B6F5-86907CE0C60A\\nCrashReporter Key:   " +
            "ddac2b00" +
            "66200e2f3105d5d84aeab1c85847e86d\\nHardware Model:      iPhone8,1\\nProcess:         Беру " +
            "[1669]\\nPath:            /var/containers/Bundle/Application/0755B820-FF9E-42D9-9DF8-9BAA7" +
            "6FDDD9F/Беру.app/Беру\\nIdentifier:      ru.yandex.blue.market\\nVersion:         1523 (20" +
            "4)\\nCode Type:       ARM-64\\nParent Process:  ? [1]\\n\\nDate/Time:       2018-12-20 01:" +
            "14:20.000 +0300\\nOS Version:      iOS 12.1 (16B92)\\nReport Version:  104\\n\\nException " +
            "Type:  EXC_CRASH (SIGABRT)\\nException Codes: 0x00000000 at 0x0000000000000000\\nCrashed T" +
            "hread:  0\\n\\nApplication Specific Information:\\n*** Terminating app due to uncaught exc" +
            "eption \\'NSRangeException\\', reason: \\'*** -[__NSArray0 objectAtIndex:]: index 2 beyond" +
            " bounds for empty NSArray\\'\\n\\nThread 0 Crashed:\\n0   CoreFoundation                  " +
            "0x000000018d5ffea0 0x18d4e4000 + 1162912 (<redacted> + 228)\\n1   libobjc.A.dylib         " +
            "        0x000000018c7d1a40 0x18c7cb000 + 27200 (objc_exception_throw + 56)\\n2   CoreFound" +
            "ation                  0x000000018d511a90 0x18d4e4000 + 187024 (<redacted> + 108)\\n3   –ë" +
            "–µ—Ä—É                        0x00000001048f30ac 0x10469c000 + 2453676\\n4   UIKitCore    " +
            "                   0x00000001ba8ac540 0x1b9d9d000 + 11597120 (<redacted> + 684)\\n5   UIKi" +
            "tCore                       0x00000001ba8aca88 0x1b9d9d000 + 11598472 (<redacted> + 80)\\n" +
            "6   UIKitCore                       0x00000001ba878160 0x1b9d9d000 + 11383136 (<redacted> " +
            "+ 2256)\\n7   UIKitCore                       0x00000001ba876684 0x1b9d9d000 + 11376260 (<" +
            "redacted> + 224)\\n8   UIKitCore                       0x00000001ba8921cc 0x1b9d9d000 + 11" +
            "489740 (<redacted> + 432)\\n9   –ë–µ—Ä—É                        0x00000001048f6650 0x10469" +
            "c000 + 2467408\\n10  UIKitCore                       0x00000001bab1e8b4 0x1b9d9d000 + 1416" +
            "2100 (<redacted> + 608)\\n11  UIKitCore                       0x00000001bab1eed0 0x1b9d9d0" +
            "00 + 14163664 (<redacted> + 60)\\n12  –ë–µ—Ä—É                        0x00000001048f64f0 0" +
            "x10469c000 + 2467056\\n13  –ë–µ—Ä—É                        0x00000001048f562c 0x10469c000 " +
            "+ 2463276\\n14  –ë–µ—Ä—É                        0x00000001046a7f50 0x10469c000 + 48976\\n1" +
            "5  –ë–µ—Ä—É                        0x00000001049637a0 0x10469c000 + 2914208\\n16  libdispa" +
            "tch.dylib               0x000000018d0396c8 0x18cfd9000 + 394952 (<redacted> + 24)\\n17  li" +
            "bdispatch.dylib               0x000000018d03a484 0x18cfd9000 + 398468 (<redacted> + 16)\\n" +
            "18  libdispatch.dylib               0x000000018cfe69b4 0x18cfd9000 + 55732 (<redacted> + 1" +
            "068)\\n19  CoreFoundation                  0x000000018d58fdd0 0x18d4e4000 + 703952 (<redac" +
            "ted> + 12)\\n20  CoreFoundation                  0x000000018d58ac98 0x18d4e4000 + 683160 (" +
            "<redacted> + 1964)\\n21  CoreFoundation                  0x000000018d58a1cc 0x18d4e4000 + " +
            "680396 (CFRunLoopRunSpecific + 436)\\n22  GraphicsServices                0x000000018f8015" +
            "84 0x18f7f6000 + 46468 (GSEventRunModal + 100)\\n23  UIKitCore                       0x000" +
            "00001ba685054 0x1b9d9d000 + 9338964 (UIApplicationMain + 212)\\n24  –ë–µ—Ä—É              " +
            "          0x0000000104a563f8 0x10469c000 + 3908600\\n25  libdyld.dylib                   0" +
            "x000000018d04abb4 0x18d04a000 + 2996 (<redacted> + 4)\\n\\nThread 1:\\n0   libsystem_kerne" +
            "l.dylib          0x000000018d197428 0x18d174000 + 144424 (__semwait_signal + 8)\\n1   libs" +
            "ystem_c.dylib               0x000000018d10c5d0 0x18d097000 + 480720 (nanosleep + 212)\\n2 " +
            "  libsystem_c.dylib               0x000000018d10c3cc 0x18d097000 + 480204 (sleep + 44)\\n3" +
            "   –ë–µ—Ä—É                        0x00000001056c8708 0x10469c000 + 16959240 (_yandex_impl" +
            "___ZN5boost13serialization16singleton_module8get_lockEv + 6189008)\\n4   libsystem_pthread" +
            ".dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_p" +
            "thread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThr" +
            "ead 2 name:  KSCrash Exception Handler (Secondary)\\nThread 2:\\n0   libsystem_kernel.dyli" +
            "b          0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kern" +
            "el.dylib          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   libsystem_k" +
            "ernel.dylib          0x000000018d1876b8 0x18d174000 + 79544 (thread_suspend + 84)\\n3   –ë" +
            "–µ—Ä—É                        0x00000001056ccfe8 0x10469c000 + 16977896 (_yandex_impl___ZN" +
            "5boost13serialization16singleton_module8get_lockEv + 6207664)\\n4   libsystem_pthread.dyli" +
            "b         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_pthrea" +
            "d.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 3" +
            " name:  KSCrash Exception Handler (Primary)\\nThread 3:\\n0   libsystem_kernel.dylib      " +
            "    0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dyli" +
            "b          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   –ë–µ—Ä—É          " +
            "              0x00000001056cd014 0x10469c000 + 16977940 (_yandex_impl___ZN5boost13serializ" +
            "ation16singleton_module8get_lockEv + 6207708)\\n3   libsystem_pthread.dylib         0x0000" +
            "00018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n4   libsystem_pthread.dylib         " +
            "0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 4 name:  com.appl" +
            "e.NSURLConnectionLoader\\nThread 4:\\n0   libsystem_kernel.dylib          0x000000018d18be" +
            "d0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib          0x0000000" +
            "18d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   CoreFoundation                  0x0000" +
            "00018d58fbc4 0x18d4e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundation                 " +
            " 0x000000018d58aa60 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreFoundation         " +
            "         0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\n5   CFNetw" +
            "ork                       0x000000018dbab834 0x18dbaa000 + 6196 (<redacted> + 212)\\n6   F" +
            "oundation                      0x000000018e0b21ac 0x18df77000 + 1290668 (<redacted> + 1040" +
            ")\\n7   libsystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted>" +
            " + 128)\\n8   libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pth" +
            "read_start + 48)\\n\\nThread 5 name:  com.apple.uikit.eventfetch-thread\\nThread 5:\\n0   " +
            "libsystem_kernel.dylib          0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8" +
            ")\\n1   libsystem_kernel.dylib          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg +" +
            " 72)\\n2   CoreFoundation                  0x000000018d58fbc4 0x18d4e4000 + 703428 (<redac" +
            "ted> + 236)\\n3   CoreFoundation                  0x000000018d58aa60 0x18d4e4000 + 682592 " +
            "(<redacted> + 1396)\\n4   CoreFoundation                  0x000000018d58a1cc 0x18d4e4000 +" +
            " 680396 (CFRunLoopRunSpecific + 436)\\n5   Foundation                      0x000000018df7f" +
            "404 0x18df77000 + 33796 (<redacted> + 300)\\n6   Foundation                      0x0000000" +
            "18df7f2b0 0x18df77000 + 33456 (<redacted> + 148)\\n7   UIKitCore                       0x0" +
            "0000001ba772808 0x1b9d9d000 + 10311688 (<redacted> + 136)\\n8   Foundation                " +
            "      0x000000018e0b21ac 0x18df77000 + 1290668 (<redacted> + 1040)\\n9   libsystem_pthread" +
            ".dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n10  libsystem_p" +
            "thread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThr" +
            "ead 6 name:  JavaScriptCore bmalloc scavenger\\nThread 6:\\n0   libsystem_kernel.dylib    " +
            "      0x000000018d196f0c 0x18d174000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_pthrea" +
            "d.dylib         0x000000018d213cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc++.1.d" +
            "ylib                  0x000000018c7644d0 0x18c75c000 + 34000 (std::__1::condition_variable" +
            "::wait(std::__1::unique_lock<std::__1::mutex>&) + 24)\\n3   JavaScriptCore                " +
            "  0x00000001949329b8 0x1948d6000 + 379320 (<redacted> + 104)\\n4   JavaScriptCore         " +
            "         0x0000000194936aac 0x1948d6000 + 395948 (<redacted> + 176)\\n5   JavaScriptCore  " +
            "                0x00000001949361e0 0x1948d6000 + 393696 (<redacted> + 12)\\n6   JavaScript" +
            "Core                  0x0000000194937c8c 0x1948d6000 + 400524 (<redacted> + 40)\\n7   libs" +
            "ystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n8 " +
            "  libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start +" +
            " 48)\\n\\nThread 7 name:  WebThread\\nThread 7:\\n0   libsystem_kernel.dylib          0x00" +
            "0000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib       " +
            "   0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   CoreFoundation            " +
            "      0x000000018d58fbc4 0x18d4e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundation     " +
            "             0x000000018d58aa60 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreFoundat" +
            "ion                  0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\" +
            "n5   WebCore                         0x00000001963eb52c 0x195f3b000 + 4916524 (<redacted> " +
            "+ 592)\\n6   libsystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<reda" +
            "cted> + 128)\\n7   libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 " +
            "(_pthread_start + 48)\\n\\nThread 8 name:  0#BgLow\\nThread 8:\\n0   libsystem_kernel.dyli" +
            "b          0x000000018d196f0c 0x18d174000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_p" +
            "thread.dylib         0x000000018d213cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc+" +
            "+.1.dylib                  0x000000018c7644d0 0x18c75c000 + 34000 (std::__1::condition_var" +
            "iable::wait(std::__1::unique_lock<std::__1::mutex>&) + 24)\\n3   –ë–µ—Ä—É                 " +
            "       0x00000001056e90e8 0x10469c000 + 17092840 (_yandex_impl___ZN5boost13serialization16" +
            "singleton_module8get_lockEv + 6322608)\\n4   –ë–µ—Ä—É                        0x00000001056" +
            "e8ef4 0x10469c000 + 17092340 (_yandex_impl___ZN5boost13serialization16singleton_module8get" +
            "_lockEv + 6322108)\\n5   –ë–µ—Ä—É                        0x00000001056ead7c 0x10469c000 + " +
            "17100156 (_yandex_impl___ZN5boost13serialization16singleton_module8get_lockEv + 6329924)\\" +
            "n6   libsystem_pthread.dylib  vateFrameworks/EmailCore.framework/EmailCore\\n       0x1b1e" +
            "71000 -        0x1b1e82fff  libGSFontCache.dylib arm64  <85d5e1f6818e3cb789232339838e1ee9>" +
            " /System/Library/PrivateFrameworks/FontServices.framework/libGSFontCache.dylib\\n       0x" +
            "1b1e83000 -        0x1b1eb5fff  libTrueTypeScaler.dylib arm64  <5ce42bac7a6e366a8122f2b649" +
            "31e4c7> /System/Library/PrivateFrameworks/FontServices.framework/libTrueTypeScaler.dylib\\" +
            "n       0x1b28e1000 -        0x1b28e1fff  libmetal_timestamp.dylib arm64  <449f125aff6c3eb" +
            "88930a13d4a4aee08> /System/Library/PrivateFrameworks/GPUCompiler.framework/Libraries/libme" +
            "tal_timestamp.dylib\\n       0x1b39b8000 -        0x1b39bcfff  InternationalSupport arm64 " +
            " <7a90f1cc4432370a817487085008012b> /System/Library/PrivateFrameworks/InternationalSupport" +
            ".framework/InternationalSupport\\n       0x1b4d8a000 -        0x1b4d96fff  PersonaUI arm64" +
            "  <c5f00611a222383a8d5f6a4036b78060> /System/Library/PrivateFrameworks/PersonaUI.framework" +
            "/PersonaUI\\n       0x1b51d2000 -        0x1b51dcfff  SignpostCollection arm64  <e8f8d0540" +
            "30e3960867be808d4a02543> /System/Library/PrivateFrameworks/SignpostCollection.framework/Si" +
            "gnpostCollection\\n       0x1b585a000 -        0x1b5860fff  TextInputUI arm64  <dab1f33343" +
            "533cb9a4910f6f4506abf5> /System/Library/PrivateFrameworks/TextInputUI.framework/TextInputU" +
            "I\\n       0x1b5d8e000 -        0x1b5d91fff  XCTTargetBootstrap arm64  <37a7b5cb51f43833b7" +
            "3284aa6da2b00c> /System/Library/PrivateFrameworks/XCTTargetBootstrap.framework/XCTTargetBo" +
            "otstrap\\n       0x1b5dd4000 -        0x1b5de6fff  libEDR arm64  <0e484330f1ea3c9aa653387f" +
            "dd8e7eea> /System/Library/PrivateFrameworks/libEDR.framework/libEDR\\n       0x1b6839000 -" +
            "        0x1b6846fff  libMobileGestaltExtensions.dylib arm64  <7bb4ccf8882d3955aa725357af0c" +
            "ee21> /usr/lib/libMobileGestaltExtensions.dylib\\n       0x1b6958000 -        0x1b6958fff " +
            " libcharset.1.dylib arm64  <eace303743a83b0883cd2da45d81bb89> /usr/lib/libcharset.1.dylib\\" +
            "n       0x1b7426000 -        0x1b7427fff  libsandbox.1.dylib arm64  <3954b72ed6543ce6a716f" +
            "260d91c03b5> /usr/lib/libsandbox.1.dylib\\n       0x1b7466000 -        0x1b7467fff  liblog" +
            "_network.dylib arm64  <da638a55600c3bf59eb90fe80bcc5f2d> /usr/lib/log/liblog_network.dyli" +
            "b\\n       0x1b7555000 -        0x1b755ffff  AuthenticationServices arm64  <59a53ebd094532" +
            "d3a895238a5143a57f> /System/Library/Frameworks/AuthenticationServices.framework/Authentica" +
            "tionServices\\n       0x1b75e7000 -        0x1b773bfff  CoreServices arm64  <59408d675a473" +
            "3f1952055efefb431cb> /System/Library/Frameworks/CoreServices.framework/CoreServices\\n    " +
            "   0x1b7768000 -        0x1b7781fff  MPSRayIntersector arm64  <45591bc292513bde96378423e15" +
            "be33d> /System/Library/Frameworks/MetalPerformanceShaders.framework/Frameworks/MPSRayInter" +
            "sector.framework/MPSRayIntersector\\n       0x1b77b1000 -        0x1b78ecfff  Network arm6" +
            "4  <676dec9679353807ba5fffaee0721745> /System/Library/Frameworks/Network.framework/Networ" +
            "k\\n       0x1b78f8000 -        0x1b7906fff  ANEServices arm64  <dba54c743b0b336babeb9b33f" +
            "f02a00b> /System/Library/PrivateFrameworks/ANEServices.framework/ANEServices\\n       0x1b" +
            "790b000 -        0x1b790ffff  ASEProcessing arm64  <6beee62c25433808848e9f61e22f04f0> /Sys" +
            "tem/Library/PrivateFrameworks/ASEProcessing.framework/ASEProcessing\\n       0x1b7910000 -" +
            "        0x1b791bfff  AXCoreUtilities arm64  <b333d651debb313ab4c880e8acf5ffb4> /System/Lib" +
            "rary/PrivateFrameworks/AXCoreUtilities.framework/AXCoreUtilities\\n       0x1b7b95000 -   " +
            "     0x1b7cd2fff  AppleMediaServices arm64  <87437326183836c19acfcb50f4700bcf> /System/Lib" +
            "rary/PrivateFrameworks/AppleMediaServices.framework/AppleMediaServices\\n       0x1b7cd300" +
            "0 -        0x1b7ce2fff  AppleNeuralEngine arm64  <6916f5470c053c5baeda7d648f898e60> /Syste" +
            "m/Library/PrivateFrameworks/AppleNeuralEngine.framework/AppleNeuralEngine\\n       0x1b7e6" +
            "c000 -        0x1b7ea2fff  C2 arm64  <8098f48338fc316abd9f88fafda6a7e2> /System/Library/Pr" +
            "ivateFrameworks/C2.framework/C2\\n       0x1b8031000 -        0x1b803cfff  Categories arm6" +
            "4  <fd2162e5f1b13e0c9686fa2500b1d48c> /System/Library/PrivateFrameworks/Categories.framewo" +
            "rk/Categories\\n       0x1b8044000 -        0x1b8165fff  ConfigurationEngineModel arm64  <" +
            "9e9dc40d171737589f6c3822375b5775> /System/Library/PrivateFrameworks/ConfigurationEngineMod" +
            "el.framework/ConfigurationEngineModel\\n       0x1b8284000 -        0x1b829ffff  DoNotDist" +
            "urb arm64  <f48569dc921d37378eda8254d026b633> /System/Library/PrivateFrameworks/DoNotDistu" +
            "rb.framework/DoNotDisturb\\n       0x1b83d7000 -        0x1b842ffff  DocumentManager arm64" +
            "  <07a9044f8ef33f6faaf3630b36701fdd> /System/Library/PrivateFrameworks/DocumentManager.fra" +
            "mework/DocumentManager\\n       0x1b8512000 -        0x1b8516fff  IdleTimerServices arm64 " +
            " <96d09b21c501360bb8af4749d990e1da> /System/Library/PrivateFrameworks/IdleTimerServices.fr" +
            "amework/IdleTimerServices\\n       0x1b8552000 -        0x1b858efff  LocalAuthenticationPr" +
            "ivateUI arm64  <547985e62fc53222b8c4c173608ac430> /System/Library/PrivateFrameworks/LocalA" +
            "uthenticationPrivateUI.framework/LocalAuthenticationPrivateUI\\n       0x1b858f000 -      " +
            "  0x1b85bafff  MetadataUtilities arm64  <34c6506e802b37769d4a1a038547e721> /System/Library" +
            "/PrivateFrameworks/MetadataUtilities.framework/MetadataUtilities\\n       0x1b8b2b000 -   " +
            "     0x1b8b3efff  NewsAnalyticsUpload arm64  <8a25548276bd3e42ba38115a51afc844> /System/Li" +
            "brary/PrivateFrameworks/NewsAnalyticsUpload.framework/NewsAnalyticsUpload\\n       0x1b8b4" +
            "1000 -        0x1b8b92fff  OTSVG arm64  <8f37fe4148de377e8a58c91ccc356dd3> /System/Library" +
            "/PrivateFrameworks/OTSVG.framework/OTSVG\\n       0x1b8b93000 -        0x1b8bb5fff  OnBoar" +
            "dingKit arm64  <ac96657973dc3e7eaeb1241a00087982> /System/Library/PrivateFrameworks/OnBoar" +
            "dingKit.framework/OnBoardingKit\\n       0x1b8c8e000 -        0x1b8cedfff  PhotoFoundation" +
            " arm64  <d652b829d1e93ead9256fe518277ef8e> /System/Library/PrivateFrameworks/PhotoFoundati" +
            "on.framework/PhotoFoundation\\n       0x1b8d41000 -        0x1b8d86fff  PhotosImagingFound" +
            "ation arm64  <b80937ab5b493a7286dba70152bea6d5> /System/Library/PrivateFrameworks/PhotosIm" +
            "agingFoundation.framework/PhotosImagingFoundation\\n       0x1b8dbc000 -        0x1b8dc5ff" +
            "f  PrototypeToolsUI arm64  <8ad3a202ba4632eebfa535f1f95b9382> /System/Library/PrivateFrame" +
            "works/PrototypeToolsUI.framework/PrototypeToolsUI\\n       0x1b8dc6000 -        0x1b8dd9ff" +
            "f  QuickLookSupport arm64  <6ee115a760143d45b89bdfdc88e741a8> /System/Library/PrivateFrame" +
            "works/QuickLookSupport.framework/QuickLookSupport\\n       0x1b8ddc000 -        0x1b8e3cff" +
            "f  ROCKit arm64  <41c33276d267315f92b97e0aa9c744a4> /System/Library/PrivateFrameworks/ROCK" +
            "it.framework/ROCKit\\n       0x1b8fae000 -        0x1b8fdffff  RemoteConfiguration arm64  " +
            "<53b23751fe773357a723ba588c39b3d0> /System/Library/PrivateFrameworks/RemoteConfiguration.f" +
            "ramework/RemoteConfiguration\\n       0x1b8fef000 -        0x1b904bfff  RemoteManagement a" +
            "rm64  <cd0a48d857cd3aef861de1016c170702> /System/Library/PrivateFrameworks/RemoteManagemen" +
            "t.framework/RemoteManagement\\n       0x1b904c000 -        0x1b905efff  RemoteTextInput ar" +
            "m64  <21a7f85c5e6a3896991ac6416c5c74dd> /System/Library/PrivateFrameworks/RemoteTextInput." +
            "framework/RemoteTextInput\\n       0x1b9087000 -        0x1b9121fff  SampleAnalysis arm64 " +
            " <8a12e6cd83fb3b5c81e7b52fae6ea396> /System/Library/PrivateFrameworks/SampleAnalysis.frame" +
            "work/SampleAnalysis\\n       0x1b91fc000 -        0x1b91fcfff  SignpostNotification arm64 " +
            " <5e421f577ebf3de6a7987b67a4340117> /System/Library/PrivateFrameworks/SignpostNotification" +
            ".framework/SignpostNotification\\n       0x1b926b000 -        0x1b9273fff  StatsKit arm64 " +
            " <676c03e847ff36248d82878812f62688> /System/Library/PrivateFrameworks/StatsKit.framework/S" +
            "tatsKit\\n       0x1b9d9d000 -        0x1baecdfff  UIKitCore arm64  <ab782a031ee43c979f4c1" +
            "5cb9751acb3> /System/Library/PrivateFrameworks/UIKitCore.framework/UIKitCore\\n       0x1b" +
            "aece000 -        0x1baed9fff  UIKitServices arm64  <7f48e5cb504b386892b7dee2480d9d75> /Sys" +
            "tem/Library/PrivateFrameworks/UIKitServices.framework/UIKitServices\\n       0x1baeda000 -" +
            "        0x1baee1fff  URLFormatting arm64  <3a3381bad32a33d3bd9164ab72dcbcad> /System/Libra" +
            "ry/PrivateFrameworks/URLFormatting.framework/URLFormatting\\n       0x1baee2000 -        0" +
            "x1baf04fff  UsageTracking arm64  <7689edf41080312b8b870d4a495aaea5> /System/Library/Privat" +
            "eFrameworks/UsageTracking.framework/UsageTracking\\n\\nExtra Information:\\n\\nApplication" +
            " Stats:\\n{\\n    \"active_time_since_last_crash\": 2225.62,\\n    \"active_time_since_lau" +
            "nch\": 2225.62,\\n    \"application_active\": true,\\n    \"application_in_foreground\": t" +
            "rue,\\n    \"background_time_since_last_crash\": 27.1647,\\n    \"background_time_since_la" +
            "unch\": 27.1647,\\n    \"launches_since_last_crash\": 1,\\n    \"sessions_since_last_cras" +
            "h\": 4,\\n    \"sessions_since_launch\": 4\\n}\\n\\nCrashDoctor Diagnosis: Application thr" +
            "ew exception NSRangeException: *** -[__NSArray0 objectAtIndex:]: index 2 beyond bounds for" +
            " empty NSArray\\n";

        KeyValueExtractor processedStackTrace = StackTraceProcessor.processStackTrace(stackTrace, "iOS");

        assertEquals("EXC_CRASH (SIGABRT)", processedStackTrace.getString("code"));
        assertEquals("Application threw exception NSRangeException: *** -[__NSArray0 objectAtIndex:]:" +
                " index 2 beyond bounds for empty NSArray",
            processedStackTrace.getString("message"));
        assertEquals("", processedStackTrace.getString("file"));
        assertEquals("0", processedStackTrace.getString("line"));
    }
}
