package probe;

import com.jprofiler.api.agent.probe.*;

// A sample interceptor that inserts itself in into all calls to the
//          void testLoadFactor(float, boolean)
// method of classed that implement LoadFactorTester.
// The load factor parameter is extracted and execution times for the method are measured and attached to
// the call tree. The Call Tree view annotates the payload data at the appropriate call stacks, the Hot
// Spots view gets an additional "Hash map load factors" hot spot type.

public class LoadFactorInterceptor implements InterceptorProbe {

    private static final String TARGET_INTERFACE_NAME = "LoadFactorTester";

    public void interceptionEnter(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Object[] parameters) {
        // Methods are intercepted based on their signature only, so it's advisable to check the class hierarchy at runtime.
        // Note that the classes are not accessible directly, since the interceptor is in the boot classpath and
        // cannot access classes in the class path
        if (context.implementsInterface(object, TARGET_INTERFACE_NAME)) {
            // do not measure if "warmup" parameter is set to true
            if (((Boolean)parameters[1])) {
                context.push(null);
            } else {
                // the "loadFactor" parameter is the content of the payload
                PayloadInfo payloadInfo = context.createPayloadInfo("Load factor: " + parameters[0]);
                // save payload for use in interceptionExit
                context.push(payloadInfo);
            }
        }
    }

    public void interceptionExit(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Object returnValue) {
        if (context.implementsInterface(object, TARGET_INTERFACE_NAME)) {
            // get payload that was saved in interceptionEnter
            PayloadInfo payloadInfo = context.pop();
            // do not handle "warmup" calls, in case of nested interceptions, only handle the outermost interception
            if (payloadInfo != null && context.isPayloadStackEmpty()) {
                // perform the time measurement relative to the creation of the payload info
                payloadInfo.calculateTime();
                // attach the payload to the current call stack, so it is displayed in the call tree and the hot spot list
                context.addPayloadInfo(payloadInfo);
            }
        }
    }

    public void interceptionExceptionExit(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Throwable throwable) {
        if (context.implementsInterface(object, TARGET_INTERFACE_NAME)) {
            // ignore exceptions
            context.pop();
        }
    }

    public ProbeMetaData getMetaData() {
        return ProbeMetaData.create("Hash map load factors").payload(true);
    }

    public InterceptionMethod[] getInterceptionMethods() {
        return new InterceptionMethod[] {
            // this is void testLoadFactor(float, boolean)
            new InterceptionMethod("testLoadFactor",  "(FZ)V")
        };
    }
}
