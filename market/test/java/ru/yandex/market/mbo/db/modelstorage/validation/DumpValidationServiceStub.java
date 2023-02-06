package ru.yandex.market.mbo.db.modelstorage.validation;

import ru.yandex.market.mbo.db.modelstorage.validation.dump.AbstractDumpValidationService;
import ru.yandex.market.mbo.export.modelstorage.pipe.InheritParamsPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.Pipe;
import ru.yandex.market.mbo.export.modelstorage.pipe.PreprocessorPipePart;

import java.util.Collections;

public class DumpValidationServiceStub extends AbstractDumpValidationService {

    @Override
    public Pipe createCategoryPipe(long hid) {
        return Pipe.start()
            .then(InheritParamsPipePart.INSTANCE)
            .then(new PreprocessorPipePart(Collections.emptyList()))
            .build();
    }
}
