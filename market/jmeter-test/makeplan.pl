#!/usr/local/bin/perl -w
#
# Creates a JMeter test plan by parsing an Apache access log file.
#
# Public domain source code.
#
# This software was written and placed in the public
# domain by Geoff Mottram on September 21, 2004.
#
# THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHOR BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
# AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THIS SOFTWARE OR THE USE OR OTHER DEALINGS IN THIS SOFTWARE.
#
# // currently modified by Oleg Bogumirsky!!!!!!!!!!
use strict;

#
# An example access log input line looks like this:
#
# 128.121.234.89 - - [23/Jan/2001:01:19:02 -0500] "GET /min/close?... HTTP/1.0"
# 200 755 - "Mozilla/4.72 [en] (X11; I; Linux 2.2.14 i586)"
#

sub arg($) {
    my @params = split(/[=\n\r]/, $_[0]);
    $params[1] = '' if !defined($params[1]);
    print <<END;
<testelement class="org.apache.jmeter.protocol.http.util.HTTPArgument" name="">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="Argument.metadata">=</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="Argument.value">$params[1]</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPArgument.use_equals">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="Argument.name">$params[0]</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPArgument.always_encode">false</property>
</testelement>
END
    return ($params[0] eq 'auto') ? 1 : 0;
}

sub argsEnd() {
    print <<END;
</collection>
END
}

sub argsStart() {
    print <<END;
<collection class="java.util.LinkedList" propType="org.apache.jmeter.testelement.property.CollectionProperty" name="Arguments.arguments">
END
}

sub requestEnd() {
    print <<END;
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
</testelement>
</node>
END
}

sub requestStart($) {
    my $path = $_[0];
    print <<END;
<node>
<testelement class="org.apache.jmeter.protocol.http.sampler.HTTPSampler">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.path">$path</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">$path</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.protocol.http.sampler.HTTPSampler</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.method">GET</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPSampler.use_keepalive">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPSampler.image_parser">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPSampler.follow_redirects">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.monitor">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="HTTPSampler.auto_redirects">false</property>
<testelement class="org.apache.jmeter.config.Arguments" name="HTTPsampler.Arguments">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.config.Arguments</property>
END
}

sub testEnd($) {
    my $host = $_[0];
    print <<END;
<node>
<testelement class="org.apache.jmeter.config.ConfigTestElement">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.protocol.http.config.gui.HttpDefaultsGui</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.config.ConfigTestElement</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">HTTP Request Defaults</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.domain">$host</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.protocol">http</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="HTTPSampler.port">80</property>
<testelement class="org.apache.jmeter.config.Arguments" name="HTTPsampler.Arguments">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.config.Arguments</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">User Defined Variables</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
</testelement>
</node>
<node>
<testelement class="org.apache.jmeter.protocol.http.control.CookieManager">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.protocol.http.gui.CookiePanel</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.protocol.http.control.CookieManager</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">HTTP Cookie Manager</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="CookieManager.clearEachIteration">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
</node>
</node>
END
}

sub testStart() {
    print <<END;
<?xml version="1.0" encoding="UTF-8"?>
<node>
<testelement class="org.apache.jmeter.testelement.TestPlan">
<testelement class="org.apache.jmeter.config.Arguments" name="TestPlan.user_defined_variables">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.config.gui.ArgumentsPanel</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.config.Arguments</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">User Defined Variables</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.control.gui.TestPlanGui</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestPlan.serialize_threadgroups">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.testelement.TestPlan</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">Test Plan</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestPlan.functional_mode">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
<node>
<testelement class="org.apache.jmeter.timers.ConstantThroughputTimer">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.testbeans.gui.TestBeanGUI</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.timers.ConstantThroughputTimer</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.DoubleProperty" name="throughput">200.0</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">Constant Throughput Timer</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
</testelement>
</node>
<node>
<testelement class="org.apache.jmeter.timers.UniformRandomTimer">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.timers.gui.UniformRandomTimerGui</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.timers.UniformRandomTimer</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">Uniform Random Timer</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="RandomTimer.range">60.0</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="ConstantTimer.delay">0</property>
</testelement>
</node>
END
}

sub threadEnd() {
    print <<END;
</node>
END
}

