#if defined(PROD_TESTING_PRESENT)
#include "signal_manager/prod_tests.h"
#include "rs485_driver.h"

#if   (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
#include "gsm_modems_lib/quectel/quectel_modem_lib.h"
#include "server_manager/server_manager.h"
#elif (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_25K)
#include "gsm_modems_lib/simcom/simcom_modem_lib.h"
#else
#error
#endif

#define __SNPRINTF_WLOG(...)                                             \
{                                                                        \
  System.Grab(portMAX_DELAY);                                            \
  snprintf(System.ptest_state, sizeof(System.ptest_state), __VA_ARGS__); \
  System.Release();                                                      \
  LOG(__VA_ARGS__);                                                      \
}

void prod_test_handler(void)
{ 
  uint32_t max_test_time=xTaskGetTickCount()+5*60*1024;
  
  Black_box.Grab();
  Black_box.fflush("LOG");
  Black_box.fsave();
  Black_box.Release();
  
  //отключаем все, что может мешать работе интерфейсов
  System.Grab(portMAX_DELAY);   
#if defined(DUTS_PRESENT)
  for(uint8_t i=0; i<MAX_DUTS; i++)  System.sensor_settings.dut[i].type=DUT_OFF;
#endif //DUTS_PRESENT
#if defined(TENZO_M_WEIGHER_PRESENT)
  System.sensor_settings.tenzo_m_weigher.interface=TENZO_M_WEIGHER_OFF;
#endif //
#if defined(NRF_BEACONS_SCANNER_PRESENT)
  System.sensor_settings.nrf_beacons_scanner.interface=NRF_BEACONS_SCANNER_OFF;
#endif //TENZO_M_WEIGHER_PRESENT
#if defined(ATOM_PRESENT)
  //
#endif //ATOM_PRESENT
#if defined(FRIDGE_PRESENT)
  System.sensor_settings.fridge.interface=FRIDGE_OFF;
#endif //FRIDGE_PRESENT
#if defined(IQFREEZE_PRESENT)
  System.sensor_settings.iqfreeze.interface=IQFREEZE_OFF;
#endif //IQFREEZE_PRESENT
#if defined(CAMERA_PRESENT)
  for(uint8_t i=0; i<MAX_CAMERAS; i++) System.sensor_settings.camera[i].interface=CAM_OFF;
#endif //CAMERA_PRESENT
#if defined(BR_PRESENT)          
  System.sensor_settings.br_settings.interface=BR_OFF;
#endif //BR_PRESENT
  
  System.Release();        
  
  static const char* const fill_str="----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";
  LOG("%s\n", fill_str);
  //LOG("%s\n", fill_str);
  __SNPRINTF_WLOG("Начало тестирования...\n");
  
#if (CHECK_PROT_BITS > 0)
  if(FLASH_OB_GetRDP()==RESET)
  {
    __SNPRINTF_WLOG("Тест %s %s %s\n", "проверки битов защиты", "RDP", "не пройден!!!");
    return;
  }
#else
#warning Отключена проверка битов защиты RDP в производственных тестах!
  __SNPRINTF_WLOG("Тест %s %s %s\n", "проверки битов защиты", "RDP", "пропущен!!!");
#endif //CHECK_PROT_BITS

#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
  if(FLASH_OB_GetWRP()!=0x7FFF) //no SPR MOD, all nWRP are set
  {
    __SNPRINTF_WLOG("Тест %s %s %s\n", "проверки битов защиты", "WRP", "не пройден!!!");
    return;
  }
#endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
  
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_25K)
  if((0x0FFF&FLASH_OB_GetWRP())!=0x0FFF) //all nWRP are set
  {
    __SNPRINTF_WLOG("Тест %s %s %s\n", "проверки битов защиты", "WRP", "не пройден!!!");
    return;
  }
#endif //DEVICE_TYPE_VEGA_MT_25K
  
  if(!(__GET_VBAT_IN_DETECT_STATE()))
  {
    __SNPRINTF_WLOG("Тест %s %s\n", "цепи детектирования внешнего питания", "не пройден!!!");
    return;
  }
  
  if(System.signal_state.external_voltage<13.9f)
  {
    __SNPRINTF_WLOG("Для проведения тестов напряжение питания должно быть не меньше 14В\n");
    return;
  }
  
  System.can_state.sec_flags.can_in_sleep=0;//для того чтобы GNSS приемник не засыпал
  
