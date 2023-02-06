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
public class TestTwoPhotoScenario implements AliceScenario {
    private static final Logger logger = LoggerFactory.getLogger(TestTwoPhotoScenario.class);
    private final DivTemplateProcessor processor = new DivTemplateProcessor("two_photos.ftj");

    @Override
    public int match(ListF<String> text) {
        return text.containsTs("twophotos") ? 100 : 0;
    }

    @Override
    public ResponseProto.TScenarioRunResponse run(long uid, HttpServletRequestX reqX, ListF<String> words, RequestProto.TScenarioRunRequest request) {
        MapF<String, String> data = Cf.map(
                "image_url_1", "https://www.freepngimg.com/thumb/mario_bros/92562-land-art-thumb-bros-mario-super-3d.png",
                "image_url_2", "https://avatars.mds.yandex.net/get-pdb/1539962/9b0840d5-eef0-49ad-adae-521ab8fedeb5/s1200"
        );

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
