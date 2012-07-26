package probe;

import com.jprofiler.api.agent.probe.Probe;
import com.jprofiler.api.agent.probe.ProbeProvider;

// The probe provider class is specified with as a VM parameter as
// -Djprofiler.probeProvider=probe.LoadFactorProbeProvider
// so the JProfiler agent can initialize all interceptors at startup.
public class LoadFactorProbeProvider implements ProbeProvider {

    public Probe[] getProbes() {
        return new Probe[] {
            new LoadFactorInterceptor()
        };
    }
}
