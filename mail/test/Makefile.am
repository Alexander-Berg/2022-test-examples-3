bin_PROGRAMS = parseresponse checkrequest
parseresponse_SOURCES = parseresp.cpp
parseresponse_LDADD = ../src/libblackbox2.la

checkrequest_SOURCES = checkreq.cpp
checkrequest_LDADD = ../src/libblackbox2.la

AM_CPPFLAGS = -I@top_srcdir@/config/ -I@top_srcdir@/include/ @BOOST_CPPFLAGS@
AM_LDFLAGS = -L/usr/local/lib @VERSION_INFO@ @BOOST_LDFLAGS@

check:
	./testall.sh
