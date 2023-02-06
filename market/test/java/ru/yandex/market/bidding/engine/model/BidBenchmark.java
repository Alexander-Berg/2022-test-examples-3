package ru.yandex.market.bidding.engine.model;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.yandex.market.bidding.model.Place;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 25.11.15
 * Time: 15:47
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BidBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(BidBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Benchmark
    public BidBuilder newBidBuilder() {
        BidBuilder builder = new BidBuilder();
        for (Place place : Place.values()) {
            builder.setPlaceBid(place, new BidBuilder.PlaceBid((short) place.code()));
        }
        return builder;
    }

    @Benchmark
    public Bid newLiteBid() {
        BidBuilder builder = newBidBuilder();
        return builder.build(Bid.class);
    }

    @Benchmark
    public FatBid newFatBid() {
        return new FatBid(newBidBuilder());
    }

    @Benchmark
    public int getLiteBidValue() {
        Bid bid = newLiteBid();

        int result = 0;
        for (Place place : Place.values()) {
            result += bid.view(place).value(bid);
        }
        return result;
    }

    @Benchmark
    public int getFatBidValue() {
        FatBid bid = newFatBid();

        int result = 0;
        for (Place place : Place.values()) {
            result += bid.get(place).value();
        }
        return result;
    }
}
