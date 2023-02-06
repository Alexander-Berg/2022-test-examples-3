#include <CppUTest/TestHarness.h>
#include <CppUTestExt/MockSupport.h>

extern "C"
{
	#include "sdc.h"
}

/*************************************************************************************************/

TEST_GROUP(SDC)
{
	void setup () 
    {
	}

	void teardown () {
	}
};

TEST(SDC, UnpackBase_should_ReturnBase)
{
    uint16_t id = SDC_BASE_COMMAND + 0x703;
    CHECK_EQUAL(SDC_BASE_COMMAND, sdc_UnpackBase(id));
};

TEST(SDC, UnpackId_should_ReturnId)
{
    uint16_t id = SDC_BASE_COMMAND + 0x703;
    CHECK_EQUAL(SDC_BASE_COMMAND, sdc_UnpackBase(id));
};
