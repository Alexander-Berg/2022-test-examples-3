package ru.yandex.market.tsum.clients.arcadia;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 15/08/2018
 */
public class ArcadiaDiffStatsOutputStreamTest {

    @Test
    public void parse() {

        String diff = "Index: java-application/core/src/main/bin/application-start.sh\n" +
            "Index: java-application/core/src/main/bin/application-start.sh\n" +
            "===================================================================\n" +
            "--- java-application/core/src/main/bin/application-start.sh\t(revision 3762215)\n" +
            "+++ java-application/core/src/main/bin/application-start.sh\t(revision 3794239)\n" +
            "@@ -60,6 +60,8 @@\n" +
            "   case ${opt} in\n" +
            "     --environment=*) ENVIRONMENT=\"${opt#*=}\"\n" +
            "     shift ;;\n" +
            "+    --dc=*) DC=\"${opt#*=}\"\n" +
            "+    shift ;;\n" +
            "     --cpu-count=*) CPU_COUNT=\"${opt#*=}\"\n" +
            "     shift ;;\n" +
            "     --logdir=*) LOG_DIR=\"${opt#*=}\"\n" +
            "@@ -141,12 +143,14 @@\n" +
            "      -Dhost.fqdn=\"${HOST_FQDN}\" \\\n" +
            "      -Dapp.name=\"{{appName}}\" \\\n" +
            "      -Denvironment=\"${ENVIRONMENT}\" \\\n" +
            "+     -Ddatacenter=\"${DC}\" \\\n" +
            "+     -Dyandex.environment.type=\"${ENVIRONMENT}\" \\\n" +
            "      -Dconfigs.path=\"${PROPERTIES_DIR}\" \\\n" +
            "      -Ddata.dir=\"${DATA_DIR}\" \\\n" +
            "      -Dext.data.dir=\"${EXT_DATA_DIR}\" \\\n" +
            "      -Dlog.dir=\"${LOG_DIR}\" \\\n" +
            "      -Dtmp.dir=\"${TMP_DIR}\" \\\n" +
            "-     -Doracle.net.tns_admin=\"${PROPERTIES_DIR}\"\n" +
            "+     -Doracle.net.tns_admin=\"${PROPERTIES_DIR}\" \\\n" +
            "      -Djava.net.preferIPv6Addresses=true \\\n" +
            "      -Djava.net.preferIPv4Stack=false \\\n" +
            "      -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \\";

        Assert.assertEquals(new ArcadiaDiffStats(5, 1, 1), parse(diff));
    }

    @Test
    public void anotherParse() {

        String diff = "Index: report/src/Request.cpp\n" +
            "===================================================================\n" +
            "--- report/src/Request.cpp\t(revision 3796302)\n" +
            "+++ report/src/Request.cpp\t(revision 3796324)\n" +
            "@@ -247,7 +247,8 @@\n" +
            "     const TRequestCategoriesClassificationFactors& Request::getRequestCategoriesClassificationFactors()" +
            " const {\n" +
            "         static const TRequestCategoriesClassificationFactors empty;\n" +
            "\n" +
            "-        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || params_.Pp().Get() == " +
            "EPlacement::MARKET_API_ADVISOR) {\n" +
            "+        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || params_.Pp().Get() == " +
            "EPlacement::MARKET_API_ADVISOR ||\n" +
            "+                params_.ReportOutputLogic().Get() != OL_PRIME) {\n" +
            "             return empty;\n" +
            "         }\n" +
            "\n" +
            "Index: report/src/rich_request.cpp\n" +
            "===================================================================\n" +
            "--- report/src/rich_request.cpp\t(revision 3796302)\n" +
            "+++ report/src/rich_request.cpp\t(revision 3796324)\n" +
            "@@ -149,7 +149,8 @@\n" +
            "     const TRequestCategoriesClassificationFactors& " +
            "TRichRequest::getRequestCategoriesClassificationFactors() const {\n" +
            "         static const TRequestCategoriesClassificationFactors empty;\n" +
            "\n" +
            "-        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || Params.Pp().Get() == " +
            "EPlacement::MARKET_API_ADVISOR) {\n" +
            "+        if (!NGlobal::AreRequestCategoriesClassificatorRequestsEnabled() || Params.Pp().Get() == " +
            "EPlacement::MARKET_API_ADVISOR ||\n" +
            "+                Params.ReportOutputLogic().Get() != OL_PRIME) {\n" +
            "             return empty;\n" +
            "         }\n";

        Assert.assertEquals(new ArcadiaDiffStats(4, 2, 2), parse(diff));
    }

