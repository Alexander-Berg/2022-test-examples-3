package ru.yandex.chemodan.app.hackathon;

import ru.yandex.alice.megamind.protos.scenarios.RequestProto;
import ru.yandex.alice.megamind.protos.scenarios.ResponseProto;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.web.servlet.HttpServletRequestX;

/**
 * @author tolmalev
 */
public class TestOnePhotoScenario implements AliceScenario {
    private static final Logger logger = LoggerFactory.getLogger(TestOnePhotoScenario.class);
    private final DivTemplateProcessor processor = new DivTemplateProcessor("one_photo.ftj");

    @Override
    public int match(ListF<String> text) {
        return text.containsTs("onephoto") ? 100 : 0;
    }

    @Override
    public ResponseProto.TScenarioRunResponse run(long uid, HttpServletRequestX reqX, ListF<String> words, RequestProto.TScenarioRunRequest request) {
        MapF<String, String> data = Cf.map("image_url", "https://blog.ginyes.org/wp-content/uploads/2017/03/Mario_Artwork_alt_-_Super_Mario_3D_World.png");

        if (words.size() > 1) {
            data = data.plus1("text", words.drop(1).mkString(" "));
        }

        String divJson = processor.processTemplate(data.uncheckedCast());
        logger.info("div json: {}", divJson);

        return ResponseProto.TScenarioRunResponse
                .newBuilder()
                .setResponseBody(ResponseProto.TScenarioResponseBody
                        .newBuilder()
                        .setLayout(ResponseProto.TLayout
                                .newBuilder()
                                .addCards(ResponseProto.TLayout.TCard
                                        .newBuilder()
//                                         .setDivCard(divJson) see: MEGAMIND-378
                                )
                        ))
                .build();
    }
}
