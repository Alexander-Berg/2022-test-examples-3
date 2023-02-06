/*
 * @title Тестирование распаршивания зависимостей скрипта из его тела
 * @type default
 * @version 2011-12-03T10:15:30+01:00
 * @dependencies metaDependency, commonDependency
 */
def someAssignment = objectTemplate.commonDependency
someMethodCall(objectTemplate?.optionalDependency)
objectTemplate.requiredDependency
objectTemplate.
        lineBreakDependency1
objectTemplate
        .lineBreakDependency2
objectTemplate?.
        lineBreakDependency3
objectTemplate
        ?.lineBreakDependency4
return 'OK'