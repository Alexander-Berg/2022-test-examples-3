#include <CppUTest/TestHarness.h>
#include <CppUTestExt/MockSupport.h>

extern "C"
{
	#include "chassis.h"
}

#include <math.h>

/*************************************************************************************************/

static const float PI = 3.1415926f;

static float LinearToFrequency(float velocity_m__s) {
    return velocity_m__s / ( 2.f * PI * CHASSIS_RADIUS_WHEEL);
}

static float AngularToFrequency(float velocity_rad__s) {
    return LinearToFrequency(velocity_rad__s * (CHASSIS_AXEL / 2));
}

/*************************************************************************************************/

TEST_GROUP(Stub)
{
	void setup () {
	}

	void teardown () {
	}
};

/*************************************************************************************************/

/**
 * chassis_GetMotorSpeed
 */
TEST(Stub, chassis_GetMotorSpeed_should_ConvertLinearVelocityToMotorFrequency)
{
    chassis_velocity_s vel = { .linear = 1, .angular = 0 };
    float nExp = LinearToFrequency(vel.linear);
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &vel);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &vel);
    
    DOUBLES_EQUAL(nExp, speedLeft,  0.001);
    DOUBLES_EQUAL(nExp, speedRight, 0.001);
};

TEST(Stub, chassis_GetMotorSpeed_should_ConvertNegativeLinearVelocityToMotorFrequency)
{
    chassis_velocity_s vel = { .linear = -1, .angular = 0 };
    float nExp = LinearToFrequency(vel.linear);
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &vel);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &vel);
    
    DOUBLES_EQUAL(nExp, speedLeft,  0.001);
    DOUBLES_EQUAL(nExp, speedRight, 0.001);
};

TEST(Stub, chassis_GetMotorSpeed_should_ConvertAngularVelocityToMotorFrequency)
{
    chassis_velocity_s vel = { .linear = 0, .angular = PI };
    float nExpLeft = -AngularToFrequency(vel.angular);
    float nExpRight = AngularToFrequency(vel.angular);
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &vel);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &vel);
    
    DOUBLES_EQUAL(nExpLeft, speedLeft,  0.001);
    DOUBLES_EQUAL(nExpRight, speedRight, 0.001);
};

TEST(Stub, chassis_GetMotorSpeed_should_ConvertNegativeAngularVelocityToMotorFrequency)
{
    chassis_velocity_s vel = { .linear = 0, .angular = -PI };
    float nExpLeft = -AngularToFrequency(vel.angular);
    float nExpRight = AngularToFrequency(vel.angular);
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &vel);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &vel);
    
    DOUBLES_EQUAL(nExpLeft, speedLeft,  0.001);
    DOUBLES_EQUAL(nExpRight, speedRight, 0.001);
};

TEST(Stub, chassis_GetMotorSpeed_should_ConvertVelocityToMotorFrequency)
{
    chassis_velocity_s vel = { .linear = 1, .angular = PI };
    float nExpLeft  = LinearToFrequency(vel.linear) - AngularToFrequency(vel.angular);
    float nExpRight = LinearToFrequency(vel.linear) + AngularToFrequency(vel.angular);
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &vel);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &vel);
    
    DOUBLES_EQUAL(nExpLeft, speedLeft,  0.001);
    DOUBLES_EQUAL(nExpRight, speedRight, 0.001);
};

/**
 * chassis_GetMotorSpeed
 */
TEST(Stub, chassis_GetVelocity_should_ReturnVelocity)
{
    chassis_velocity_s velExp = { .linear = 1, .angular = PI };
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &velExp);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &velExp);
    
    chassis_velocity_s velAct = chassis_GetVelocity(speedLeft, speedRight);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);
};

TEST(Stub, chassis_GetVelocity_should_ReturnVelocity_when_AngularVelocityIsNegative)
{
    chassis_velocity_s velExp = { .linear = 0.1f, .angular = -CHASSIS_VELOCITY_MAX_ANGULAR };
    float speedLeft  = chassis_GetMotorSpeed(CHASSIS_MOTOR_LEFT, &velExp);
    float speedRight = chassis_GetMotorSpeed(CHASSIS_MOTOR_RIGHT, &velExp);
    
    chassis_velocity_s velAct = chassis_GetVelocity(speedLeft, speedRight);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);
};

/**
 * chassis_ControlVelocity
 */
TEST(Stub, chassis_ControlVelocity_should_DoNoting_when_VelocityInLimits)
{
    chassis_velocity_s velExp = { 0, 0 };
    chassis_velocity_s velAct = { 0, 0 };

    velExp.linear = CHASSIS_VELOCITY_MAX_LINEAR;
    velExp.angular = 0;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);

    velExp.linear = -CHASSIS_VELOCITY_MAX_LINEAR;
    velExp.angular = 0;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);

    velExp.linear = 0;
    velExp.angular = CHASSIS_VELOCITY_MAX_ANGULAR;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);

    velExp.linear = 0;
    velExp.angular = -CHASSIS_VELOCITY_MAX_ANGULAR;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(velExp.linear, velAct.linear,  0.001);
    DOUBLES_EQUAL(velExp.angular, velAct.angular, 0.001);
};