#if defined(GSENSOR_PRESENT)
  //тестируем акселерометр
  float g=0;
  static const uint8_t accel_test_attempt=8;
  
  for(uint8_t i=0; i<accel_test_attempt; i++)
  {
    vTaskDelay(350);
    System.Grab(portMAX_DELAY);
    g=pow((pow(System.signal_state.gsensor_axis_x, 2) + pow(System.signal_state.gsensor_axis_y, 2) + pow(System.signal_state.gsensor_axis_z, 2)), 0.5f);
    System.Release();
    
    if(System.signal_state.accel_no_init==0 && g>0.85f && g<1.15f)
    {
      break;
    }
    else if(i==(accel_test_attempt-1))
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "акселерометра", "не пройден!!!");
      return;
    }
  }
  
  __SNPRINTF_WLOG("Тест %s %s\n", "акселерометра", "пройден");
#endif //GSENSOR_PRESENT
  
#if defined(RS485_PRESENT)          
  static const uint8_t intface_test_attempt=8;
  //тестируем интерфейс RS-485
  for(uint8_t i=0; i<intface_test_attempt; i++)
  {
    static const uint8_t tx_ref[]={0xAA, 0x00, 0x00, 0x9E, 0x64, 0x6A};
    static const uint8_t rx_ref[]={0xAA, 0x00, 0x01, 0x9E, 0x01, 0x61, 0x33};
    uint8_t rx_buff[sizeof(rx_ref)];
    uint16_t rx_len;
    
    RS485_SetParam(115200, 150, "8N1");                        
    rx_len=RS485_TxRx((uint8_t*)tx_ref, sizeof(tx_ref), rx_buff, sizeof(rx_buff));
    
    if(rx_len==sizeof(rx_ref) && memcmp(rx_buff, rx_ref, sizeof(rx_ref))==0)
    {
      break;
    }
    else if(i==(intface_test_attempt-1))
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "RS-485", "не пройден!!!");
      return;
    }
    
    vTaskDelay(300); 
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "RS-485", "пройден");
#endif //RS485_PRESENT
  
