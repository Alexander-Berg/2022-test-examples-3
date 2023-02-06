package ru.yandex.chemodan.app.telemost.ugcLive;

import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.ugcLive.model.LineInfo;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamAction;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamSchedule;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamState;
import ru.yandex.misc.lang.Check;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

public class UgcLiveClientStub implements UgcLiveClient {

    private final AtomicLong counter = new AtomicLong();

    private final MapF<Long, LineInfo> lines = Cf.hashMap();
    private final MapF<String, Long> episodes = Cf.hashMap();

    @Override
    public long createLine(String name) {
        long id = counter.incrementAndGet();
        lines.put(id, new LineInfo(Random2.R.nextAlnum(6), Option.empty(), Option.empty()));
        return id;
    }

    @Override
    public LineInfo getLineInfo(long lineId) {
        return lines.getOrThrow(lineId);
    }

    @Override
    public void deleteLine(long lineId) {
        Check.none(lines.getO(lineId).flatMapO(LineInfo::getEpisodeSlug), "Line has active episode");
        lines.removeTs(lineId);
    }

    @Override
    public String createEpisode(long lineId, String title) {
        Check.some(lines.getO(lineId), "Line not found");
        Check.none(lines.getO(lineId).flatMapO(LineInfo::getEpisodeSlug), "Line has active episode");

        String slug = Random2.R.nextAlnum(6);
        String rtmpKey = lines.getOrThrow(lineId).getRtmpKey();

        episodes.put(slug, lineId);
        lines.put(lineId, new LineInfo(rtmpKey, Option.of(slug), Option.of(StreamState.OFFLINE)));

        return slug;
    }

    @Override
    public Option<StreamState> getStreamState(String episodeSlug) {
        return episodes.getO(episodeSlug)
                .flatMapO(line -> lines.getO(line)
                        .flatMapO(LineInfo::getStreamState));
    }

    @Override
    public void performStreamAction(String episodeSlug, StreamAction action) {
        Option<Long> lineId = episodes.getO(episodeSlug);
        Option<LineInfo> line = lineId.flatMapO(lines::getO);
        Option<StreamState> state = line.flatMapO(LineInfo::getStreamState);

        if (state.isPresent()) {
            if (!state.get().getApplicableActions().containsTs(action)) {
                Assert.fail(action + " is not applicable to " + state.get());
            }
            lines.put(lineId.get(), new LineInfo(
                    line.get().getRtmpKey(),
                    line.get().getEpisodeSlug(),
                    Option.of(action.getResultState()))
            );
        }
    }

    @Override
    public void deleteEpisode(String episodeSlug) {
        long lineId = episodes.removeTs(episodeSlug);

        if (lines.getO(lineId).flatMapO(LineInfo::getEpisodeSlug).isSome(episodeSlug)) {
            String rtmpKey = lines.getOrThrow(lineId).getRtmpKey();
            lines.put(lineId, new LineInfo(rtmpKey, Option.empty(), Option.empty()));
        }
    }

    @Override
    public StreamSchedule getSchedule() {
        // TODO: StreamSchedule
        return new StreamSchedule();
    }
}