TEST(Stub, chassis_ControlVelocity_should_RestrictVelocities_when_VelocityOverLimits)
{
    chassis_velocity_s velExp = { 0, 0 };
    chassis_velocity_s velAct = { 0, 0 };

    velExp.linear = CHASSIS_VELOCITY_MAX_LINEAR + 0.01f;
    velExp.angular = 0;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(CHASSIS_VELOCITY_MAX_LINEAR, velAct.linear,  0.001f);
    DOUBLES_EQUAL(0, velAct.angular, 0.001);

    velExp.linear = -(CHASSIS_VELOCITY_MAX_LINEAR + 0.01f);
    velExp.angular = 0;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(-CHASSIS_VELOCITY_MAX_LINEAR, velAct.linear,  0.001);
    DOUBLES_EQUAL(0, velAct.angular, 0.001);

    velExp.linear = 0;
    velExp.angular = CHASSIS_VELOCITY_MAX_ANGULAR + 0.01f;
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(0, velAct.linear,  0.001);
    DOUBLES_EQUAL(CHASSIS_VELOCITY_MAX_ANGULAR, velAct.angular, 0.001);

    velExp.linear = 0;
    velExp.angular = -(CHASSIS_VELOCITY_MAX_ANGULAR + 0.01f);
    velAct = chassis_ControlVelocity(&velExp);
    DOUBLES_EQUAL(0, velAct.linear,  0.001);
    DOUBLES_EQUAL(-CHASSIS_VELOCITY_MAX_ANGULAR, velAct.angular, 0.001);
};

TEST(Stub, chassis_ControlVelocity_should_SaveCurvature_when_VelocityOverLimits)
{
    chassis_velocity_s velTarget = { 
        .linear = CHASSIS_VELOCITY_MAX_LINEAR * 2, 
        .angular = CHASSIS_VELOCITY_MAX_ANGULAR * 3
    };

    float curvatureExp = velTarget.linear / velTarget.angular;
    chassis_velocity_s velAct = chassis_ControlVelocity(&velTarget);
    float curvatureAct = velAct.linear / velAct.angular;

    DOUBLES_EQUAL(curvatureExp, curvatureAct, 0.001);
}

/**
 * Chassis_ControlAcceleration
 */
TEST(Stub, Chassis_ControlAcceleration_should_DoNothing_when_TargetAndActualAreEqual)
{
    chassis_velocity_s exp = { 0.5f, 0.1f };
    chassis_velocity_s act = exp;
    chassis_velocity_s target = act;
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, 10);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

