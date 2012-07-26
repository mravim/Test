package probe;

import com.jprofiler.api.agent.probe.*;

// A sample interceptor that inserts itself in into all calls to
//          java.awt.EventQueue.dispatchEvent(java.awt.AWTEvent event)
// records the execution type broken down by the class name of the event parameter and attaches this data to the call tree.
// The Call Tree view annotates the payload data at the appropriate call stack, the Hot
// Spots view gets an additional "AWT event types" hot spot type.
//
// Another example for an interceptor can be found the "platform" sample. There you can see how to insert an interceptor
// into an interface-based class hierarchy.

public class AWTEventProbe implements InterceptorProbe {

    public void interceptionEnter(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Object[] parameters) {
        // the "loadFactor" parameter is the content of the payload
        PayloadInfo payloadInfo = context.createPayloadInfo(parameters[0].getClass().getName());
        // save payload for use in interceptionExit
        context.push(payloadInfo);
    }

    public void interceptionExit(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Object returnValue) {
        // get payload that was saved in interceptionEnter
        PayloadInfo payloadInfo = context.pop();
        // in case of nested interceptions, only handle the outermost interception
        if (context.isPayloadStackEmpty()) {
            // perform the time measurement relative to the creation of the payload info and
            // attach the payload to the current call stack, so it is displayed in the call tree and the hot spot list
            context.addPayloadInfo(payloadInfo.calculateTime());
        }
    }

    public void interceptionExceptionExit(InterceptorContext context, Object object, Class declaringClass, String declaringClassName, String methodName, String methodSignature, Throwable throwable) {
        // ignore exceptions
        context.pop();
    }

    public ProbeMetaData getMetaData() {
        return ProbeMetaData.create("AWT event types").payload(true);
    }

    public InterceptionMethod[] getInterceptionMethods() {
        return new InterceptionMethod[] {
            // this is java.awt.EventQueue.dispatchEvent(java.awt.AWTEvent event)
            new InterceptionMethod("java.awt.EventQueue", "dispatchEvent",  "(Ljava/awt/AWTEvent;)V")
        };
    }
}