sub threadStart($) {
    my $num = $_[0];
    #print(STDERR "THREAD $num\n");
    print <<END;
<node>
<testelement class="org.apache.jmeter.threads.ThreadGroup">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.LongProperty" name="ThreadGroup.start_time">1095436017000</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.threads.ThreadGroup</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="ThreadGroup.num_threads">10</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="ThreadGroup.scheduler">false</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.threads.gui.ThreadGroupGui</property>
<testelement class="org.apache.jmeter.control.LoopController" name="ThreadGroup.main_controller">
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.gui_class">org.apache.jmeter.control.gui.LoopControlPanel</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="LoopController.loops">1</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.test_class">org.apache.jmeter.control.LoopController</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">Loop Controller</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="TestElement.enabled">true</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.BooleanProperty" name="LoopController.continue_forever">false</property>
</testelement>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="TestElement.name">Thread Group $num</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.LongProperty" name="ThreadGroup.end_time">1095436017000</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="ThreadGroup.on_sample_error">continue</property>
<property xml:space="preserve" propType="org.apache.jmeter.testelement.property.StringProperty" name="ThreadGroup.ramp_time">1</property>
</testelement>
END
}

sub usage() {
    print(STDERR "\nUsage: makeplan.pl [-h HOST] [-t THREADS] [FILE1] [FILE2] ...\n");
    print(STDERR "Where: -h specifies the host name or ip address to test (default is \"localhost\").\n");
    print(STDERR "       -t specifies a non-zero number of threads to generate (default is 1).\n\n");
    print(STDERR "Reads each FILE in succession (or standard input), generating\n");
    print(STDERR "  a JMeter test plan to standard output.\n\n");
    exit(1);
}

sub run() {
    my ($arg, $cmd, $j, @list, $param, @req);

    my $host = 'localhost';
    my $threads = 1;
    my $i = 0;
    while ($i <= $#ARGV) {
	$arg = $ARGV[$i];
	if ($arg =~ m/^-/) {
	    if ($arg eq '-h') {			# host name or ip address
		if ($i == $#ARGV) {
		    usage();			# exits
		}
		$host = $ARGV[$i + 1];
		splice(@ARGV, $i, 2);		# remove options
	    } elsif ($arg eq '-t') {
		if ($i == $#ARGV) {
		    usage();			# exits
		}
		$threads = $ARGV[$i + 1];
		if ($threads =~ m/\D/) {
		    usage();			# exits
		}
		$threads = int($threads);
		if ($threads == 0) {
		    print(STDERR "\nNumber of threads must be greater than zero.\n");
		    usage();			# exits
		}
		splice(@ARGV, $i, 2);		# remove options
	    } else {
		usage();			# exits
	    }
	} else {
	    $i++;
	}
    }
    my $s = ($threads > 1) ? 's' : '';
    print(STDERR "Generating $threads thread$s for host \"$host\"\n");
    testStart();
    foreach $arg (@ARGV) {
	if (!open(FILE, $arg)) {
	    print(STDERR "Could not open $arg: $!\n");
	    next;
	}
	while (<FILE>) {
	    chomp();
	    s/  +/ /g;			# remove extra spaces
	    next if !(@list = split(/ /, $_, 8));
	    push(@req, $list[0]); # BO was 6
	}
	if (@req) {
	    my $count = $#req + 1;
	    my $rpt = $count / $threads;	# requests per thread
	    if ($count % $threads) {
		$rpt++;
	    }
	    my $k = 0;
	    for ($i = 0; $i < $threads; $i++) {
		last if !defined($req[$i]);
		threadStart($i + 1);
		for ($j = 0, $k = $i; $j < $rpt; $j++, $k += $threads) {
		    last if !defined($req[$k]);
		    @list = split(/\?/, $req[$k]);
		    requestStart($list[0]);
		    my $auto = ($list[0] eq '/min/minaret') ? 0 : 1;
		    #print(STDERR "Command: $cmd\n");
		    if (defined($list[1])) {
			argsStart();
			#print(STDERR "   Args: $list[1]\n");
			@list = split(/&/, $list[1]);
			foreach $param (@list) {
			    #print(STDERR "   Args: $i\n");
			    if (arg($param)) {
				$auto = 1;
			    }
			}
			if (!$auto) {
			    arg('auto=1');
			}
			argsEnd();
		    }
		    requestEnd();
		}
		threadEnd();
	    }
	}
    }
    testEnd($host);
}

run();
exit(0);