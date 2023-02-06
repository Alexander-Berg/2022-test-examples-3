#ifndef __YMBOT_TEST_PLATFORM_MOTOR_TEST_MOTOR_H
#define __YMBOT_TEST_PLATFORM_MOTOR_TEST_MOTOR_H

#include <CppUTest/TestHarness.h>
#include <CppUTestExt/MockSupport.h>

#include "motor.h"
#include "sdc.h"

#include <string>
#include <cstring>

/*************************************************************************************************/

#define TEST_MOTOR_MOTOR_ID   (uint16_t)6u

/*************************************************************************************************/

class TMessage {
public:
    TMessage(sdc_base_e messageType) {
        this->id = TEST_MOTOR_MOTOR_ID | (uint16_t)messageType;
        length = SDC_MESSAGE_LENGTH;
        memset(data, 0, SDC_MESSAGE_LENGTH);
    }

    uint8_t data[SDC_MESSAGE_LENGTH];
    uint16_t id;
    uint8_t length;

    bool operator==(const TMessage& inst) const {
	    return (id == inst.id &&
                length == inst.length &&
                !(bool)memcmp(data, inst.data, SDC_MESSAGE_LENGTH));
    }

    bool operator!=(const TMessage& inst) const {
	    return !(*this == inst);
    }
};

class TMassageCommand : public TMessage {
public: 
    TMassageCommand() : TMessage(SDC_BASE_COMMAND) {
        control.field = 0;
    }

    void SetControlEnable(bool bit) { 
        control.enable = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field; 
    }

    void SetControlCompensatorReset(bool bit) { 
        control.compensatorReset = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field; 
    }

    void SetControlDynBrakeEnable(bool bit) { 
        control.dynBrakeEnable = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field;
    } 

    void SetControlFaultReset(bool bit) { 
        control.faultReset = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field;
    }

    void SetControlOperationCycle(bool bit) { 
        control.operationCycle = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field;
    }

    void SetControlCalibrationStart(bool bit) { 
        control.calibrationStart = bit;
        data[SDC_COMMAND_OFFSET_CONTROL] = control.field;
    }

    void SetSpeedTarget_Hz(float speed_Hz) { 
        int16_t speed_mHz = static_cast<int16_t>(speed_Hz * 1000.f);
        memcpy(&data[SDC_COMMAND_OFFSET_FREQUENCY], &speed_mHz, 2);
    }

    void SetSpeedTarget_mHz(int16_t speed_mHz) { 
        memcpy(&data[SDC_COMMAND_OFFSET_FREQUENCY], &speed_mHz, 2);
    }

private:
    sdc_motor_control_u control;
};

class TMassageFeedback : public TMessage {
public: 
    TMassageFeedback() : TMessage(SDC_BASE_FEEDBACK) {
        status4.field = 0;
        status5.field = 0;
    }

    void SetStatus4Enable(bool bit) { 
        status4.enable = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }
    void SetStatus4CompensatorReset(bool bit) { 
        status4.compensatorReset = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field; 
    }
    void SetStatus4CommonCurrentFault(bool bit) { 
        status4.commonCurrentFault = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    } 
    void SetStatus4PhaseCurrentFault(bool bit) { 
        status4.phaseCurrentFault = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }
    void SetStatus4DynBrakeEnable(bool bit) { 
        status4.dynBrakeEnable = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }
    void SetStatus4PositionSensorFault(bool bit) { 
        status4.positionSensorFault = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }
    void SetStatus4PowerStageFaul(bool bit) { 
        status4.powerStageFaul = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }
    void SetStatus4OperationCyclel(bool bit) { 
        status4.operationCycle = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_4] = status4.field;
    }

    void SetStatus5Overheat(bool bit) { 
        status5.overheat = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_5] = status5.field;
    }
    void SetStatus5TemperatureSensorFault(bool bit) { 
        status5.temperatureSensorFault = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_5] = status5.field;
    }
    void SetStatus5PowerSupplyVoltageFault(bool bit) { 
        status5.powerSupplyVoltageFault = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_5] = status5.field;
    }
    void SetStatus5FaultReset(bool bit) { 
        status5.faultReset = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_5] = status5.field;
    }
    void SetStatus5CalibrationStart(bool bit) { 
        status5.calibrationStart = bit;
        data[SDC_FEEDBACK_OFFSET_STATUS_5] = status5.field;
    }

    void SetSpeedActual_Hz(float speed_Hz) { 
        int16_t speed_mHz = static_cast<int16_t>(speed_Hz * 1000.f);
        memcpy(&data[SDC_FEEDBACK_OFFSET_FREQUENCY], &speed_mHz, 2);
    }

    void SetCurrent_A(float current_A) { 
        int16_t current_10mA = static_cast<int16_t>(current_A * 100.f);
        memcpy(&data[SDC_FEEDBACK_OFFSET_CURRENT], &current_10mA, 2);
    }

private:
    sdc_motor_status_4_u status4;
    sdc_motor_status_5_u status5;
};

class TMassageFeedback0 : public TMessage {
public: 
    TMassageFeedback0() : TMessage(SDC_BASE_FEEDBACK_0) {
        statusCalibration.field = 0;
    }

    void SetStatusCalibrationCalibrated(bool bit) { 
        statusCalibration.calibrated = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetStatusCalibrationCalibrating(bool bit) { 
        statusCalibration.calibrating = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field; 
    }

    void SetStatusCalibrationPowerSupplyVoltageFault(bool bit) { 
        statusCalibration.powerSupplyVoltageFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    } 

    void SetStatusCalibrationPowerStageFault(bool bit) { 
        statusCalibration.powerStageFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetStatusCalibrationPositionSensorFault(bool bit) { 
        statusCalibration.positionSensorFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetStatusCalibrationVelocitySensorFault(bool bit) { 
        statusCalibration.velocitySensorFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetStatusCalibrationPhaseACurrentFault(bool bit) { 
        statusCalibration.phaseACurrentFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetStatusCalibrationPhaseBCurrentFault(bool bit) { 
        statusCalibration.phaseBCurrentFault = bit;
        data[SDC_FEEDBACK_0_OFFSET_CALIBRATION] = statusCalibration.field;
    }

    void SetSpeed_Hz(float speed_Hz) { 
        int16_t speed_mHz = static_cast<int16_t>(speed_Hz * 1000.f);
        memcpy(&data[SDC_FEEDBACK_OFFSET_FREQUENCY], &speed_mHz, 2);
    }

    void SetCurrent_A(float current_A) { 
        int16_t current_10mA = static_cast<int16_t>(current_A * 100.f);
        memcpy(&data[SDC_FEEDBACK_OFFSET_CURRENT], &current_10mA, 2);
    }

private:
    sdc_motor_status_calibration_u statusCalibration;
};

SimpleString StringFrom(TMessage& message) {
    using namespace std;

    string result {"\n"};

    result += "id     " + to_string(message.id) + "\n";
    result += "length " + to_string(message.length) + "\n";
    result += "data   ";

    for (uint8_t i = 0; i < message.length; i++) {
        char hex[4]; ///< xx_\n
        sprintf(hex, "%02x ", message.data[i]);
        result.append(hex);
    }

    return SimpleString(result.c_str());
}

/*************************************************************************************************/

#endif /* __YMBOT_TEST_PLATFORM_MOTOR_TEST_MOTOR_H */