#if defined(RS232_PRESENT)
#error
#endif //RS232_PRESENT
  
  //тестируем цифровые выходы и входы
  System.Grab(portMAX_DELAY);
  System.sensor_settings.mfi[0].input_type=DIG_IN;
  System.sensor_settings.mfi[0].polarity=ACTIVE_1;
  
  System.sensor_settings.mfi[1].input_type=DIG_IN;
  System.sensor_settings.mfi[1].polarity=ACTIVE_1;
  
  System.sensor_settings.mfi[2].input_type=DIG_IN;
  System.sensor_settings.mfi[2].polarity=ACTIVE_1; 
  
  System.sensor_settings.mfi[3].input_type=DIG_IN;
  System.sensor_settings.mfi[3].polarity=ACTIVE_1; 
  
  System.sensor_settings.mfi[4].input_type=DIG_IN;
  System.sensor_settings.mfi[4].polarity=ACTIVE_1;        
  System.Release();
  
  static const struct io_test_struct
  {
    uint8_t o1:1; //выход 2        (1 или 2 MT32K LTE)
    uint8_t o2:1; //выход 3 или 4  (3 или 4 MT32K LTE)
    uint8_t o3:1; //выход 5 или 6  (5 или 6 MT32K LTE)
    uint8_t o4:1; //выход 7 или 8  (7 или 8 MT32K LTE)
    uint8_t o5:1; //выход 9 или 10 (9 или 10 MT32K LTE)
    
    uint8_t ref_in1:1;
    uint8_t ref_in2:1;
    uint8_t ref_in3:1;
    uint8_t ref_in4:1;
    uint8_t ref_in5:1;
  }io_test[]=
  {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {0, 1, 0, 1, 0, 0, 1, 0, 1, 0}, {1, 0, 1, 0, 1,  1, 0, 1, 0, 1}, {1, 1, 0, 1, 1, 1, 1, 0, 1, 1}, {0, 1, 1, 0, 1, 0, 1, 1, 0, 1}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
  
  for(uint8_t i=0; i<sizeof(io_test)/sizeof(io_test[0]); i++)
  {
    System.signal_state.digital_output[1]=io_test[i].o1;
    System.signal_state.digital_output[3]=io_test[i].o2;
    System.signal_state.digital_output[5]=io_test[i].o3;
    System.signal_state.digital_output[7]=io_test[i].o4;
    System.signal_state.digital_output[9]=io_test[i].o5;
    
    vTaskDelay(300); 
    
    if(System.signal_state.digital_input[0]!=io_test[i].ref_in1 \
      || System.signal_state.digital_input[1]!=io_test[i].ref_in2 \
        || System.signal_state.digital_input[2]!=io_test[i].ref_in3 \
          || System.signal_state.digital_input[3]!=io_test[i].ref_in4 \
            || System.signal_state.digital_input[4]!=io_test[i].ref_in5 \
              )
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "цифровых входов/выходов", "не пройден!!!");
      return;
    }
  }
  
  System.Grab(portMAX_DELAY);
  System.sensor_settings.mfi[0].input_type=DIG_IN;
  System.sensor_settings.mfi[0].polarity=ACTIVE_0;
  
  System.sensor_settings.mfi[1].input_type=DIG_IN;
  System.sensor_settings.mfi[1].polarity=ACTIVE_0;
  
  System.sensor_settings.mfi[2].input_type=DIG_IN;
  System.sensor_settings.mfi[2].polarity=ACTIVE_0; 
  
  System.sensor_settings.mfi[3].input_type=DIG_IN;
  System.sensor_settings.mfi[3].polarity=ACTIVE_0; 
  
  System.sensor_settings.mfi[4].input_type=DIG_IN;
  System.sensor_settings.mfi[4].polarity=ACTIVE_0;        
  System.Release();
  
  for(uint8_t i=0; i<sizeof(io_test)/sizeof(io_test[0]); i++)
  {
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
    System.signal_state.digital_output[0]=io_test[i].o1;
#endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_25K)
    System.signal_state.digital_output[1]=io_test[i].o1;
#endif //DEVICE_TYPE_VEGA_MT_25K
    System.signal_state.digital_output[2]=io_test[i].o2;
    System.signal_state.digital_output[4]=io_test[i].o3;
    System.signal_state.digital_output[6]=io_test[i].o4;
    System.signal_state.digital_output[8]=io_test[i].o5;
    
    vTaskDelay(300); 
    
    if(System.signal_state.digital_input[0]==io_test[i].ref_in1 \
      || System.signal_state.digital_input[1]==io_test[i].ref_in2 \
        || System.signal_state.digital_input[2]==io_test[i].ref_in3 \
          || System.signal_state.digital_input[3]==io_test[i].ref_in4 \
            || System.signal_state.digital_input[4]==io_test[i].ref_in5 \
              )
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "цифровых входов/выходов", "не пройден!!!");
      return;
    }
  }
  
  __SNPRINTF_WLOG("Тест %s %s\n", "цифровых входов/выходов", "пройден");
  
  //тестируем аналоговые входы
  System.Grab(portMAX_DELAY);
  System.sensor_settings.mfi[0].input_type=ANALOG_IN;
  System.sensor_settings.mfi[1].input_type=ANALOG_IN;
  System.sensor_settings.mfi[2].input_type=ANALOG_IN;
  System.sensor_settings.mfi[3].input_type=ANALOG_IN;
  System.sensor_settings.mfi[4].input_type=ANALOG_IN;
  System.Release();
  
  static const struct analog_inputs_test_struct
  {
    uint8_t o1:1; //выход 2        (1 или 2 MT32K LTE)
    uint8_t o2:1; //выход 3 или 4  (3 или 4 MT32K LTE)
    uint8_t o3:1; //выход 5 или 6  (5 или 6 MT32K LTE)
    uint8_t o4:1; //выход 7 или 8  (7 или 8 MT32K LTE)
    uint8_t o5:1; //выход 9 или 10 (9 или 10 MT32K LTE)
  }analog_inputs_test[]=
  {{1, 1, 1, 1, 1}, {0, 1, 0, 1, 0}, {1, 0, 1, 0, 1}, {1, 1, 0, 1, 1}, {0, 0, 0, 0, 0}};
  
  
  for(uint8_t i=0; i<sizeof(analog_inputs_test)/sizeof(analog_inputs_test[0]); i++)
  {
    System.signal_state.digital_output[1]=analog_inputs_test[i].o1;
    System.signal_state.digital_output[3]=analog_inputs_test[i].o2;
    System.signal_state.digital_output[5]=analog_inputs_test[i].o3;
    System.signal_state.digital_output[7]=analog_inputs_test[i].o4;
    System.signal_state.digital_output[9]=analog_inputs_test[i].o5;
    vTaskDelay(500); 
    
    float in[5];
    float ref[5];
    
    System.Grab(portMAX_DELAY);
    if(analog_inputs_test[i].o1) ref[0]=System.signal_state.external_voltage;
    else                         ref[0]=0.0f;
    if(analog_inputs_test[i].o2) ref[1]=System.signal_state.external_voltage;
    else                         ref[1]=0.0f;
    if(analog_inputs_test[i].o3) ref[2]=System.signal_state.external_voltage;
    else                         ref[2]=0.0f;
    if(analog_inputs_test[i].o4) ref[3]=System.signal_state.external_voltage;
    else                         ref[3]=0.0f;
    if(analog_inputs_test[i].o5) ref[4]=System.signal_state.external_voltage;
    else                         ref[4]=0.0f;
    
    for(uint8_t j=0; j<sizeof(ref)/sizeof(ref[0]); j++)
    {
      in[j]=System.signal_state.analog_input[j];
    }
    System.Release();
    
    LOG("ain1: %.3f, ain2: %.3f, ain3: %.3f, ain4: %.3f, ain4: %.3f, ref1: %.3f, ref2: %.3f, ref3: %.3f, ref4: %.3f, ref5: %.3f\n", in[0], in[1], in[2], in[3], in[4], ref[0], ref[1], ref[2], ref[3], ref[4]);
    
    for(uint8_t j=0; j<sizeof(ref)/sizeof(ref[0]); j++)
    {
      if(in[j]>(ref[j]+0.25f) || in[j]<(ref[j]-0.25f))
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "аналоговых входов", "не пройден!!!");
        return;
      }
    }
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "аналоговых входов", "пройден");
  
