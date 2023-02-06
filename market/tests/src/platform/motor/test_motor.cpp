#include <CppUTest/TestHarness.h>
#include <CppUTestExt/MockSupport.h>

#include "motor.h"
#include "sdc.h"

#include <cstring>
#include <iostream>
#include <string>

#include "test_motor.h"

using namespace std;

/*************************************************************************************************/

static motor_t gMotor;

/*************************************************************************************************/

TEST_GROUP(motor_Create) { /* Nothing */  };

TEST_GROUP(motor_Destroy) { /* Nothing */ };

TEST_GROUP(motor_Setup)
{
	void setup () {
        motor_init_s motorInit { TEST_MOTOR_MOTOR_ID, MOTOR_DIRECTION_FORWARD };
        gMotor = motor_Create(&motorInit);
	}

	void teardown ()  {
        motor_Destroy(&gMotor);
	}
};

TEST_GROUP(motor)
{
	void setup () {
        motor_init_s motorInit { TEST_MOTOR_MOTOR_ID, MOTOR_DIRECTION_FORWARD };
        gMotor = motor_Create(&motorInit);

        motor_Setup(gMotor);
    
        TMassageCommand command;
        command.SetControlCalibrationStart(1);
        motor_Pack(gMotor, &command.id, command.data, &command.length);

        TMassageFeedback feedback;
        TMassageFeedback0 feedback0;
        feedback0.SetStatusCalibrationCalibrated(1);
        motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
        motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
	}

	void teardown ()  {
        motor_Destroy(&gMotor);
	}
};

TEST_GROUP(motorReverse)
{
	void setup () {
        motor_init_s motorInit { TEST_MOTOR_MOTOR_ID, MOTOR_DIRECTION_REVERSE };
        gMotor = motor_Create(&motorInit);

        motor_Setup(gMotor);
    
        TMassageCommand command;
        command.SetControlCalibrationStart(1);
        motor_Pack(gMotor, &command.id, command.data, &command.length);

        TMassageFeedback feedback;
        TMassageFeedback0 feedback0;
        feedback0.SetStatusCalibrationCalibrated(1);
        motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
        motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
	}

	void teardown ()  {
        motor_Destroy(&gMotor);
	}
};

/*************************************************************************************************/

TEST(motor_Create, should_AllocateMemory)
{
    motor_init_s motorInit { 0, MOTOR_DIRECTION_FORWARD };
    motor_t motor = motor_Create(&motorInit);

    CHECK_TRUE(motor);
    free(motor);
}

TEST(motor_Create, should_ReturnNULL_when_GetNULL)
{
    CHECK_FALSE(motor_Create(nullptr));
}

TEST(motor_Create, should_ReturnNULL_when_GetInvalidDiection)
{
    motor_init_s motorInit;

    motorInit.direction = MOTOR_DIRECTIONS_AMOUNT;
    CHECK_FALSE(motor_Create(&motorInit));
}

TEST(motor_Create, should_InitInstanceIdAndDirection)
{   
    uint16_t idExp = 12;
    motor_direction_e directionExp = MOTOR_DIRECTION_REVERSE;
    motor_init_s motorInit { idExp, directionExp };

    motor_t motor = motor_Create(&motorInit);

    uint16_t idAct;
    motor_direction_e directionAct;

    motor_GetId(motor, &idAct);
    motor_GetDirection(motor, &directionAct);

    CHECK_EQUAL(idExp, idAct);
    CHECK_EQUAL(directionExp, directionAct);

    free(motor);
}

TEST(motor_Create, should_InitStatus)
{   
    motor_init_s motorInit { 0, MOTOR_DIRECTION_FORWARD };
    motor_t motor = motor_Create(&motorInit);
    
    motor_status_u statusAct;
    motor_status_u statusExp;
    
    statusExp.operationCycle = 0;
    statusExp.notSetup = 1;
    statusExp.enable = 0;
    statusExp.errors = 0;
    statusExp.stop = 0;

    motor_GetStatus(motor, &statusAct);

    CHECK_EQUAL(statusExp.field, statusAct.field);
    free(motor);
}

TEST(motor_Create, should_InitError)
{   
    motor_init_s motorInit { 0, MOTOR_DIRECTION_FORWARD };
    motor_t motor = motor_Create(&motorInit);

    motor_errors_u errorsAct;
    motor_errors_u errorsExp;

    errorsExp.field = 0;

    motor_GetErrors(motor, &errorsAct);

    CHECK_EQUAL(errorsExp.field, errorsAct.field);
    free(motor);
}

