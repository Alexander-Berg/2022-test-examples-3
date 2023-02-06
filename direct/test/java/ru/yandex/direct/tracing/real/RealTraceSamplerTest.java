package ru.yandex.direct.tracing.real;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceSampler;
import ru.yandex.direct.tracing.data.TraceData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by snaury on 23/05/16.
 */
public class RealTraceSamplerTest {
    RealTrace trace;
    TraceSampler sampler;

    @Before
    public void prepare() {
        trace = RealTrace.builder().build();
        sampler = mock(TraceSampler.class);
        trace.setSampler(sampler);
    }

    @Test
    public void defaultSamplerateAssigned() {
        when(sampler.defaultSampleRate(trace)).thenReturn(42);
        when(sampler.sample(trace)).thenReturn(true);
        TraceData data = trace.snapshot(Trace.FULL);
        assertThat(data.getSamplerate(), is(42));
    }

    @Test
    public void samplerateIsNotOverwritten() {
        trace.setSamplerate(51);
        when(sampler.sample(trace)).thenReturn(true);
        TraceData data = trace.snapshot(Trace.FULL);
        verify(sampler, never()).defaultSampleRate(trace);
        assertThat(data.getSamplerate(), is(51));
    }

    @Test
    public void notSampled() {
        when(sampler.defaultSampleRate(trace)).thenReturn(42);
        when(sampler.sample(trace)).thenReturn(false);
        TraceData data = trace.snapshot(Trace.FULL);
        assertThat(data, is(nullValue()));
    }

    @Test
    public void sampleCalledOnlyOnce() {
        when(sampler.defaultSampleRate(trace)).thenReturn(42);
        when(sampler.sample(trace)).thenReturn(true);
        trace.snapshot(Trace.PARTIAL);
        trace.snapshot(Trace.PARTIAL);
        verify(sampler, times(1)).defaultSampleRate(trace);
        verify(sampler, times(1)).sample(trace);
    }
}