#if defined(IGNITION_PRESENT)            
  //тестируем вход зажигания
  //System.Grab(portMAX_DELAY);
  System.sensor_settings.use_can_ignition=0;
  //System.Release();
  
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
  static const struct ign_test_struct
  {
    uint8_t o:1;
    uint8_t ref:1;
  }ign_test[]=
  {{1, 1}, {0, 0}};
  
  for(uint8_t i=0; i<sizeof(ign_test)/sizeof(ign_test[0]); i++)
  {
    System.signal_state.digital_output[10]=ign_test[i].o;
    vTaskDelay(300); 
    
    if(System.signal_state.ignition!=ign_test[i].ref)
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "входа зажигания и выходов 11,12", "не пройден!!!");
      return;
    }
  }
  
  for(uint8_t i=0; i<sizeof(ign_test)/sizeof(ign_test[0]); i++)
  {
    System.signal_state.digital_output[11]=ign_test[i].o;
    vTaskDelay(300); 
    
    if(System.signal_state.ignition!=ign_test[i].ref)
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "входа зажигания и выходов 11,12", "не пройден!!!");
      return;
    }
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "входа зажигания и выходов 11,12", "пройден");
  
  if(true)
  { 
    //тестируем выход питания BT
    System.Grab(portMAX_DELAY);
    System.sensor_settings.mfi[0].input_type=ANALOG_IN;
    System.signal_state.digital_output[11]=1; //переключаем реле на выход VBT (выход 11 или 12) ко входу 1
    System.Release();
    
    bool is_fail=false;  
    
    for(uint8_t i=0; i<2; i++)
    {
      float ain;
      float ref;
      
      if(i==0) {__EXT_BT_PWR_DIS(); ref=0.0f;}
      else     {__EXT_BT_PWR_EN();  ref=3.7f;}
      
      vTaskDelay(500);
      
      System.Grab(portMAX_DELAY);
      ain=System.signal_state.analog_input[0];
      System.Release();
      
      LOG("BT PWR, ain: %.3f, ref: %.3f\n", ain, ref);
      
      if(ain>(ref+0.20f) || ain<(ref-0.20f))
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "питания BT", "не пройден!!!");
        is_fail=true;
        break;
      }
    }
    
    System.signal_state.digital_output[11]=0;
    __EXT_BT_PWR_DIS();
    
    if(is_fail) return;
    
    __SNPRINTF_WLOG("Тест %s %s\n", "питания BT", "пройден");
  }
  
  if(true)
  {
    GPIO_InitTypeDef GPIO_InitStructure;
    
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_2MHz;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_OUT;
    GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_NOPULL;
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
    GPIO_Init(GPIOD, &GPIO_InitStructure); //UART 4 TX
        
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
    GPIO_Init(GPIOE, &GPIO_InitStructure); //UART 8 TX
    
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_NOPULL;
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
    GPIO_Init(GPIOD, &GPIO_InitStructure); //UART 4 RX
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
    GPIO_Init(GPIOE, &GPIO_InitStructure); //UART 8 RX
    
    bool is_fail=false; 
    
    for(;;)
    {
      GPIO_SetBits(GPIOD, GPIO_Pin_1);
      vTaskDelay(1);
      if(!GPIO_ReadInputDataBit(GPIOD, GPIO_Pin_0)) 
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "UART 1", "не пройден!!!");
        is_fail=true;
        break;
      }
      GPIO_ResetBits(GPIOD, GPIO_Pin_1);
      vTaskDelay(1);
      if(GPIO_ReadInputDataBit(GPIOD, GPIO_Pin_0)) 
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "UART 1", "не пройден!!!");
        is_fail=true;
        break;
      }
      __SNPRINTF_WLOG("Тест %s %s\n", "UART 1", "пройден");
      
      GPIO_SetBits(GPIOE, GPIO_Pin_1);
      vTaskDelay(1);
      if(!GPIO_ReadInputDataBit(GPIOE, GPIO_Pin_0)) 
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "UART 2", "не пройден!!!");
        is_fail=true;
        break;
      }
      GPIO_ResetBits(GPIOE, GPIO_Pin_1);
      vTaskDelay(1);
      if(GPIO_ReadInputDataBit(GPIOE, GPIO_Pin_0)) 
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "UART 2", "не пройден!!!");
        is_fail=true;
        break;
      }
      __SNPRINTF_WLOG("Тест %s %s\n", "UART 2", "пройден");
      
      break;
    }

    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AIN;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_NOPULL;
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
    GPIO_Init(GPIOD, &GPIO_InitStructure); //UART 4 TX
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
    GPIO_Init(GPIOE, &GPIO_InitStructure); //UART 8 TX
        
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
    GPIO_Init(GPIOD, &GPIO_InitStructure); //UART 4 RX
    
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
    GPIO_Init(GPIOE, &GPIO_InitStructure); //UART 8 RX
    
    if(is_fail) return;
  }
  
