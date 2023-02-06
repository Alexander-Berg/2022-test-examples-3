#USE_XRM  = yes
USE_LIBZ = yes

PEERDIR+=mail/so/spamstop/sp
PEERDIR+=mail/so/spamstop/zlib
PEERDIR+=mail/so/spamstop/mimelib
PEERDIR+=ysite/parser
PEERDIR+=ysite/parser/html
PEERDIR+=ysite/recognize
PEERDIR+=mail/so/spamstop/tools/so-clients
PEERDIR+=mail/so/spamstop/tools/so-common

SRCDIR += contrib
SRCDIR += mail/so/spamstop/test
SRCDIR += mail/so/corp

SRCS += Reader.cpp
SRCS += parsmess.cpp
SRCS += so_log.cpp

CREATEPROG=Reader

CFLAGS  += -DSPAMSTOP -I/usr/include -I/usr/include/ysite/parser/html
OBJADDE += -L/usr/lib 
OBJADDE += ${ROOM}/arcadia/contrib/pcre/.libs/libpcre.a

LDADD    = $(PEERLDADD)

.include "../../../../dt.mk"
