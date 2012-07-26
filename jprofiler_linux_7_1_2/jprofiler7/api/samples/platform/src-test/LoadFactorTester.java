
// This is an interface for testing different load factors for hash maps.
// It illustrates how interceptors can check class hierarchies at runtime and insert themselves into
// arbitrary implementations of an interface (see interceptors/LoadFactorInterceptor.java)
public interface LoadFactorTester {
    void testLoadFactor(float loadFactor, boolean warmup);
}