//#warning нет теста для KLINE
//#warning нет теста для NRF
#endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
  
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_25K)
  static const struct ign_test_struct
  {
    uint8_t o:1;
    uint8_t ref:1;
  }ign_test[]=
  {{1, 1}, {0, 0}};
  
  for(uint8_t i=0; i<sizeof(ign_test)/sizeof(ign_test[0]); i++)
  {
    System.signal_state.digital_output[0]=ign_test[i].o;
    vTaskDelay(300); 
    
    if(System.signal_state.ignition!=ign_test[i].ref)
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "входа зажигания", "не пройден!!!");
      return;
    }
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "входа зажигания", "пройден");
#endif //DEVICE_TYPE_VEGA_MT_25K
  
#endif //IGNITION_PRESENT    
  
#if defined(INTERNAL_AKB_PRESENT)
  //System.signal_state.digital_output[0]=1;
  //Тест проводить на напряжении 14В, для того чтобы был включен заряд акб!
  
  bool is_acc_ok=false;
  
  for(uint8_t i=0; i<16; i++)
  {
    vTaskDelay(300); 
    
    System.Grab(portMAX_DELAY);
    if(System.signal_state.internal_acc_voltage>3.5f) 
    {
      System.Release();
      is_acc_ok=true;
      break;
    }
    System.Release();
  }
  
  //System.signal_state.digital_output[0]=0;
  
  if(!is_acc_ok)
  {
    __SNPRINTF_WLOG("Тест %s %s\n", "заряда аккумулятора", "не пройден!!!");
    return;
  }
  System.Release();
  
  
  __SNPRINTF_WLOG("Тест %s %s\n", "заряда аккумулятора", "пройден");
