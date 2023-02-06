#pragma once

#include <util/generic/string.h>

const TString RollbackMessage = "Rollback: ";

const TString SendingPingMessage = "Проверка связи...";
const TString ReceivingPingMessage = "Ожидание сигнала от машины...";
const TString UnlockingCarMessage = "Машина открывается...";
const TString UnlockingHoodMessage = "Капот разблокируется...";
const TString CheckUnlockedHoodMessage = "Ожидание состояния замка капота: разблокирован...";

const TString StopEngineMessage = "Заглушите двигатель";
const TString StoppingEngineMessage = "Двигатель глушится...";
const TString CheckStoppedEngineMessage = "Ожидание состояния двигателя: заглушен...";
const TString StartEngineMessage = "Заведите двигатель";
const TString StartingEngineMessage = "Двигатель заводится...";
const TString CheckStartedEngineMessage = "Ожидание состояния двигателя: заведен...";

const TString CheckPMessage = "Ожидание состояния селектора передач: P...";
const TString SelectPMessage = "А теперь переведите селектор передач в положение P";
const TString SelectRMessage = "Переведите селектор передач в положение R";

const TString PressBrakePedalMessage = "Нажмите педаль тормоза";
const TString ReleaseBrakePedalMessage = "Отпустите педаль тормоза";

const TString EnablingRadioblockMessage = "Радиоблокировка включается...";
const TString CheckEnabledRadioblockMessage = "Ожидание состояния радиоблокировки: включена..";
const TString DisablingRadioblockMessage = "Радиоблокировки выключается...";
const TString CheckDisabledRadioblockMessage = "Ожидание состояния радиоблокировки: отключена...";
const TString EnablingMoveblockMessage = "Блокировка по движению включается...";
const TString CheckEnabledMoveblockMessage = "Ожидание состояния блокировки по движению: включена...";
const TString DisablingMoveblockMessage = "Блокировка по движению выключается...";
const TString CheckDisabledMoveblockMessage = "Ожидание состояния блокировки по движению: отключена...";
const TString SmashMessage = "Постучите по корпусу в месте установки метки";

const TString CountBlinksMessage = "Посчитайте количество миганий фар";
const TString EnterBlinksMessage = "Введите число миганий фар";
const TString ReadyBlinksMessage = "Вы готовы посчитать количество миганий фар?";
const TString CountHornsMessage = "Посчитайте количество гудков";
const TString EnterHornsMessage = "Введите число гудков";
const TString ReadyHornsMessage = "Вы готовы посчитать количество гудков?";
const TString CountHoodLockingsMessage = "Посчитайте визуально количество щелчков замка капота";
const TString EnterHoodLockingsMessage = "Введите количество щелчков замка капота";
const TString ReadyHoodLockingsMessage = "Вы готовы посчитать количество щелчков замка капота?";

const TString DippedBeamOnMessage = "Включите ближний свет";
const TString DippedBeamOffMessage = "А теперь выключите ближний свет";
const TString HighBeamOnMessage = "Включите дальний свет";
const TString HighBeamOffMessage = "А теперь выключите дальний свет";
const TString HandbrakeOnMessage = "Поднимите ручной тормоз";
const TString HandbrakeOffMessage = "А теперь опустите ручной тормоз";

const TString DriverDoorOnMessage = "Откройте водительскую дверь";
const TString DriverDoorOffMessage = "А теперь закройте водительскую дверь";
const TString PassengerDoorOnMessage = "Откройте пассажирскую дверь";
const TString PassengerDoorOffMessage = "А теперь закройте пассажирскую дверь";
const TString LeftRearDoorOnMessage = "Откройте левую заднюю дверь";
const TString LeftRearDoorOffMessage = "А теперь закройте левую заднюю дверь";
const TString RightRearDoorOnMessage = "Откройте правую заднюю дверь";
const TString RightRearDoorOffMessage = "А теперь закройте правую заднюю дверь";
const TString HoodOnMessage = "Откройте капот";
const TString HoodOffMessage = "А теперь закройте капот";
const TString TrunkOnMessage = "Откройте багажник";
const TString TrunkOffMessage = "А теперь закройте багажник";

const TString YouAreNotPreparedMessage = "Вы не готовы";
const TString IncorrectNumberMessage = "Введено неправильное число: ожидали %ld, получили %ld";

const TString WarmupPrepareMessage = "Выйдите из машины, закройте все двери";
const TString WarmupStartMessage = "Прогрев включается...";
const TString WarmupStopMessage = "Прогрев выключается...";

const TString WindowsOpeningMessage = "Окна открываются...";
const TString WindowsOpenCheckMessage = "Окна открыты?";
const TString WindowsOpenMessage = "Откройте окна";
const TString WindowsClosingMessage = "Окна закрываются...";
const TString WindowsCloseCheckMessage = "Окна закрыты?";

const TString LockDoorPrepareMessage = "Выйдите из машины, закройте все двери";
const TString LockDoorLockMessage = "Центральный замок закрывается...";
const TString LockDoorLockCheckMessage = "Двери заперты?";
const TString LockDoorUnlockMessage = "Центральный замок открывается...";
const TString LockDoorUnlockCheckMessage = "Двери разблокированы?";
const TString LockDoorLockFailedMessage = "Не удалось заблокировать двери";
const TString LockDoorUnlockFailedMessage = "Не удалось разблокировать двери";

const TString FinishUnlockMessage = "Разблокируем машину...";
