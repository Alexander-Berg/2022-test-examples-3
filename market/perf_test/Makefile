include $(APPDIR)/Make.defs

LIB_DIR = $(APPDIR)/uros
CFLAGS += ${shell $(INCDIR) "$(CC)" $(LIB_DIR)/include}

#This is the name of the app on the NSH console
PROGNAME = perf_test
PRIORITY = SCHED_PRIORITY_DEFAULT
STACKSIZE = 6000
MODULE = $(CONFIG_YMBOT_PERFORMANCE_TEST)

#Add the source files.
MAINSRC = perf_test.c 

FORMATED_CXXFLAGS := $(subst /,\/,$(CXXFLAGS))
FORMATED_CXXFLAGS := $(subst ",,$(FORMATED_CXXFLAGS))

FORMATED_CFLAGS := $(subst /,\/,$(CFLAGS))
FORMATED_CFLAGS := $(subst ",,$(FORMATED_CFLAGS))

include $(APPDIR)/Application.mk