#endif //INTERNAL_AKB_PRESENT  
  
#define CAN_REF_STD_ID 0x329
#define CAN_TEST_SPEED 250
  
  static const can_filter_t test_filter_can1[]=
  {
    {.bank_type=STD_ID_FILTER_BANK_TYPE, {.std.value1=0, .std.value2=0, .std.value3=0, .std.value4=0},}
  };
  
  static const can_filter_t test_filter_can2[]=
  {
    {.bank_type=STD_ID_FILTER_BANK_TYPE, {.std.value1=CAN_REF_STD_ID, .std.value2=0, .std.value3=0, .std.value4=0},}
  };
  
  CanTxMsg ref_can_tx_mess = {CAN_REF_STD_ID, 0, CAN_ID_STD, CAN_RTR_Data, 8, {255,1,2,3,4,5,6,7}};
  static const uint8_t can_test_attempt=4;

#if (HW_CAN_COUNT > 1)
  can_hw_deinit();
  can_hw_init(CAN_TEST_SPEED, test_filter_can1, sizeof(test_filter_can1), CAN_TEST_SPEED, test_filter_can2, sizeof(test_filter_can2), 0, NULL, 0);
   
  for(uint8_t i=0; i<can_test_attempt; i++)
  {
    CAN_Transmit_Blocked(CAN1, &ref_can_tx_mess, 200);
    can_rx_frame_t rx;
    if(filled_count_in_sfifo(&can_rx_sfifo[1]))
    {
      read_from_sfifo(&can_rx_sfifo[1], &rx);
      
      if(rx.id==ref_can_tx_mess.StdId && rx.dlen==ref_can_tx_mess.DLC && memcmp(rx.data, ref_can_tx_mess.Data, ref_can_tx_mess.DLC)==0)
      {
        break;
      }
    }
    
    while(filled_count_in_sfifo(&can_rx_sfifo[1])) read_from_sfifo(&can_rx_sfifo[1], &rx);
    
    if(i==(can_test_attempt-1))
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN2", "не пройден!!!");
      return;
    }
    
    vTaskDelay(200);
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN2", "пройден");
#else
  __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN2", "пропущен");
#endif //(HW_CAN_COUNT > 1)
  
#if (HW_CAN_COUNT > 2)
  can_hw_deinit();
  can_hw_init(CAN_TEST_SPEED, test_filter_can1, sizeof(test_filter_can1), 0, NULL, 0, CAN_TEST_SPEED, test_filter_can2, sizeof(test_filter_can2));
   
  for(uint8_t i=0; i<can_test_attempt; i++)
  {
    CAN_Transmit_Blocked(CAN1, &ref_can_tx_mess, 200);
    
    can_rx_frame_t rx;
    
    if(filled_count_in_sfifo(&can_rx_sfifo[2]))
    {
       read_from_sfifo(&can_rx_sfifo[2], &rx);
      
      if(rx.id==ref_can_tx_mess.StdId && rx.dlen==ref_can_tx_mess.DLC && memcmp(rx.data, ref_can_tx_mess.Data, ref_can_tx_mess.DLC)==0)
      {
        break;
      }
    }
    
    while(filled_count_in_sfifo(&can_rx_sfifo[2])) read_from_sfifo(&can_rx_sfifo[2], &rx);
    
    if(i==(can_test_attempt-1))
    {
      __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN3", "не пройден!!!");
      return;
    }
    
    vTaskDelay(200);
  }
  __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN3", "пройден");
#else
  __SNPRINTF_WLOG("Тест %s %s\n", "CAN1-CAN3", "пропущен");
#endif //(HW_CAN_COUNT > 2)
  
  //тестируем внутренний датчик температуры
  float temp;
  System.Grab(portMAX_DELAY);
  temp=System.signal_state.internal_temp;
  System.Release();
  if(temp<10.0f || temp>65.0f)
  {
    __SNPRINTF_WLOG("Тест %s %s (%.3fC)\n", "датчика температуры", "не пройден!!!", temp);
    return;
  }
  __SNPRINTF_WLOG("Тест %s %s (%.3fC)\n", "датчика температуры", "пройден", temp);
  
