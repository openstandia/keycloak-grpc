package jp.openstandia.keycloak.grpc;

public class BuilderWrapper<T> {
    public interface Task<T, V> {
        T run(T builer, V value);
    }

    private T b;

    private BuilderWrapper(T b) {
        this.b = b;
    }

    public static <T> BuilderWrapper<T> wrap(T b) {
        return new BuilderWrapper<T>(b);
    }

    public <V> BuilderWrapper<T> nullSafe(V value, Task<T, V> t) {
        if (value != null) {
            t.run((T) this.b, value);
        }
        return this;
    }

    public T unwrap() {
        return b;
    }
}