    private static ArcadiaDiffStats parse(String diff) {
        ArcadiaDiffStatsOutputStream stream = new ArcadiaDiffStatsOutputStream();
        try {
            stream.write(diff.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stream.getDiffStat();
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void parseDiffWithDiff() {
        String diff = "Index: contrib/tools/python/src/Modules/_ctypes/libffi.diff\n" +
            "===================================================================\n" +
            "--- contrib/tools/python/src/Modules/_ctypes/libffi.diff\t(revision 3975157)\n" +
            "+++ contrib/tools/python/src/Modules/_ctypes/libffi.diff\t(nonexistent)\n" +
            "@@ -1,209 +0,0 @@\n" +
            "-diff -urN libffi-3.1/configure libffi/configure\n" +
            "---- libffi-3.1/configure\t2014-05-19 15:44:03.000000000 +0200\n" +
            "-+++ libffi/configure\t2014-08-09 21:51:07.877871443 +0200\n" +
            "-@@ -17236,6 +17236,10 @@\n" +
            "- \tfi\n" +
            "- \t;;\n" +
            "- \n" +
            "-+  i*86-*-nto-qnx*)\n" +
            "-+        TARGET=X86; TARGETDIR=x86\n" +
            "-+        ;;\n" +
            "-+\n" +
            "-   x86_64-*-darwin*)\n" +
            "- \tTARGET=X86_DARWIN; TARGETDIR=x86\n" +
            "- \t;;\n" +
            "-@@ -17298,12 +17302,12 @@\n" +
            "- \t;;\n" +
            "- \n" +
            "-   mips-sgi-irix5.* | mips-sgi-irix6.* | mips*-*-rtems*)\n" +
            "--\tTARGET=MIPS; TARGETDIR=mips\n" +
            "-+\tTARGET=MIPS_IRIX; TARGETDIR=mips\n" +
            "- \t;;\n" +
            "-   mips*-*linux* | mips*-*-openbsd*)\n" +
            "- \t# Support 128-bit long double for NewABI.\n" +
            "- \tHAVE_LONG_DOUBLE='defined(__mips64)'\n" +
            "--\tTARGET=MIPS; TARGETDIR=mips\n" +
            "-+\tTARGET=MIPS_LINUX; TARGETDIR=mips\n" +
            "- \t;;\n" +
            "- \n" +
            "-   nios2*-linux*)\n" +
            "-@@ -17373,7 +17377,7 @@\n" +
            "-   as_fn_error $? \"\\\"libffi has not been ported to $host.\\\"\" \"$LINENO\" 5\n" +
            "- fi\n" +
            "- \n" +
            "-- if test x$TARGET = xMIPS; then\n" +
            "-+ if expr x$TARGET : 'xMIPS' > /dev/null; then\n" +
            "-   MIPS_TRUE=\n" +
            "-   MIPS_FALSE='#'\n" +
            "- else\n" +
            "-@@ -18814,6 +18818,12 @@\n" +
            "- ac_config_files=\"$ac_config_files include/Makefile include/ffi.h Makefile testsuite/Makefile " +
            "man/Makefile libffi.pc\"\n" +
            "- \n" +
            "- \n" +
            "-+ac_config_links=\"$ac_config_links include/ffi_common.h:include/ffi_common.h\"\n" +
            "-+\n" +
            "-+\n" +
            "-+ac_config_files=\"$ac_config_files fficonfig.py\"\n" +
            "-+\n" +
            "-+\n" +
            "- cat >confcache <<\\_ACEOF\n" +
            "- # This file is a shell script that caches the results of configure\n" +
            "- # tests run on this system so they can be shared between configure\n" +
            "-@@ -20126,6 +20136,8 @@\n" +
            "-     \"testsuite/Makefile\") CONFIG_FILES=\"$CONFIG_FILES testsuite/Makefile\" ;;\n" +
            "-     \"man/Makefile\") CONFIG_FILES=\"$CONFIG_FILES man/Makefile\" ;;\n" +
            "-     \"libffi.pc\") CONFIG_FILES=\"$CONFIG_FILES libffi.pc\" ;;\n" +
            "-+    \"include/ffi_common.h\") CONFIG_LINKS=\"$CONFIG_LINKS include/ffi_common.h:include/ffi_common.h\"" +
            " ;;\n" +
            "-+    \"fficonfig.py\") CONFIG_FILES=\"$CONFIG_FILES fficonfig.py\" ;;\n" +
            "- \n" +
            "-   *) as_fn_error $? \"invalid argument: \\`$ac_config_target'\" \"$LINENO\" 5;;\n" +
            "-   esac\n" +
            "-diff -urN libffi-3.1/configure.ac libffi/configure.ac\n" +
            "---- libffi-3.1/configure.ac\t2014-05-11 15:57:49.000000000 +0200\n" +
            "-+++ libffi/configure.ac\t2014-08-09 21:51:07.877871443 +0200\n" +
            "-@@ -1,4 +1,7 @@\n" +
            "- dnl Process this with autoconf to create configure\n" +
            "-+#\n" +
            "-+# file from libffi - slightly patched for Python's ctypes\n" +
            "-+#\n" +
            "- \n" +
            "- AC_PREREQ(2.68)\n" +
            "- \n" +
            "-@@ -144,6 +147,9 @@\n" +
            "- \t  AM_LTLDFLAGS='-no-undefined -bindir \"$(bindir)\"';\n" +
            "- \tfi\n" +
            "- \t;;\n" +
            "-+  i*86-*-nto-qnx*) \n" +
            "-+        TARGET=X86; TARGETDIR=x86\n" +
            "-+        ;;\n" +
            "-   i?86-*-darwin*)\n" +
            "- \tTARGET=X86_DARWIN; TARGETDIR=x86\n" +
            "- \t;;\n" +
            "-@@ -218,12 +224,12 @@\n" +
            "- \t;;\n" +
            "- \n" +
            "-   mips-sgi-irix5.* | mips-sgi-irix6.* | mips*-*-rtems*)\n" +
            "--\tTARGET=MIPS; TARGETDIR=mips\n" +
            "-+\tTARGET=MIPS_IRIX; TARGETDIR=mips\n" +
            "- \t;;\n" +
            "-   mips*-*linux* | mips*-*-openbsd*)\n" +
            "- \t# Support 128-bit long double for NewABI.\n" +
            "- \tHAVE_LONG_DOUBLE='defined(__mips64)'\n" +
            "--\tTARGET=MIPS; TARGETDIR=mips\n" +
            "-+\tTARGET=MIPS_LINUX; TARGETDIR=mips\n" +
            "- \t;;\n" +
            "- \n" +
            "-   nios2*-linux*)\n" +
            "-@@ -293,7 +299,7 @@\n" +
            "-   AC_MSG_ERROR([\"libffi has not been ported to $host.\"])\n" +
            "- fi\n" +
            "- \n" +
            "--AM_CONDITIONAL(MIPS, test x$TARGET = xMIPS)\n" +
            "-+AM_CONDITIONAL(MIPS,[expr x$TARGET : 'xMIPS' > /dev/null])\n" +
            "- AM_CONDITIONAL(BFIN, test x$TARGET = xBFIN)\n" +
            "- AM_CONDITIONAL(SPARC, test x$TARGET = xSPARC)\n" +
            "- AM_CONDITIONAL(X86, test x$TARGET = xX86)\n" +
            "-@@ -617,4 +623,8 @@\n" +
            "- \n" +
            "- AC_CONFIG_FILES(include/Makefile include/ffi.h Makefile testsuite/Makefile man/Makefile libffi.pc)\n" +
            "- \n" +
            "-+AC_CONFIG_LINKS(include/ffi_common.h:include/ffi_common.h)\n" +
            "-+\n" +
            "-+AC_CONFIG_FILES(fficonfig.py)\n" +
            "-+\n" +
            "- AC_OUTPUT\n" +
            "-diff -urN libffi-3.1/fficonfig.py.in libffi/fficonfig.py.in\n" +
            "---- libffi-3.1/fficonfig.py.in\t1970-01-01 01:00:00.000000000 +0100\n" +
            "-+++ libffi/fficonfig.py.in\t2014-08-09 21:43:25.229871827 +0200\n" +
            "-@@ -0,0 +1,35 @@\n" +
            "-+ffi_sources = \"\"\"\n" +
            "-+src/prep_cif.c\n" +
            "-+src/closures.c\n" +
            "-+\"\"\".split()\n" +
            "-+\n" +
            "-+ffi_platforms = {\n" +
            "-+    'MIPS_IRIX': ['src/mips/ffi.c', 'src/mips/o32.S', 'src/mips/n32.S'],\n" +
            "-+    'MIPS_LINUX': ['src/mips/ffi.c', 'src/mips/o32.S'],\n" +
            "-+    'X86': ['src/x86/ffi.c', 'src/x86/sysv.S', 'src/x86/win32.S'],\n" +
            "-+    'X86_FREEBSD': ['src/x86/ffi.c', 'src/x86/freebsd.S'],\n" +
            "-+    'X86_WIN32': ['src/x86/ffi.c', 'src/x86/win32.S'],\n" +
            "-+    'SPARC': ['src/sparc/ffi.c', 'src/sparc/v8.S', 'src/sparc/v9.S'],\n" +
            "-+    'ALPHA': ['src/alpha/ffi.c', 'src/alpha/osf.S'],\n" +
            "-+    'IA64': ['src/ia64/ffi.c', 'src/ia64/unix.S'],\n" +
            "-+    'M32R': ['src/m32r/sysv.S', 'src/m32r/ffi.c'],\n" +
            "-+    'M68K': ['src/m68k/ffi.c', 'src/m68k/sysv.S'],\n" +
            "-+    'POWERPC': ['src/powerpc/ffi.c', 'src/powerpc/ffi_sysv.c', 'src/powerpc/ffi_linux64.c', " +
            "'src/powerpc/sysv.S', 'src/powerpc/ppc_closure.S', 'src/powerpc/linux64.S', 'src/powerpc/linux64_closure" +
            ".S'],\n" +
            "-+    'POWERPC_AIX': ['src/powerpc/ffi_darwin.c', 'src/powerpc/aix.S', 'src/powerpc/aix_closure.S'],\n" +
            "-+    'POWERPC_FREEBSD': ['src/powerpc/ffi.c', 'src/powerpc/sysv.S', 'src/powerpc/ppc_closure.S'],\n" +
            "-+    'AARCH64': ['src/aarch64/sysv.S', 'src/aarch64/ffi.c'],\n" +
            "-+    'ARM': ['src/arm/sysv.S', 'src/arm/ffi.c'],\n" +
            "-+    'LIBFFI_CRIS': ['src/cris/sysv.S', 'src/cris/ffi.c'],\n" +
            "-+    'FRV': ['src/frv/eabi.S', 'src/frv/ffi.c'],\n" +
            "-+    'S390': ['src/s390/sysv.S', 'src/s390/ffi.c'],\n" +
            "-+    'X86_64': ['src/x86/ffi64.c', 'src/x86/unix64.S', 'src/x86/ffi.c', 'src/x86/sysv.S'],\n" +
            "-+    'SH': ['src/sh/sysv.S', 'src/sh/ffi.c'],\n" +
            "-+    'SH64': ['src/sh64/sysv.S', 'src/sh64/ffi.c'],\n" +
            "-+    'PA': ['src/pa/linux.S', 'src/pa/ffi.c'],\n" +
            "-+    'PA_LINUX': ['src/pa/linux.S', 'src/pa/ffi.c'],\n" +
            "-+    'PA_HPUX': ['src/pa/hpux32.S', 'src/pa/ffi.c'],\n" +
            "-+}\n" +
            "-+\n" +
            "-+ffi_sources += ffi_platforms['@TARGET@']\n" +
            "-+\n" +
            "-+ffi_cflags = '@CFLAGS@'\n" +
            "-diff -urN libffi-3.1/src/dlmalloc.c libffi/src/dlmalloc.c\n" +
            "---- libffi-3.1/src/dlmalloc.c\t2014-04-25 19:45:13.000000000 +0200\n" +
            "-+++ libffi/src/dlmalloc.c\t2014-08-09 21:51:07.881871443 +0200\n" +
            "-@@ -457,6 +457,11 @@\n" +
            "- #define LACKS_ERRNO_H\n" +
            "- #define MALLOC_FAILURE_ACTION\n" +
            "- #define MMAP_CLEARS 0 /* WINCE and some others apparently don't clear */\n" +
            "-+#elif !defined _GNU_SOURCE\n" +
            "-+/* mremap() on Linux requires this via sys/mman.h\n" +
            "-+ * See roundup issue 10309\n" +
            "-+ */\n" +
            "-+#define _GNU_SOURCE 1\n" +
            "- #endif  /* WIN32 */\n" +
            "- \n" +
            "- #ifdef __OS2__\n" +
            "-@@ -4497,7 +4502,7 @@\n" +
            "-     char* tbase = (char*)(CALL_MMAP(tsize));\n" +
            "-     if (tbase != CMFAIL) {\n" +
            "-       m = init_user_mstate(tbase, tsize);\n" +
            "--      set_segment_flags(&m->seg, IS_MMAPPED_BIT);\n" +
            "-+      (void)set_segment_flags(&m->seg, IS_MMAPPED_BIT);\n" +
            "-       set_lock(m, locked);\n" +
            "-     }\n" +
            "-   }\n" +
            "-@@ -4512,7 +4517,7 @@\n" +
            "-   if (capacity > msize + TOP_FOOT_SIZE &&\n" +
            "-       capacity < (size_t) -(msize + TOP_FOOT_SIZE + mparams.page_size)) {\n" +
            "-     m = init_user_mstate((char*)base, capacity);\n" +
            "--    set_segment_flags(&m->seg, EXTERN_BIT);\n" +
            "-+    (void)set_segment_flags(&m->seg, EXTERN_BIT);\n" +
            "-     set_lock(m, locked);\n" +
            "-   }\n" +
            "-   return (mspace)m;\n" +
            "-diff -urN libffi-3.1/src/arm/ffi.c libffi/src/arm/ffi.c\n" +
            "---- libffi-3.1/src/arm/ffi.c\tSat Aug 09 23:52:34 2014 +0200\n" +
            "-+++ libffi/src/arm/ffi.c\tSat Aug 09 23:58:38 2014 +0200\n" +
            "-@@ -154,9 +154,6 @@\n" +
            "- \n" +
            "- int ffi_prep_args_VFP(char *stack, extended_cif *ecif, float *vfp_space)\n" +
            "- {\n" +
            "--  // make sure we are using FFI_VFP\n" +
            "--  FFI_ASSERT(ecif->cif->abi == FFI_VFP);\n" +
            "--\n";

        Assert.assertEquals(new ArcadiaDiffStats(0, 195, 1), parse(diff));
    }
}