#if defined(TAMPERS_PRESENT)
  if(false)
  {
    __SNPRINTF_WLOG("Тест %s\n", "датчиков вcкрытия");
    uint8_t state=0;
    for(;;)
    {
      if(state==0)
      {
        __SNPRINTF_WLOG("Положите плату модемом вниз...\n");
        state++;
      }
      else if(state==1)
      {
        if(System.signal_state.tamper[0]==1 && System.signal_state.tamper[1]==0)
        {
          __SNPRINTF_WLOG("Ok\n");
          state++;
        }
      }
      else if(state==2)
      {
        __SNPRINTF_WLOG("Положите плату модемом вверх...\n");
        state++;
      }
      else if(state==3)
      {
        if(System.signal_state.tamper[0]==0 && System.signal_state.tamper[1]==1)
        {
          __SNPRINTF_WLOG("Ok\n");
          state++;
        }
      }
      else if(state==4)
      {
        __SNPRINTF_WLOG("Положите плату набок...\n");
        state++;
      }
      else if(state==5)
      {
        if(System.signal_state.tamper[0]==1 && System.signal_state.tamper[1]==1)
        {
          __SNPRINTF_WLOG("Ok\n");
          state++;
        }
      }
      else
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "датчиков вcкрытия", "пройден");
        break;
      }
      
      if(timeAfter(xTaskGetTickCount(), max_test_time))
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "датчиков вcкрытия", "не пройден!!!");
      }
      
      vTaskDelay(10); 
    }
  }
#endif //TAMPERS_PRESENT
  
  //тест gsm и gnss
  System.Grab(portMAX_DELAY);
  memcpy(System.connection_settings.server[MAX_SERVERS_COUNT-1].address, "89.189.183.233:5604", sizeof("89.189.183.233:5604"));
  System.connection_settings.server[MAX_SERVERS_COUNT-1].connection_period=0;
  System.connection_settings.server[MAX_SERVERS_COUNT-1].server_protocol=VEGA;           
  System.Release();
  
#if (MAX_SIM_COUNT != 2)
#error
#endif //(MAX_SIM_COUNT > 1)
  
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
  uint8_t current_sim_test=SERVER_manager.current_sim_id;
  uint8_t first_sim_test=current_sim_test; 
  //modem_initialized
#endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
  
  bool is_modem_ok[MAX_SIM_COUNT]={false, false};
  bool is_gnss_ok=false;
  
  for(;;)
  {
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
    if(!is_modem_ok[0] || !is_modem_ok[1])
    {
      //static char imei[16];
      //static char iccid[2][20];
      //без опции force опрашивть можно, т.к. ничего в модем не пишется, возвращается всегда GSM_RES_OK
      creg_state_t creg_state;
      get_creg_state(&creg_state, 0);
      
      System.Grab(portMAX_DELAY);
      if(SERVER_manager.force_change_sim==0 && current_sim_test==SERVER_manager.current_sim_id \
        && System.server_state.is_on && System.server_state.signal_rssi<=31 && System.server_state.signal_rssi>=15 \
          && System.server_state.serving_cell.MCC>0 \
            && System.server_state.tcp_connect_state[MAX_SERVERS_COUNT-1] \
              && creg_state.netLac>0 && creg_state.netCellId>0 && creg_state.regStatus==REGISTERED_FROM_HOME_OPERATOR_CREG_STATUS)
      {
        is_modem_ok[current_sim_test]=true;
        //snprintf(imei, sizeof(imei), "%s", System.server_state.IMEI);
        //snprintf(iccid[current_sim_test], sizeof(iccid[current_sim_test]), "%s", System.server_state.ICCID[current_sim_test]);
      }
      System.Release();
      
      if(first_sim_test==0)
      {
        if(current_sim_test==0 && is_modem_ok[0])
        {
          current_sim_test++;
          SERVER_manager.force_change_sim=1;
        }
      }
      else
      {
        if(current_sim_test==1 && is_modem_ok[1])
        {
          current_sim_test--;
          SERVER_manager.force_change_sim=1;
        }
      }
      
      if(is_modem_ok[0] && is_modem_ok[1])
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "GSM", "пройден");
        System.Grab(portMAX_DELAY);
        LOG("\nIMEI: %s\nICCID1: %s\nICCID2: %s\n", System.server_state.IMEI, System.server_state.ICCID[0], System.server_state.ICCID[1]);
        System.Release();
      }
    }
#endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
    
