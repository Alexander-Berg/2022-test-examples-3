## Silence switch ##
# run `make V=1` to make it verbose.
ifeq (1,${V})
    SILENCE =
else
    SILENCE = @  
endif

## CppUTest configurations ##
# Read header comment on file `$(CPPUTEST_HOME)/build/MakefileWorker.mk` to understand these definitions.

COMPONENT_NAME = ymbot
CPPUTEST_HOME = ../../../../tools/cpputest

CPPUTEST_ENABLE_DEBUG = Y
CPPUTEST_USE_EXTENSIONS = Y
CPP_PLATFORM = Gcc
CPPUTEST_USE_GCOV = Y
CPPUTEST_OBJS_DIR = objs/tests
CPPUTEST_LIB_DIR = lib/tests

INCLUDE_DIRS = $(CPPUTEST_HOME)/include \
	../utils \
	../platform \
	../platform/motor \

SRC_DIRS = \

SRC_FILES = ../platform/chassis.c \
	../platform/motor/motor.c \
	../platform/motor/sdc.c \
	
TEST_SRC_DIRS = \
    ./src \
	./src/utils \
	./src/platform \
	./src/platform/motor \

CPPUTEST_CPPFLAGS = -DDISABLE_LOG -DTEST_BUILD
CPPUTEST_CXXFLAGS = -std=c++17 -O2
CPPUTEST_LDFLAGS = -pthread
CPPUTEST_WARNINGFLAGS = -Wall -Wextra -Wshadow -Wswitch-default -Wswitch-enum -Wconversion -Wno-long-long

## The real work ##
# including this file that will use configuration and have the make rules.
include $(CPPUTEST_HOME)/build/MakefileWorker.mk

# Coverage Report rules #
coverage: all
	$(SILENCE)lcov --capture --directory objs/tests/src --output-file coverage.info
	$(SILANCE)genhtml coverage.info --output-directory coverage
	@echo
	@echo "Written coverage report to coverage/index.html"
	@echo
	
coverage_clean:
	rm -rf coverage coverage.info
