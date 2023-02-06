package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;

public interface NeedsCmdSteps {

    DirectCmdSteps getDirectCmdSteps();
    <T extends NeedsCmdSteps> T withDirectCmdSteps(DirectCmdSteps directCmdSteps);
}