#if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_25K)
    if(!is_modem_ok[0] || !is_modem_ok[1])
    {
      //без опции force опрашивть можно, т.к. ничего в модем не пишется, возвращается всегда GSM_RES_OK
      creg_state_t creg_state[MAX_SIM_COUNT];
      get_ds_creg_state(&creg_state[0], &creg_state[1], 0);
      
      System.Grab(portMAX_DELAY);
      for(uint8_t i=0; i<MAX_SIM_COUNT; i++)
      {
        if(System.server_state.is_on && System.server_state.signal_rssi<=31 && System.server_state.signal_rssi>=15 \
          && System.server_state.serving_cell.MCC>0 \
            && System.server_state.tcp_connect_state[MAX_SERVERS_COUNT-1] \
              && creg_state[i].netLac>0 && creg_state[i].netCellId>0 && creg_state[i].regStatus==REGISTERED_FROM_HOME_OPERATOR_CREG_STATUS)
        {
          is_modem_ok[i]=true;
        }
      }
      System.Release();
      
      if(is_modem_ok[0] && is_modem_ok[1])
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "GSM", "пройден");
      }
    }
#endif //DEVICE_TYPE_VEGA_MT_25K
        
    if(!is_gnss_ok)
    {
      System.Grab(portMAX_DELAY);
      if(System.gnss_state.is_on && System.gnss_state.receiver_is_ok && System.gnss_state.fix_type==FIX3D
         && System.gnss_state.sat_inuse>5 && System.gnss_state.total_sat_inview>6)
      {
        is_gnss_ok=true;
      }
      System.Release();  
      if(is_gnss_ok)
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "GNSS", "пройден");
      }
    }
    
    if(is_modem_ok[0] && is_modem_ok[1] && is_gnss_ok) break;
    
    if(timeAfter(xTaskGetTickCount(), max_test_time))
    {
      if(!is_gnss_ok)
      {
        __SNPRINTF_WLOG("Тест %s %s\n", "GNSS", "не пройден!!!");
      }
      else
      {
        __SNPRINTF_WLOG("Тест %s %s (SIM1: %s, SIM2: %s)\n", "GSM", "не пройден!!!", (is_modem_ok[0])?"ok":"err", (is_modem_ok[1])?"ok":"err");
      }
      
      return;
    }
    vTaskDelay(10); 
  }

  
  #if (DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE == DEVICE_TYPE_VEGA_MT_32K_LTE_V2)
  if(true)
  {
    if(!__GET_TAMPER1_STATE()) {__SNPRINTF_WLOG("Тест %s %s\n", "датчика вскрытия 1", "не пройден"); return;}
    if(!__GET_TAMPER2_STATE()) {__SNPRINTF_WLOG("Тест %s %s\n", "датчика вскрытия 2", "не пройден"); return;}
    
    for(uint8_t i=0; i<MAX_TAMPERS; i++)
    {
      __SNPRINTF_WLOG("Зажмите тампер %hhu...\n", i+1);
      
      uint32_t end_test_tamper_time;
      
      end_test_tamper_time=xTaskGetTickCount()+40000;
      for(;;)
      {
        if(i==0)
        {
          if(!__GET_TAMPER1_STATE() && __GET_TAMPER2_STATE()) {__SNPRINTF_WLOG("Тест %s %hhu %s\n", "датчика вскрытия", i+1, "пройден"); break;}
        }
        else
        {
          if(!__GET_TAMPER2_STATE() && __GET_TAMPER1_STATE()) {__SNPRINTF_WLOG("Тест %s %hhu %s\n", "датчика вскрытия", i+1, "пройден"); break;}
        }
                
        if(timeAfter(xTaskGetTickCount(), end_test_tamper_time))
        {
          __SNPRINTF_WLOG("Тест %s %hhu %s\n", "датчика вcкрытия", i+1, "не пройден!!!");
          LedCAN_Off();
          return;
        }
        vTaskDelay(100);
        LedCAN_Toggle();
      }
    }
  }
  LedCAN_Off();
  #endif //DEVICE_TYPE_VEGA_MT_32K_LTE || DEVICE_TYPE_VEGA_MT_32K_LTE_V2
  
  __SNPRINTF_WLOG("Все тесты успешно пройдены\n");
  LOG("%s\n", fill_str);
}
#endif //PROD_TESTING_PRESENT