TEST(motor_Destroy, should_FreeMemory_And_SettPointerToNULL)
{
    motor_init_s motorInit { 0, MOTOR_DIRECTION_FORWARD };
    motor_t motor = motor_Create(&motorInit);
    CHECK_TRUE(motor);

    motor_Destroy(&motor);
    CHECK_FALSE(motor);
}

TEST(motor_Destroy, should_DoNothing_when_GettNULL)
{
    motor_Destroy(NULL);

    motor_t nullMotor = NULL;
    motor_Destroy(&nullMotor);
}

TEST(motor_Setup, should_StartCalibration)
{
    TMassageCommand act;
    TMassageCommand exp;
    
    exp.SetControlCalibrationStart(1);

    motor_Setup(gMotor);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    
    CHECK_EQUAL(exp, act);
}

TEST(motor_Setup, should_ClearNotSetupFlag_when_Calibrated)
{
    motor_Setup(gMotor);
    
    motor_status_u status;
    motor_GetStatus(gMotor, &status);
    CHECK_TRUE(status.notSetup);
    CHECK_TRUE(status.inSetup);

    TMassageFeedback0 feedback0;
    feedback0.SetStatusCalibrationCalibrated(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(status.notSetup);
    CHECK_FALSE(status.inSetup);
}

TEST(motor_Setup, should_SetOperationCycle_when_SetupComplete)
{
    motor_Setup(gMotor);
    
    motor_status_u status;
    motor_GetStatus(gMotor, &status);
    CHECK_TRUE(status.notSetup);
    CHECK_TRUE(status.inSetup);

    TMassageFeedback0 feedback0;
    feedback0.SetStatusCalibrationCalibrated(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(status.notSetup);
    CHECK_FALSE(status.inSetup);
}

TEST(motor_Setup, should_SendControlCalibration_while_NotGetStatusCalibrated)
{
    TMassageCommand act;
    TMassageCommand exp;
    exp.SetControlCalibrationStart(1);

    motor_Setup(gMotor);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    TMassageFeedback0 feedback0;
    TMassageFeedback feedback;

    feedback.SetStatus5CalibrationStart(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback0.SetStatusCalibrationCalibrating(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback0.SetStatusCalibrationCalibrated(1);
    feedback0.SetStatusCalibrationCalibrating(0);
    exp.SetControlCalibrationStart(0);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor_Setup, should_ResetFeedback_0_FailsBeforeCalibration)
{
    TMassageFeedback0 feedback0;
    TMassageCommand act;
    TMassageCommand exp;

    feedback0.SetStatusCalibrationPowerSupplyVoltageFault(1);
    exp.SetControlFaultReset(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Setup(gMotor);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback0.SetStatusCalibrationPowerSupplyVoltageFault(0);
    exp.SetControlFaultReset(0);
    exp.SetControlCalibrationStart(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor_Setup, should_ResetFeedbackFailsBeforeCalibration)
{
    TMassageFeedback feedback;
    TMassageCommand act;
    TMassageCommand exp;

    feedback.SetStatus5Overheat(1);
    exp.SetControlFaultReset(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Setup(gMotor);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback.SetStatus5Overheat(0);
    exp.SetControlFaultReset(0);
    exp.SetControlCalibrationStart(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor_Setup, should_FailCalibration_and_SetError_when_GetCalibrationError)
{
    TMassageFeedback0 feedback0;
    TMassageFeedback feedback;
    TMassageCommand act;
    TMassageCommand exp;

    exp.SetControlCalibrationStart(1);
    motor_Setup(gMotor);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback.SetStatus5CalibrationStart(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback.SetStatus5CalibrationStart(0);
    feedback0.SetStatusCalibrationCalibrating(1);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    feedback0.SetStatusCalibrationPowerStageFault(1);
    exp.SetControlCalibrationStart(0);
    motor_Unpack(gMotor, feedback0.id, feedback0.data, feedback0.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_status_u status = { .field = 0 };

    motor_GetStatus(gMotor, &status);
    CHECK_TRUE(status.notSetup);
    CHECK_TRUE(status.errors);
    CHECK_FALSE(status.inSetup);

    motor_errors_u errors = {.field = 0};

    motor_GetErrors(gMotor, &errors);
    CHECK_TRUE(errors.calibration);
}

TEST(motor_Setup, should_NotEnable_when_IsNotSetup)
{
    TMassageCommand act;
    TMassageCommand exp;
    
    exp.SetControlEnable(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Enable(gMotor);

    exp.SetControlEnable(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, Enable_should_EnableMotor)
{
    TMassageCommand act;
    TMassageCommand exp;
    
    exp.SetControlEnable(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Enable(gMotor);

    exp.SetControlEnable(1);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, Enable_should_SetEnableMotor_when_GetEnableResponce)
{
    TMassageCommand act;
    TMassageCommand exp;
    TMassageFeedback feedback;

    motor_Enable(gMotor);

    exp.SetControlEnable(1);
    feedback.SetStatus4Enable(1);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, Enable_should_SetEnableMotor_when_DoNotGetEnableResponce)
{
    TMassageCommand act;
    TMassageCommand exp;
    TMassageFeedback feedback;

    motor_Enable(gMotor);

    exp.SetControlEnable(1);
    feedback.SetStatus4Enable(0);
    
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, Disable_should_DisableMotor)
{
    TMassageCommand act;
    TMassageCommand exp;

    motor_Enable(gMotor);

    exp.SetControlEnable(1);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Disable(gMotor);

    exp.SetControlEnable(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, Disable_should_ResetSpeed)
{
    TMassageCommand act;
    TMassageCommand exp;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 1.5f);
    exp.SetControlEnable(1);
    exp.SetSpeedTarget_Hz(1.5f);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_Disable(gMotor);
    exp.SetControlEnable(0);
    exp.SetSpeedTarget_Hz(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, SetSpeedTarget_should_SetSpeed_when_MotorEnabled)
{
    TMassageCommand act;
    TMassageCommand exp;
    float speedExp = 1.5f;
    float speedAct;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, speedExp);

    exp.SetControlEnable(1);
    exp.SetSpeedTarget_Hz(speedExp);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_GetSpeedTarget(gMotor, &speedAct);
    CHECK_EQUAL(speedExp, speedAct);
}

TEST(motorReverse, SetSpeedTarget_should_SetSpeed_when_MotorEnabled)
{
    TMassageCommand act;
    TMassageCommand exp;
    float speedExp = 1.5f;
    float speedAct;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, speedExp);

    exp.SetControlEnable(1);
    exp.SetSpeedTarget_Hz(-speedExp);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    motor_GetSpeedTarget(gMotor, &speedAct);
    CHECK_EQUAL(speedExp, speedAct);
}

TEST(motor, SetSpeedTarget_should_DoNothing_when_MotorDisabled)
{
    TMassageCommand act;
    TMassageCommand exp;

    motor_SetSpeedTarget(gMotor, 1.5f);

    exp.SetControlEnable(0);
    exp.SetSpeedTarget_Hz(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, SetSpeedTarget_should_TurnOnBrake_when_Get_0)
{
    TMassageCommand act;
    TMassageCommand exp;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 0.f);

    exp.SetControlEnable(1);
    exp.SetControlDynBrakeEnable(1);
    exp.SetSpeedTarget_Hz(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, SetSpeedTarget_should_TurnOffBrake_when_GetSpeed)
{
    TMassageCommand act;
    TMassageCommand exp;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 0.f);

    exp.SetControlEnable(1);
    exp.SetControlDynBrakeEnable(1);
    exp.SetSpeedTarget_Hz(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    float speed = 0.11f;
    motor_SetSpeedTarget(gMotor, speed);

    exp.SetControlEnable(1);
    exp.SetControlDynBrakeEnable(0);
    exp.SetSpeedTarget_Hz(speed);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, GetSpeed_should_UnpackReseivedSpeeds)
{
    TMassageFeedback feedback;
    float speedExp = 0.133f;
    float speedAct;

    feedback.SetSpeedActual_Hz(speedExp);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetSpeedActual(gMotor, &speedAct);
    CHECK_EQUAL(speedExp, speedAct);
}

TEST(motorReverse, GetSpeed_should_UnpackReseivedSpeeds)
{
    TMassageFeedback feedback;
    float speedExp = 0.133f;
    float speedAct;

    feedback.SetSpeedActual_Hz(-speedExp);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetSpeedActual(gMotor, &speedAct);
    CHECK_EQUAL(speedExp, speedAct);
}

TEST(motor, GetErrors_should_ReturnSpecificErrors)
{
    motor_errors_u errors = {.field = 0};
    TMassageFeedback feedback;

    feedback.SetStatus4CommonCurrentFault(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetErrors(gMotor, &errors);
    CHECK_TRUE(errors.current);

    feedback.SetStatus4PositionSensorFault(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetErrors(gMotor, &errors);
    CHECK_TRUE(errors.positionSensor);

    feedback.SetStatus5TemperatureSensorFault(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetErrors(gMotor, &errors);
    CHECK_TRUE(errors.temperature);
}

TEST(motor, ResetErrors_should_ResetFault_and_ClearStatus)
{
    motor_errors_u errors = {.field = 0};
    motor_status_u status = {.field = 0};
    TMassageFeedback feedback;

    feedback.SetStatus4CommonCurrentFault(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_GetErrors(gMotor, &errors);
    motor_GetStatus(gMotor, &status);
    CHECK_TRUE(errors.current);
    CHECK_TRUE(status.errors);

    motor_ResetErrors(gMotor);
    motor_GetErrors(gMotor, &errors);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(errors.current);
    CHECK_FALSE(status.errors);
}

TEST(motor, ResetErrors_should_SendResetCommand)
{
    TMassageCommand act;
    TMassageCommand exp;
    TMassageFeedback feedback;

    feedback.SetStatus4CommonCurrentFault(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_ResetErrors(gMotor);
    exp.SetControlFaultReset(1);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, should_SetStopStatus_when_SetAndActualSpeedsAre_0)
{
    TMassageCommand command;
    TMassageFeedback feedback;

    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetStatus4CompensatorReset(1);
    feedback.SetSpeedActual_Hz(-0.00009f);

    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);

    motor_status_u status = { .field = 0};
    motor_GetStatus(gMotor, &status);

    CHECK_TRUE(status.enable);
    CHECK_TRUE(status.stop);
}

TEST(motor, should_NotSetStopStatus_when_OnlySetSpeedTargetIs_0)
{
    TMassageFeedback feedback;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 0.f);

    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetStatus4CompensatorReset(1);
    feedback.SetSpeedActual_Hz(-0.001f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);

    motor_status_u status = { .field = 0};
    motor_GetStatus(gMotor, &status);

    CHECK_TRUE(status.enable);
    CHECK_FALSE(status.stop);
}

TEST(motor, should_NotSetStopStatus_when_OnlyActualSpeedIs_0)
{
    TMassageFeedback feedback;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 1.1f);

    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetStatus4CompensatorReset(1);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);

    motor_status_u status = { .field = 0};
    motor_GetStatus(gMotor, &status);

    CHECK_TRUE(status.enable);
    CHECK_FALSE(status.stop);
}

TEST(motor, should_RestRegulator_when_Stoped)
{
    TMassageFeedback feedback;
    TMassageCommand act;
    TMassageCommand exp;

    motor_Enable(gMotor);
    motor_SetSpeedTarget(gMotor, 0.f);

    /// Останавливаем моторы
    exp.SetControlEnable(1);
    exp.SetControlDynBrakeEnable(1);
    exp.SetSpeedTarget_Hz(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    /// Моторы еще не остановились
    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetSpeedActual_Hz(0.05f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    /// Моторы остановились - сбрасываем ошибку регулятора
    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetSpeedActual_Hz(0.0f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    exp.SetControlCompensatorReset(1);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);

    /// Ошибка сброшена
    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetStatus4CompensatorReset(1);
    feedback.SetSpeedActual_Hz(0.0f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    exp.SetControlCompensatorReset(0);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    CHECK_EQUAL(exp, act);
}

TEST(motor, should_ClearStopStatus_when_StartMotionAgain)
{
    TMassageFeedback feedback;
    TMassageCommand act;
    motor_status_u status = { .field = 0 };

    motor_Enable(gMotor);

    /// Останавливаем моторы
    motor_SetSpeedTarget(gMotor, 0.f);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(status.stop);

    /// Моторы остановились - сбрасываем ошибку регулятора
    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetSpeedActual_Hz(0.0f);
    motor_SetSpeedTarget(gMotor, 0.f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(status.stop);

    /// Ошибка сброшена
    feedback.SetStatus4Enable(1);
    feedback.SetStatus4DynBrakeEnable(1);
    feedback.SetStatus4CompensatorReset(1);
    feedback.SetSpeedActual_Hz(0.0f);
    motor_SetSpeedTarget(gMotor, 0.f);
    motor_Unpack(gMotor, feedback.id, feedback.data, feedback.length);
    motor_Pack(gMotor, &act.id, act.data, &act.length);
    motor_GetStatus(gMotor, &status);
    CHECK_TRUE(status.stop);

    motor_SetSpeedTarget(gMotor, 0.55f);
    motor_GetStatus(gMotor, &status);
    CHECK_FALSE(status.stop);
}