TEST(Stub, Chassis_ControlAcceleration_should_Increment_when_Accelerates)
{
    uint32_t time = 10;
    chassis_velocity_s exp = { 
        CHASSIS_ACCELERATION * (float)time / 1000.f, 
        0.0f 
    };
    chassis_velocity_s act = { 0.0f, 0.0f };
    chassis_velocity_s target = { CHASSIS_VELOCITY_MAX_LINEAR, 0.0f };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

TEST(Stub, Chassis_ControlAcceleration_should_Increment_when_Deccelaretes)
{
    uint32_t time = 10;
    chassis_velocity_s exp = { 
        CHASSIS_VELOCITY_MAX_LINEAR - CHASSIS_DECCELERATION * (float)time / 1000.f, 
        0.0f 
    };
    chassis_velocity_s act = { CHASSIS_VELOCITY_MAX_LINEAR, 0.0f };
    chassis_velocity_s target = { 0.0f, 0.0f };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

TEST(Stub, Chassis_ControlAcceleration_should_IncrementHalfStep_when_TargetAndActualBacomeEqual)
{
    uint32_t time = 10;
    float step = CHASSIS_ACCELERATION * (float)time / 1000.f;
    chassis_velocity_s exp = { 
        -step, 
        0.0f 
    };
    chassis_velocity_s act = { 
        -step / 2, 
        0.0f 
    };
    chassis_velocity_s target = { -step, 0.0f };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}


TEST(Stub, Chassis_ControlAcceleration_should_IncrementAnglular_when_Accelerates)
{
    uint32_t time = 10;
    chassis_velocity_s exp = { 
        0.0f,
        -CHASSIS_ACCELERATION_ANGULAR * (float)time / 1000.f, 
    };
    chassis_velocity_s act = { 0.0f, 0.0f };
    chassis_velocity_s target = { 0.0f, -CHASSIS_VELOCITY_MAX_ANGULAR };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

TEST(Stub, Chassis_ControlAcceleration_should_IncrementAnglular_when_Deccelaretes)
{
    uint32_t time = 10;
    chassis_velocity_s exp = { 
        0.0f,
        -CHASSIS_VELOCITY_MAX_ANGULAR + CHASSIS_DECCELERATION_ANGULAR * (float)time / 1000.f, 
    };
    chassis_velocity_s act = { 0.0f, -CHASSIS_VELOCITY_MAX_ANGULAR };
    chassis_velocity_s target = { 0.0f, 0.0f };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

TEST(Stub, Chassis_ControlAcceleration_should_IncrementHalfDtepAnglular_when_Accelerates)
{
    uint32_t time = 10;
    float step = CHASSIS_ACCELERATION * (float)time / 1000.f;
    chassis_velocity_s exp = { 0.0f, step };
    chassis_velocity_s act = { 0.0f, step / 2 };
    chassis_velocity_s target = { 0.0f, step };
    chassis_velocity_s restricted = chassis_ControlAcceleration(&target, &act, time);
    DOUBLES_EQUAL(exp.linear, restricted.linear,  0.001);
    DOUBLES_EQUAL(exp.angular, restricted.angular, 0.001);
}

/**
 * Chassis_CalculatePosition
 */
TEST(Stub, chassis_CalculatePosition_should_CalculatePosition_when_GoForward)
{
    uint32_t timeDelta_ms = 1000;
    chassis_velocity_s vel = { .linear = 1, .angular = 0 };
    chassis_position_s posActual = { 0, 0 ,0 };

    posActual = chassis_CalculatePosition(&posActual, &vel, timeDelta_ms);
    DOUBLES_EQUAL(1, posActual.x, 0.001);
    DOUBLES_EQUAL(0, posActual.y, 0.001);
    DOUBLES_EQUAL(0, posActual.phi, 0.001);
};

TEST(Stub, Chassis_CalculatePosition_should_CalculateAngle_when_RotatesOnPlace)
{
    uint32_t timeDelta_ms = 20;
    chassis_velocity_s vel = { .linear = 0, .angular = 1 };
    chassis_position_s posAct = { 0, 0, 0 };
    chassis_position_s posExp = { 0, 0, 0 };

    for (uint32_t i = 0; i < 50; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
        posExp.phi += 1 * 0.02;
        DOUBLES_EQUAL(posExp.phi, posAct.phi, 0.001);
    }
};

TEST(Stub, Chassis_CalculatePosition_should_CalculateAngle_when_RotatesOnPlace_and_Back)
{
    uint32_t time_ms = 1000;
    uint32_t timeDelta_ms = 20;
    uint32_t steps = time_ms / timeDelta_ms;
    chassis_velocity_s vel = { .linear = 0, .angular = 2.f * PI };
    chassis_position_s posAct = { 0, 0, 0 };

    /// 1-н оборот
    for (uint32_t i = 0; i < steps; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }
    DOUBLES_EQUAL(cos(0), cos(posAct.phi), 0.01);

    /// 2-й оборот
    for (uint32_t i = 0; i < steps; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }
    DOUBLES_EQUAL(cos(0), cos(posAct.phi), 0.01);

    /// 1-н оборот обратно
    vel.angular = -2.f * PI;
    for (uint32_t i = 0; i < steps; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }
    DOUBLES_EQUAL(cos(0), cos(posAct.phi), 0.01);

    /// 2-й оборот обратно
    for (uint32_t i = 0; i < steps; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }
    DOUBLES_EQUAL(cos(0), cos(posAct.phi), 0.01);
};

TEST(Stub, chassis_CalculatePosition_should_CalculatePosition_when_GoWithCurvature_1)
{
    uint32_t timeDelta_ms = 20;
    float V = 0.2f;
    uint32_t steps = (uint32_t) ((2.f * PI / V) * 2000 / (float)timeDelta_ms);
    
    chassis_velocity_s vel = { .linear = V, .angular = V };
    chassis_position_s posAct = { 0, 0, 0 };

    for (uint32_t i = 0; i < steps; i++) {
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }

    DOUBLES_EQUAL(0, posAct.x, 0.01);
    DOUBLES_EQUAL(0, posAct.y, 0.01);
    DOUBLES_EQUAL(cos(0), cos(posAct.phi), 0.01);
};

#include "values.txt" /// TODO: YMBOT-779. Собрать точный датасет

TEST(Stub, chassis_CalculatePosition_should_CalculatePosition_when_ProcessDataset)
{
    uint32_t timeDelta_ms = 20;
    double lefts[] = TEST_FULL_LEFT;
    double rights[] = TEST_FULL_RIGHT;
    uint32_t size = sizeof(rights) / sizeof(double);
    chassis_position_s posAct = { 0, 0, 0 };

    for (uint32_t i = 0; i < size; i++) {
        chassis_velocity_s vel = chassis_GetVelocity((float)lefts[i], (float)rights[i]);
        posAct = chassis_CalculatePosition(&posAct, &vel, timeDelta_ms);
    }

    float relation = 2.f * PI / (float)posAct.phi;

    DOUBLES_EQUAL(0, posAct.x, 0.01);   
    DOUBLES_EQUAL(0, posAct.y, 0.01);
    DOUBLES_EQUAL(1, relation, 0.05); ///< Погрешность должна быть в пределах 5%
};
