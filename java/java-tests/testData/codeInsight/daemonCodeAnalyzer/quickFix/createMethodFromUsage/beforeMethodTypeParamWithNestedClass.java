// "Create method 'f' in 'Nested in aMethod() in CreateMethodTest'" "true"
public class CreateMethodTest {
  public <T> void aMethod(final T t) {
    class Nested {
        public T call() {
            T result = f<caret>(t);
            return result;
        }
    }
  }
}